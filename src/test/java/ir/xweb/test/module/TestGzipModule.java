
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;

import ir.xweb.module.GzipModule;
import ir.xweb.module.ModuleParam;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestGzipModule {

    private final TestModules modules;

    public TestGzipModule(final TestModules modules) {
        this.modules = modules;
    }

    public void test(final boolean zip) throws IOException, ServletException {

        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("api", "empty2");
        params.put("key1", "value1");

        final String data = "this is test";
        final byte[] bytes = data.getBytes();
        final byte[] zipped = zip(bytes);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api");
        when(request.getParameter(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final String arg = (String) args[0];
                return (String) params.get(arg);
            }
        });
        when(request.getParameterNames()).thenReturn(
            new IteratorEnumeration(params.keySet().iterator()));
        if(zip) {
            when(request.getInputStream()).thenReturn(new RequestInput(zipped));
            when(request.getHeader("Content-Encoding")).thenReturn("gzip");
            when(request.getHeader("Accept-Encoding")).thenReturn("gzip");
        } else {
            when(request.getInputStream()).thenReturn(new RequestInput(bytes));
        }

        final ResponseOut out = new ResponseOut();

        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(out);
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
        when(modules.getServletContext().getRealPath(anyString())).thenReturn("");

        final EmptyModule2 module = modules.getManager().getModuleOrThrow(EmptyModule2.class);
        final GzipModule gzip = modules.getManager().getModuleOrThrow(GzipModule.class);
        final TestChain chain = new TestChain();

        gzip.doFilter(modules.getServletContext(), request, response, chain);

        module.process(
                modules.getServletContext(),
                (HttpServletRequest) chain.request,
                (HttpServletResponse) chain.response,
                new ModuleParam(params), null);

        chain.response.flushBuffer();

        final byte[] outData = out.baos.toByteArray();

        if(zip) {
            verify(response, times(1)).addHeader(eq("Content-Encoding"), eq("gzip"));
            assertEquals(data, new String(unzip(outData)));
        } else {
            verify(response, times(0)).addHeader(eq("Content-Encoding"), eq("gzip"));
            assertEquals(data, new String(outData));
        }
    }

    private class TestChain implements FilterChain {

        ServletRequest request;

        ServletResponse response;

        @Override
        public void doFilter(
                final ServletRequest servletRequest,
                final ServletResponse servletResponse) throws IOException, ServletException
        {
            this.request = servletRequest;
            this.response = servletResponse;
        }

        boolean done() {
            return this.request != null || this.response != null;
        }

    }

    private class IteratorEnumeration<E> implements Enumeration<E> {

        private final Iterator<E> iterator;

        public IteratorEnumeration(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        public E nextElement() {
            return iterator.next();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

    }

    private byte[] zip(final byte[] data) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPOutputStream gos = new GZIPOutputStream(baos);
        gos.write(data);
        gos.finish();
        gos.flush();
        gos.close();

        return baos.toByteArray();
    }

    private byte[] unzip(final byte[] data) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final GZIPInputStream gis = new GZIPInputStream(bais);

        byte[] buffer = new byte[1024];
        int size;

        while((size = gis.read(buffer)) > 0) {
            baos.write(buffer, 0, size);
        }

        gis.close();

        return baos.toByteArray();
    }

    private class RequestInput extends ServletInputStream {

        ByteArrayInputStream bais;

        RequestInput(final byte[] data) {
            bais = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            return bais.read();
        }

        /*
        @Override
        public boolean isFinished() {
            return false;
        }
        */

        /*
        @Override
        public boolean isReady() {
            return false;
        }
        */

        /*
        @Override
        public void setReadListener(ReadListener readListener) {

        }
        */
    }

    private class ResponseOut extends ServletOutputStream {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        /*
        @Override
        public boolean isReady() {
            return false;
        }
        */

        /*
        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
        */
    }

}

