/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class TestGzipModule extends TestModule {


    public TestGzipModule() throws IOException {
        super();
    }

    @Test
    public void testGzipContent() throws IOException, ServletException {
        assertEquals(testGzipContent(false), testGzipContent(true));
    }

    public String testGzipContent(boolean zip) throws IOException, ServletException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        params.put("key3", "value3");

        //final StringWriter writer = new StringWriter();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter printWriter = new PrintWriter(baos);
        final Writer writer = new OutputStreamWriter(baos);
        final ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                baos.write(b);
            }
        };

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        if(zip) {
            when(request.getHeader("Accept-Encoding")).thenReturn("gzip");
        }
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api");
        when(request.getParameter("api")).thenReturn("reply");
        when(response.getWriter()).thenReturn(printWriter);
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
        when(getServletContext().getRealPath(anyString())).thenReturn("");

        final ReplyModule module = getManager().getModuleOrThrow(ReplyModule.class);
        final GzipModule gzip = getManager().getModuleOrThrow(GzipModule.class);
        final TestChain chain = new TestChain();

        gzip.doFilter(getServletContext(), request, response, chain);

        module.process(
                getServletContext(),
                (HttpServletRequest) chain.request,
                (HttpServletResponse) chain.response,
                new ModuleParam(params), null);

        chain.response.flushBuffer();

        if(zip) {
            verify(response, times(1)).addHeader(eq("Content-Encoding"), eq("gzip"));
            GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(baos.toByteArray()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, "UTF-8"));

            return reader.readLine();
        } else {
            return new String(baos.toByteArray(), "UTF-8");
        }
    }

    private class TestChain implements FilterChain {

        ServletRequest request;

        ServletResponse response;

        @Override
        public void doFilter(
                final ServletRequest servletRequest,
                final ServletResponse servletResponse) throws IOException, ServletException {

            this.request = servletRequest;
            this.response = servletResponse;
        }

    }

}

