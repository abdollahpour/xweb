package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.server.XWebUser;
import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;

public class ResourceModule extends Module {

    private final static Logger logger = LoggerFactory.getLogger("ResourceModule");

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz", Locale.ENGLISH);

    private final int BUFFER_SIZE = 256;

    private long startTempNumber = System.currentTimeMillis();

    private final static String PROPERTY_TEMP_DIR = "temp_dir";

    private final static String PROPERTY_DEFAULT_IDENTIFIER   = "default_id";

    private final static String PROPERTY_DATA_DIR   = "data_dir";

    private final static String PROPERTY_STORE_PATTERN   = "store_pattern";

    public final static String MODE_PUBLIC = "public";

    public final static String MODE_PROTECTED = "protected";

    public final static String MODE_PRIVATE= "private";

    private final File tempDir;

    private final File dataDir;

    private final String defaultIdentifier;

    private final String storePattern;

    public ResourceModule(final Manager manager, final ModuleInfo info, final ModuleParam properties) {
        super(manager, info, properties);

        tempDir = new File(properties.getString(PROPERTY_TEMP_DIR, null));
        storePattern = properties.getString(PROPERTY_STORE_PATTERN, null);

        // deprecated
        defaultIdentifier = properties.getString(
                PROPERTY_DEFAULT_IDENTIFIER,
                getManager().getProperty("default_identifier"));

        dataDir = new File(properties.getString(
                PROPERTY_DATA_DIR,
                manager.getContext().getInitParameter("data_store_path")));
    }

    @Override
    public void process(
            ServletContext context,
            HttpServletRequest request,
            HttpServletResponse response, ModuleParam params,
            HashMap<String, FileItem> files) throws IOException {

        final XWebUser user = (XWebUser)request.getSession().getAttribute(ir.xweb.server.Constants.SESSION_USER);

        if(params.containsKey("download")) {
            String path = params.getString("download", null);
            String id = params.getString("id", null);

            // Deprecated
            if(id == null) {
                id = params.get(id, null);
            }

            File file = null;
            if(id == null) {
                if(user == null) {
                    file = getFile(path);
                } else {
                    file = getFile(user, path);
                }
            } else {
                // TODO: Complete this part

                /*User u = findUser(emf, id);

                if(u != null) {
                    if(path.startsWith(MODE_PUBLIC + File.separator)) {
                        file = getFile(u, path);
                    } else if(path.startsWith(MODE_PROTECTED + File.separator)) {
                        if(user != null) {
                            file = getFile(u, path);
                        } else {
                            logger.info("Try to access resource that is protected by user session is not active: " + path);
                        }
                    } else if(Constants.ROLE_ADMIN.endsWith(u.role)) {
                        file = getFile(u, path);
                    } else {
                        logger.info("Try to access resource that is private by user session is not active: " + path);
                    }
                }*/
            }

            if(file == null || !file.exists()) {
                // we do not rise exception because it's not important to log this message
                throw new ModuleException(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
            } else {
                // check to store
                boolean store = false;
                DataSource dataSource = getManager().getDataSource("data-resource");

                if(dataSource != null) {
                    if(storePattern != null && path.matches(storePattern)) {
                        if (request.getHeader("Range") != null) {
                            // record it just for the first section of file
                            store = request.getHeader("Range").startsWith("bytes=0") || request.getHeader("Range").startsWith("bytes=-");
                        } else {
                            store = true;
                        }
                    }
                }

                try {
                    // store count
                    if(store) {
                        dataSource.setData(context, ResourceDataSource.DATA_SOURCE_STORE, path);
                    }

                    if (request.getHeader("Range") != null) {
                        rangeDownload(request, response, file);
                    } else {
                        fullDownload(request, response, file);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new ModuleException("", ex);
                }
            }
        }
    }

    private void rangeDownload(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final File file) throws ServletException, IOException {

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        final ArrayList<Long> rawRangesList = new ArrayList<Long>();
        final String range = request.getHeader("Range");
        if(!range.startsWith("bytes=")){
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }
        try {
            final StringTokenizer tokenizer = new StringTokenizer(range.substring("bytes=".length()), ",");
            String token;
            while(tokenizer.hasMoreTokens()){
                token = tokenizer.nextToken();
                rawRangesList.add(getRangeStart(token));
                rawRangesList.add(getRangeEnd(token));
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }
        if(rawRangesList.size()==0){
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }

        //calculate size and validate values
        final long fileSize = file.length();
        final long validRanges [] = new long[rawRangesList.size()];
        long totalSize = 0;
        for (int i = 0; i < rawRangesList.size()/2; i++) {
            final Long start = rawRangesList.get(i);
            final Long end = rawRangesList.get(i + 1);
            if(start==null&&end==null){
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            final long from;
            final long till;
            if (start == null) {
                from = fileSize - end.longValue();
                till = fileSize;
            } else if (end == null) {
                from = start.longValue();
                till = fileSize;
            } else {
                from = start.longValue();
                till = end.longValue() + 1;
            }
            if( from >= till || from < 0 || till < 0 ||till > fileSize){
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            validRanges[i] = from;
            validRanges[i+1] = till;
            totalSize += till - from;
        }

        //set headers
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", getETag(file));
        response.setHeader("Last-Modified", getLastModified(file));
        response.setHeader("Content-Length", String.valueOf(totalSize));
        response.setHeader("Content-Type", getContentType(file));
        response.setHeader("Content-Range", getContentRange(validRanges, fileSize));

        //send body
        response.setBufferSize(0);
        final OutputStream out = response.getOutputStream();

        if (!"head".equalsIgnoreCase(request.getMethod())) {
            for (int i = 0; i < validRanges.length / 2; i++) {
                write(file, out, validRanges[i], validRanges[i + 1]);
            }
        }
    }

    private String getContentRange(final long [] ranges, final long fileSize) {
        //TODO
        return "bytes " + ranges[0] + "-" + (ranges[1]-1) + "/" + fileSize;
    }

    public File getFile(String path) {
        if(this.defaultIdentifier == null) {
            throw new IllegalArgumentException("Default identifier not found");
        }

        return getFile(this.defaultIdentifier, path);
    }

    public File getFile(String identifier, String path) {
        if(identifier == null) {
            throw new IllegalArgumentException("null user");
        }
        if(path == null) {
            throw new IllegalArgumentException("null path");
        }
        if(path.startsWith(".")) {
            throw new IllegalArgumentException("Illegal path: " + path);
        }

        File f = new File(this.dataDir, identifier + File.separator + path);
        if(f.exists()) {
            return f;
        }

        return null;
    }

    public File getFile(final XWebUser user, final String path) {
        if(user == null) {
            throw new IllegalArgumentException("null user");
        }
        if(path == null) {
            throw new IllegalArgumentException("null path");
        }

        final File f = new File(this.dataDir, user.getIdentifier() + File.separator + path);
        if(f.exists()) {
            return f;
        }

        return null;
    }

    public File initTempFile() {
        startTempNumber++;
        final File file = new File(this.tempDir, Long.toString(startTempNumber));
        return file;
    }

    public File initTempDir() throws IOException {
        final File file = new File(this.tempDir, Long.toString(System.currentTimeMillis()));
        if(!file.exists() && !file.mkdirs()) {
            throw new IOException("Can not create temp dir. Please set '" + PROPERTY_TEMP_DIR + "' property. " + file);
        }
        return file;
    }

    public void writeFile(final HttpServletResponse response, final File file) throws IOException {
        if(file != null && file.exists()) {
            final long from = 0;
            final long till = file.length();

            // set headers
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("ETag", getETag(file));
            response.setHeader("Last-Modified", getLastModified(file));
            response.setHeader("Content-Length", String.valueOf(till - from));

            if(!response.containsHeader("Content-Type")) {
                response.setHeader("Content-Type", getContentType(file));
            }

            response.setBufferSize(0);
            final OutputStream out = response.getOutputStream();

            try {
                write(file, out, from, till);
            } catch (ServletException ex) {
                throw new IOException(ex);
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void write(final File file, final OutputStream out, final long from, final long till)
            throws ServletException, IOException {

        final RandomAccessFile in = new RandomAccessFile(file, "r");
        // TODO
        in.skipBytes((int) from);

        final byte[] buffer = new byte[BUFFER_SIZE];

        long contentLength = till - from;
        int bufferFullness;
        //final long timeStampNano = System.nanoTime();
        //final long timeDifferenceNano;

        while (contentLength > 0) {
            if (contentLength / buffer.length > 0) {
                bufferFullness = buffer.length;
            } else {
                bufferFullness = (int) contentLength % buffer.length;
            }
            in.read(buffer, 0, bufferFullness);
            out.write(buffer, 0, bufferFullness);
            contentLength = contentLength - bufferFullness;
        }
        out.flush();
    }

    private String getETag(final File file) {
        return "W/\"" + file.length() + "-" + file.lastModified() + "\"";
    }

    private String getLastModified(final File file) {
        return this.dateFormat.format(new Date(file.lastModified()));
    }

    private String getContentType(final File file) {
        String mime = getContext().getMimeType(file.getName());
        if (mime != null) {
            // add UTF-8 for text contents
            if(mime.startsWith("text/")) {
                // All the text contents should be with UTF-8 format
                mime += "; charset=utf-8";
            }

            return mime;
        } else {
            return "application/octetstream";
        }
    }

    private void fullDownload(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final File file) throws ServletException, IOException {

        final long from = 0;
        final long till = file.length();

        // set headers
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", getETag(file));
        response.setHeader("Last-Modified", getLastModified(file));
        response.setHeader("Content-Length", String.valueOf(till - from));
        response.setHeader("Content-Type", getContentType(file));

        response.setBufferSize(0);
        final OutputStream out = response.getOutputStream();

        if(!"head".equalsIgnoreCase(request.getMethod())){
            write(file, out, from, till);
        }
    }

    private Long getRangeStart(final String rangeInterval) throws NumberFormatException{
        final int index = rangeInterval.indexOf("-");
        if(index < 0){
            throw new NumberFormatException();
        }
        final String value = rangeInterval.substring(0, index);
        if(value.length() == 0){
            return null;
        }

        return new Long(value);
    }

    private Long getRangeEnd(final String rangeInterval)throws NumberFormatException{
        final int index = rangeInterval.indexOf("-");
        if(index < 0){
            throw new NumberFormatException();
        }
        final String value = rangeInterval.substring(index+1);
        if(value.length() == 0){
            return null;
        }

        return new Long(value);
    }

}
