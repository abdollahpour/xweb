/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.util.Tools;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipModule extends Module {

    public final static String PARAM_MODULES = "modules";

    public final static String PARAM_FILES = "files";

    public final static String PARAM_REQUESTS = "requests";

    private final List<String> modules;

    private final String files;

    private final String requests;

    public GzipModule(final Manager manager, final ModuleInfo info, final ModuleParam properties) {
        super(manager, info, properties);

        String moduleNames = properties.getString(PARAM_MODULES, null);
        modules = (moduleNames == null ? null : Arrays.asList(moduleNames.split("[,;]")));
        files = properties.getString(PARAM_FILES, null);
        requests = properties.getString(PARAM_REQUESTS, null);
    }

    @Override
    public void doFilter(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest chainedRequest = null;
        HttpServletResponse chainedResponse = null;

        // we will check for incoming request anyway
        String contentEncoding = request.getHeader("Content-Encoding");
        if(contentEncoding != null && contentEncoding.toLowerCase().indexOf("gzip") > -1) {
            chainedRequest = new ZipRequestWrapper(request);
        }

        /*URI uri = null;
        try {
            uri = new URI(request.getRequestURI());
        } catch (Exception ex) {
            // never happens
        }*/


        String acceptEncoding = request.getHeader("Accept-Encoding");
        if(acceptEncoding != null && acceptEncoding.toLowerCase().indexOf("gzip") > -1) {
            // we don't care about context path, so we trunk it
            String path = request.getRequestURI();

            if(requests != null) {
                if(path.matches(requests)) {
                    chainedResponse = new ZipResponseWrapper(response);
                }
            }

            if(chainedResponse == null && modules != null) {
                // get module names
                boolean isApiCall = path.equals(Constants.MODULE_URI_PERFIX);
                if(isApiCall) {
                    String moduleName = request.getParameter(Constants.MODULE_NAME_PARAMETER);
                    if(modules.contains(moduleName)) {
                        chainedResponse = new ZipResponseWrapper(response);
                    }
                }
            }

            if(chainedResponse == null) {
                File dir = new File(context.getRealPath(File.separator));
                File file = new File(dir, path);



                if(file.exists()) {
                    File zipFile = new File(file.getPath() + ".gz");

                    if(!zipFile.exists() || zipFile.lastModified() < zipFile.lastModified()) {
                        if(zipFile.canWrite()) {
                            Tools.zipFile(file, zipFile);
                        }
                    }

                    chainedRequest = new ZipFileRequestWrapper(request, response);
                }
            }
        }

        //if(chainedRequest != null || chainedResponse != null) {
            filterChain.doFilter(
                    chainedRequest == null ? request : chainedRequest,
                    chainedResponse == null ? response : chainedResponse
            );
        //}
    }

    private class ZipFileRequestWrapper extends HttpServletRequestWrapper {

        final HttpServletRequest request;

        public ZipFileRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.request = request;

            response.addHeader("Content-Encoding", "gzip");
        }

        @Override
        public String getRequestURI() {
            return this.request.getRequestURI() + ".gz";
        }
    }

    private class ZipRequestStream extends ServletInputStream {

        final HttpServletRequest request;
        final ServletInputStream inStream;
        final GZIPInputStream in;

        public ZipRequestStream(final HttpServletRequest request) throws IOException {
            this.request = request;
            inStream = request.getInputStream();
            in = new GZIPInputStream(inStream);
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }

        @Override
        public int read(byte b[]) throws IOException {
            return in.read(b);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            return in.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    private class ZipRequestWrapper extends HttpServletRequestWrapper {

        final HttpServletRequest origRequest;

        final ServletInputStream inStream;

        final BufferedReader reader;

        public ZipRequestWrapper(final HttpServletRequest req) throws IOException {
            super(req);

            origRequest = null;
            inStream = new ZipRequestStream(req);
            reader = new BufferedReader(new InputStreamReader(inStream));
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return inStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return reader;
        }
    }

    private class ZipResponseStream extends ServletOutputStream {

        final ServletOutputStream outStream;

        final GZIPOutputStream out;

        public ZipResponseStream(final HttpServletResponse response) throws IOException {
            outStream = response.getOutputStream();
            out = new GZIPOutputStream(outStream);
            response.addHeader("Content-Encoding", "gzip");
        }

        @Override
        public void write(final int b) throws IOException {
            out.write(b);
        }

        @Override
        public void write(final byte b[]) throws IOException {
            out.write(b);
        }

        @Override
        public void write(final byte b[], final int off, final int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public void flush()  throws IOException {
            out.flush();
        }

        public void finish() throws IOException {
            out.finish();
        }
    }

    private class ZipResponseWrapper extends HttpServletResponseWrapper {

        final HttpServletResponse response;

        ZipResponseStream outStream = null;

        PrintWriter writer;

        public ZipResponseWrapper(final HttpServletResponse response) {
            super(response);
            this.response = response;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if(outStream == null) {
                outStream = new ZipResponseStream(response);
            }
            return outStream;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
                outStream.finish();
                outStream.flush();
            } else if (outStream != null) {
                outStream.finish();
                outStream.flush();
            }

            super.flushBuffer();
        }

        @Override
        public void setContentLength(int len) {}

        @Override
        public PrintWriter getWriter() throws IOException {
            if(writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getResponse().getCharacterEncoding()), true);
            }
            return writer;
        }
    }

}
