/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.util.Tools;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipModule extends Module {

    public final static String PARAM_MODULES = "modules";

    public final static String PARAM_EXTENSIONS = "extensions";

    public final static String PARAM_REQUESTS = "requests";

    public final static String PARAM_DIR_CACHE = "dir.cache";

    public final static String PARAM_SIZE_MAX = "size.max";

    private final int maxSize;

    private final List<String> modules;

    private final List<String> extensions;

    private final String requests;

    private File cacheDir;

    public GzipModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        this.modules = Arrays.asList(properties.getStrings(PARAM_MODULES, new String[0]));
        this.extensions = Arrays.asList(properties.getStrings(PARAM_EXTENSIONS, new String[0]));
        requests = properties.getString(PARAM_REQUESTS);
        cacheDir = properties.getFile(PARAM_DIR_CACHE);
        maxSize = properties.getInt(PARAM_SIZE_MAX, 2097152); // 2MB default
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest chainedRequest = null;
        HttpServletResponse chainedResponse = null;

        if (!response.isCommitted() && !response.containsHeader("Content-Encoding")) {

            // we will check for incoming request anyway
            final String contentEncoding = request.getHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.toLowerCase().indexOf("gzip") > -1) {
                chainedRequest = new ZipRequestWrapper(request);
            }

            final String acceptEncoding = request.getHeader("Accept-Encoding");
            if (acceptEncoding != null && acceptEncoding.toLowerCase().indexOf("gzip") > -1) {
                URI uri;
                try {
                    uri = new URI(request.getRequestURI());
                } catch (Exception ex) {
                    // It will never happen because it passed by http request
                    throw new IOException(ex);
                }

                // we don't care about context path, so we trunk it
                final String path = uri.getPath().substring(request.getContextPath().length());

                // By regex
                if(requests != null) {
                    if(path.matches(requests)) {
                        chainedResponse = new ZipResponseWrapper(response);
                    }
                }

                // By API call
                if(chainedResponse == null) {
                    if(modules.size() > 0) {
                        // We handle API requests with different authentication method (Role base)
                        boolean isApiCall = path.equals(Constants.MODULE_URI_PERFIX);

                        if(isApiCall) {
                            final String moduleName = request.getParameter(Constants.MODULE_NAME_PARAMETER);
                            if(modules.contains(moduleName)) {
                                chainedResponse = new ZipResponseWrapper(response);
                            }
                        }
                    }
                }

                // By extensions
                if (chainedResponse == null) {
                    if (extensions.size() > 0) {
                        File dir = new File(context.getRealPath(File.separator));
                        File file = new File(dir, path);

                        if(file.exists() && file.length() <= maxSize) {
                            String extension = Tools.getFileExtension(file.getName());

                            // check extension
                            if(extensions.contains(extension.toLowerCase())) {
                                // check for cache dir
                                if(cacheDir == null || !cacheDir.exists()) {
                                    cacheDir = getManager().getModule(ResourceModule.class).initTempDir();
                                }

                                File zipFile = new File(cacheDir.getPath() + path + ".gz");
                                File zipDir = zipFile.getParentFile();

                                // Because of some weird problem, this solution does not work in Jetty, but Java7
                                // API is fine anywhere
                                //if(zipDir == null || (!zipDir.exists() && !zipDir.mkdirs())) {
                                //    throw new IOException("Can not create zip dir: " + zipDir);
                                //}
                                Files.createDirectories(zipDir.toPath());

                                if(!zipFile.exists() || zipFile.lastModified() < file.lastModified()) {
                                    Tools.zipFile(file, zipFile);
                                }

                                // TODO: It's not the right way to redirect and cut the filter!
                                // It will cut the rest of filters!
                                // we redirect it to resource module
                                final String filePath = Constants.MODULE_URI_PERFIX + "?" +
                                        Constants.MODULE_NAME_PARAMETER  + "=" +
                                        getInfo().getName() + "&file=" + path;
                                RequestDispatcher dispatcher = request.getRequestDispatcher(filePath);
                                dispatcher.forward(request, response);
                                return;
                            }
                        }
                    }
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

    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam param,
            final HashMap<String, FileItem> files) throws IOException {

        if(param.containsKey("file")) {
            final ResourceModule module = getManager().getModuleOrThrow(ResourceModule.class);

            final String path = param.getString("file", null);
            final File file = new File(cacheDir, path);

            module.writeFile(request, response, file);
        }
    }

    /*private class ZipFileRequestWrapper extends HttpServletRequestWrapper {

        FileServletInputStream inputStream;

        final File zipFile;

        BufferedReader reader;

        public ZipFileRequestWrapper(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final File zipFile) {

            super(request);
            this.zipFile = zipFile;

            response.addHeader("Content-Encoding", "gzip");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if(inputStream == null) {
                inputStream = new FileServletInputStream(zipFile);
            }
            return inputStream;
        }

        @Override
        public int getContentLength() {
            return (int) zipFile.length();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if(this.reader == null) {
                this.reader = new BufferedReader(new InputStreamReader(getInputStream(), getRequest().getCharacterEncoding()));
            }
            return this.reader;
        }
    }

    private class FileServletInputStream extends ServletInputStream {

        final FileInputStream inputStream;

        FileServletInputStream(final File file) throws IOException {
            inputStream = new FileInputStream(file);
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return inputStream.read(b);
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }*/

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

        BufferedReader reader;

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
