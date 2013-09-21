package ir.xweb.module;

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
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
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

    public final static String PROPERTY_TEMP_DIR = "dir.temp";

    public final static String PROPERTY_DEFAULT_ID = "default.id";

    public final static String PROPERTY_DATA_DIR   = "dir.data";

    public final static String PROPERTY_STORE_PATTERN   = "store.pattern";

    public final static String MODE_PUBLIC = "public";

    public final static String MODE_PROTECTED = "protected";

    public final static String MODE_PRIVATE= "private";

    private final File tempDir;

    private final File dataDir;

    private final String defaultId;

    private final String storePattern;

    private ServletContext context;

    public ResourceModule(final Manager manager, final ModuleInfo info, final ModuleParam properties) {
        super(manager, info, properties);

        String dataPath = properties.getString(PROPERTY_DATA_DIR, null);
        if(dataPath == null) {
            throw new IllegalArgumentException("Data path not found please set: " + PROPERTY_DATA_DIR);
        }
        dataDir = new File(dataPath);

        String tempPath = properties.getString(PROPERTY_TEMP_DIR, System.getProperty("java.io.tmpdir"));
        if(tempPath == null) {
            tempPath = dataPath + File.separator + "temp";
        }
        tempDir = new File(tempPath);

        storePattern = properties.getString(PROPERTY_STORE_PATTERN, null);

        // deprecated
        defaultId = properties.getString(PROPERTY_DEFAULT_ID, null);
    }

    @Override
    public void init(final ServletContext context) {
        this.context = context;
    }

    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam params,
            final HashMap<String, FileItem> files) throws IOException {

        final XWebUser user = (XWebUser)request.getSession().getAttribute(ir.xweb.server.Constants.SESSION_USER);

        if(params.containsKey("download")) {
            final String path = params.getString("download", null);
            final String id = params.getString("id", null);

            File file = null;
            if(id == null) {
                if(user == null) {
                    file = getFile(path);
                } else {
                    file = getFile(user, path);

                    // if file not found for current user, try to get it from default user
                    if(file == null) {
                        file = getFile(path);
                    }
                }
            } else {
                if(path.startsWith(MODE_PUBLIC + File.separator)) {
                    file = getFile(id, path);
                } else if(path.startsWith(MODE_PROTECTED + File.separator)) {
                    if(user != null) {
                        file = getFile(id, path);
                    } else {
                        logger.info("Try to access resource that is protected by user session is not active: " + path);
                    }
                } else if(isAdmin(context, user)) {
                    file = getFile(id, path);
                } else {
                    logger.info("Try to access resource that is private by user session is not active: " + path);
                }
            }

            if(file == null || !file.exists()) {
                // we do not rise exception because it's not important to log this message
                throw new ModuleException(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
            } else {
                // check to store
                boolean store = false;
                //DataSource dataSource = getManager().getDataSource("data-resource");

                //if(dataSource != null) {
                    if(storePattern != null && path.matches(storePattern)) {
                        if (request.getHeader("Range") != null) {
                            // record it just for the first section of file
                            store = request.getHeader("Range").startsWith("bytes=0") || request.getHeader("Range").startsWith("bytes=-");
                        } else {
                            store = true;
                        }
                    }
                //}

                try {
                    // store count
                    if(store) {
                        storeResourceUsage(context, path);
                        //dataSource.setData(context, ResourceDataSource.DATA_SOURCE_STORE, path);
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
        response.setHeader("Content-Disposition",
                "inline; " +
                        "filename=" + file.getName() + "; " +
                        "modification-date=\"" + dateFormat.format(file.lastModified()) + "\"");

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

    public File getFile(final String path) {
        if(this.defaultId == null) {
            throw new IllegalArgumentException("Default identifier not found. Please set: " + PROPERTY_DEFAULT_ID);
        }

        return getFile(this.defaultId, path);
    }

    public File getFile(final String id, final String path) {
        if(id == null) {
            throw new IllegalArgumentException("null id");
        }
        if(path == null) {
            throw new IllegalArgumentException("null path");
        }
        if(path.matches("^(\\./|\\.\\./|~/|\\\\)")) {   // try to hack
            throw new IllegalArgumentException("Illegal path: " + path);
        }

        File f = new File(this.dataDir, getUserDirectory(context, id) + File.separator + path);
        if(f.exists()) {
            return f;
        }

        return null;
    }

    public File getFile(final XWebUser user, final String path) {
        return getFile(user.getId(), path);
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
            response.setHeader("Content-Disposition",
                    "inline; " +
                    "filename=" + file.getName() + "; " +
                    "modification-date=\"" + dateFormat.format(file.lastModified()) + "\"");

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

    public File initResourceDir(final String path) {
        if(this.defaultId == null) {
            throw new IllegalArgumentException("Default identifier not found");
        }

        return initResourceDir(this.defaultId, path);
    }

    public File getDataDir() {
        return dataDir;
    }

    public File getTempDir() {
        return tempDir;
    }

    public File initResourceDir(String id, String path) {
        File dir = new File(dataDir, id + File.separator + path);
        if(dir.exists() || dir.mkdirs()) {
            return dir;
        }
        return null;
    }

    public File initResourceFile(String userId, String path) {
        File dir = new File(dataDir, userId);
        File file = new File(dir, path);

        File p = file.getParentFile();
        if(!p.exists() && !p.mkdirs()) {
            return null;
        }

        return file;
    }

    /*public File initResourceDir(XWebUser user, String path) {
        File dir = new File(dataDir, user.getId() + File.separator + path);
        if(dir.exists() || dir.mkdirs()) {
            return dir;
        }
        return null;
    }*/

    /**
     * Write partial file data
     * @param file
     * @param out
     * @param from
     * @param till
     * @throws ServletException
     * @throws IOException
     */
    private void write(final File file, final OutputStream out, final long from, final long till)
            throws ServletException, IOException {

        final RandomAccessFile f = new RandomAccessFile(file, "r");
        final WritableByteChannel outChannel = Channels.newChannel(out);
        try {
            final MappedByteBuffer map = f.getChannel().map(FileChannel.MapMode.READ_ONLY, from, till - from);
            outChannel.write(map);
        } finally {
            if(f != null) {
                try {
                    f.close();
                } catch (Exception ex) {}
            }
        }
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
        response.setHeader("Content-Disposition",
                "inline; " +
                "filename=" + file.getName() + "; " +
                "modification-date=\"" + dateFormat.format(file.lastModified()) + "\"");

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

    protected boolean isAdmin(ServletContext context, XWebUser user) {
        return false;
    }

    protected void storeResourceUsage(ServletContext context, String path) {

    }

    protected String getUserDirectory(final ServletContext context, final String id) {
        return id;
    }

}

