package ir.xweb.module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Mock implementation of the HttpServletResponse interface.
 *
 * <p>Used for testing the web framework; also useful
 * for testing application controllers.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class ScheduleResponse implements HttpServletResponse {

    public static final int DEFAULT_SERVER_PORT = 80;


    //---------------------------------------------------------------------
    // ServletResponse properties
    //---------------------------------------------------------------------

    private String characterEncoding = "utf-8";//WebUtils.DEFAULT_CHARACTER_ENCODING;

    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    //private final DelegatingServletOutputStream outputStream = new DelegatingServletOutputStream(this.content);

    private PrintWriter writer;

    private long contentLength = 0;

    private String contentType;

    private int bufferSize = 4096;

    private boolean committed;

    private Locale locale = Locale.getDefault();


    //---------------------------------------------------------------------
    // HttpServletResponse properties
    //---------------------------------------------------------------------

    private final List cookies = new ArrayList();

    private final Map headers = new HashMap();

    private int status = HttpServletResponse.SC_OK;

    private String errorMessage;

    private String redirectedUrl;

    private String forwardedUrl;

    private String includedUrl;


    //---------------------------------------------------------------------
    // ServletResponse interface
    //---------------------------------------------------------------------

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public ServletOutputStream getOutputStream() {
        return null;
    }

    public PrintWriter getWriter() throws UnsupportedEncodingException {
        if (this.writer == null) {
            Writer targetWriter = (this.characterEncoding != null ?
                    new OutputStreamWriter(this.content, this.characterEncoding) : new OutputStreamWriter(this.content));
            this.writer = new PrintWriter(targetWriter);
        }
        return writer;
    }

    public byte[] getContentAsByteArray() {
        flushBuffer();
        return this.content.toByteArray();
    }

    public String getContentAsString() throws UnsupportedEncodingException {
        flushBuffer();
        return (this.characterEncoding != null) ?
                this.content.toString(this.characterEncoding) : this.content.toString();
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /*
    @Override
    public void setContentLengthLong(long l) {
        this.contentLength = l;
    }
    */

    public int getContentLength() {
        return (int)contentLength;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void flushBuffer() {
        /*if (this.writer != null) {
            this.writer.flush();
        }
        if (this.outputStream != null) {
            try {
                this.outputStream.flush();
            }
            catch (IOException ex) {
                throw new IllegalStateException("Could not flush OutputStream: " + ex.getMessage());
            }
        }
        this.committed = true;*/
    }

    public void resetBuffer() {
        if (this.committed) {
            throw new IllegalStateException("Cannot reset buffer - response is already committed");
        }
        this.content.reset();
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void reset() {
        resetBuffer();
        this.characterEncoding = null;
        this.contentLength = 0;
        this.contentType = null;
        this.locale = null;
        this.cookies.clear();
        this.headers.clear();
        this.status = HttpServletResponse.SC_OK;
        this.errorMessage = null;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }


    //---------------------------------------------------------------------
    // HttpServletResponse interface
    //---------------------------------------------------------------------

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public Cookie[] getCookies() {
        return (Cookie[]) this.cookies.toArray(new Cookie[this.cookies.size()]);
    }

    public Cookie getCookie(String name) {
        for (Iterator it = this.cookies.iterator(); it.hasNext();) {
            Cookie cookie = (Cookie) it.next();
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    public boolean containsHeader(String name) {
        return this.headers.containsKey(name);
    }

    @Override
    public String getHeader(String name) {
        final Object header = this.headers.get(name);
        if(header != null) {
            if(!(header instanceof Collection)) { // array headers
                return header.toString();
            }
        }
        return null;
    }

    public List getHeaders(String name) {
        Object value = this.headers.get(name);
        if (value instanceof List) {
            return (List) value;
        }
        else if (value != null) {
            return Collections.singletonList(value);
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }

    public Set getHeaderNames() {
        return this.headers.keySet();
    }

    public String encodeURL(String url) {
        return url;
    }

    public String encodeRedirectURL(String url) {
        return url;
    }

    public String encodeUrl(String url) {
        return url;
    }

    public String encodeRedirectUrl(String url) {
        return url;
    }

    public void sendError(int status, String errorMessage) throws IOException {
        if (this.committed) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        this.status = status;
        this.errorMessage = errorMessage;
        this.committed = true;
    }

    public void sendError(int status) throws IOException {
        if (this.committed) {
            throw new IllegalStateException("Cannot set error status - response is already committed");
        }
        this.status = status;
        this.committed = true;
    }

    public void sendRedirect(String url) throws IOException {
        if (this.committed) {
            throw new IllegalStateException("Cannot send redirect - response is already committed");
        }
        this.redirectedUrl = url;
        this.committed = true;
    }

    public String getRedirectedUrl() {
        return redirectedUrl;
    }

    public void setDateHeader(String name, long value) {
        this.headers.put(name, new Long(value));
    }

    public void addDateHeader(String name, long value) {
        doAddHeader(name, new Long(value));
    }

    public void setHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public void addHeader(String name, String value) {
        doAddHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.headers.put(name, new Integer(value));
    }

    public void addIntHeader(String name, int value) {
        doAddHeader(name, new Integer(value));
    }

    private void doAddHeader(String name, Object value) {
        Object oldValue = this.headers.get(name);
        if (oldValue instanceof List) {
            List list = (List) oldValue;
            list.add(value);
        }
        else if (oldValue != null) {
            List list = new LinkedList();
            list.add(oldValue);
            list.add(value);
            this.headers.put(name, list);
        }
        else {
            this.headers.put(name, value);
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setStatus(int status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


    //---------------------------------------------------------------------
    // Methods for MockRequestDispatcher
    //---------------------------------------------------------------------

    public void setForwardedUrl(String forwardedUrl) {
        this.forwardedUrl = forwardedUrl;
    }

    public String getForwardedUrl() {
        return forwardedUrl;
    }

    public void setIncludedUrl(String includedUrl) {
        this.includedUrl = includedUrl;
    }

    public String getIncludedUrl() {
        return includedUrl;
    }

}