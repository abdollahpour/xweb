package ir.xweb.module;

import java.io.*;
import java.lang.reflect.Array;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 * Mock implementation of the HttpServletRequest interface.
 *
 * <p>Used for testing the web framework; also useful
 * for testing application controllers.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class ScheduleRequest implements HttpServletRequest, Serializable {

    public static final String DEFAULT_PROTOCOL = "http";

    public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

    public static final String DEFAULT_SERVER_NAME = "localhost";

    public static final int DEFAULT_SERVER_PORT = 80;

    public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";

    public static final String DEFAULT_REMOTE_HOST = "localhost";


    //---------------------------------------------------------------------
    // ServletRequest properties
    //---------------------------------------------------------------------

    private final Hashtable attributes = new Hashtable();

    private String characterEncoding;

    private byte[] content;

    private String contentType;

    private final Map parameters;// = CollectionFactory.createLinkedMapIfPossible(16);

    private String protocol = DEFAULT_PROTOCOL;

    private String scheme = DEFAULT_PROTOCOL;

    private String serverName = DEFAULT_SERVER_NAME;

    private int serverPort = DEFAULT_SERVER_PORT;

    private String remoteAddr = DEFAULT_REMOTE_ADDR;

    private String remoteHost = DEFAULT_REMOTE_HOST;

    /** List of locales in descending order */
    private final Vector locales = new Vector();

    private boolean secure = false;

    private final ServletContext servletContext;

    private int remotePort = DEFAULT_SERVER_PORT;

    private String localName = DEFAULT_SERVER_NAME;

    private String localAddr = DEFAULT_SERVER_ADDR;

    private int localPort = DEFAULT_SERVER_PORT;


    //---------------------------------------------------------------------
    // HttpServletRequest properties
    //---------------------------------------------------------------------

    private String authType;

    private Cookie[] cookies;

    private final Hashtable headers = new Hashtable();

    private String method;

    private String pathInfo;

    private String contextPath = "";

    private String queryString;

    private String remoteUser;

    private final Set    userRoles = new HashSet();

    private Principal userPrincipal;

    private String requestURI = "";

    private String servletPath = "";

    private HttpSession session;

    private boolean requestedSessionIdValid = true;

    private boolean requestedSessionIdFromCookie = true;

    private boolean requestedSessionIdFromURL = false;


    //---------------------------------------------------------------------
    // Constructors
    //---------------------------------------------------------------------

    /**
     * Create a new MockHttpServletRequest.
     * @param servletContext the ServletContext that the request runs in
     * @param method the request method
     * @param requestURI the request URI
     * @see #setMethod
     * @see #setRequestURI
     */
    public ScheduleRequest(
            final ServletContext servletContext,
            final ModuleParam param,
            final String method,
            final String requestURI)
    {
        this.locales.add(Locale.ENGLISH);
        this.servletContext = servletContext;
        this.parameters = param;
        this.method = method;
        this.requestURI = requestURI;
    }


    //---------------------------------------------------------------------
    // ServletRequest interface
    //---------------------------------------------------------------------

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        return this.attributes.keys();
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getContentLength() {
        return (this.content != null ? content.length : -1);
    }

    /*
    @Override
    public long getContentLengthLong() {
        return 0;
    }
    */

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public ServletInputStream getInputStream() {
        return null;
    }

    /**
     * Add a single value for an HTTP parameter.
     * <p>If there are already one or more values registered for the given
     * parameter name, the given value will be added to the end of the list.
     */
    public void addParameter(String name, String value) {
        addParameter(name, new String[] {value});
    }

    /**
     * Add an array of values for an HTTP parameter.
     * <p>If there are already one or more values registered for the given
     * parameter name, the given values will be added to the end of the list.
     */
    public void addParameter(String name, String[] values) {
        String[] oldArr = (String[]) this.parameters.get(name);
        if (oldArr != null) {
            String[] newArr = new String[oldArr.length + values.length];
            System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
            System.arraycopy(values, 0, newArr, oldArr.length, values.length);
            this.parameters.put(name, newArr);
        }
        else {
            this.parameters.put(name, values);
        }
    }

    public String getParameter(String name) {
        String[] arr = (String[]) this.parameters.get(name);
        return (arr != null && arr.length > 0 ? arr[0] : null);
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(this.parameters.keySet());
    }

    public String[] getParameterValues(String name) {
        return (String[]) this.parameters.get(name);
    }

    public Map getParameterMap() {
        return Collections.unmodifiableMap(this.parameters);
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public BufferedReader getReader() throws UnsupportedEncodingException {
        if (this.content != null) {
            InputStream sourceStream = new ByteArrayInputStream(this.content);
            Reader sourceReader = (this.characterEncoding != null) ?
                    new InputStreamReader(sourceStream, this.characterEncoding) : new InputStreamReader(sourceStream);
            return new BufferedReader(sourceReader);
        }
        else {
            return null;
        }
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setAttribute(String name, Object value) {
        if (value != null) {
            this.attributes.put(name, value);
        }
        else {
            this.attributes.remove(name);
        }
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    /**
     * Add a new preferred locale, before any existing locales.
     */
    public void addPreferredLocale(Locale locale) {
        this.locales.add(0, locale);
    }

    public Locale getLocale() {
        return (Locale) this.locales.get(0);
    }

    public Enumeration getLocales() {
        return this.locales.elements();
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isSecure() {
        return secure;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return new ScheduleRequestDispatcher(path);
    }

    public String getRealPath(String path) {
        return this.servletContext.getRealPath(path);
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalAddr(String localAddr) {
        this.localAddr = localAddr;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getLocalPort() {
        return localPort;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }


    //---------------------------------------------------------------------
    // HttpServletRequest interface
    //---------------------------------------------------------------------

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthType() {
        return authType;
    }

    public void setCookies(Cookie[] cookies) {
        this.cookies = cookies;
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    /**
     * Add a header entry for the given name.
     * <p>If there was no entry for that header name before,
     * the value will be used as-is. In case of an existing entry,
     * a String array will be created, adding the given value (more
     * specifically, its toString representation) as further element.
     * <p>Multiple values can only be stored as list of Strings,
     * following the Servlet spec (see <code>getHeaders</code> accessor).
     * As alternative to repeated <code>addHeader</code> calls for
     * individual elements, you can use a single call with an entire
     * array or Collection of values as parameter.
     * @see #getHeaderNames
     * @see #getHeader
     * @see #getHeaders
     * @see #getDateHeader
     * @see #getIntHeader
     */
    public void addHeader(String name, Object value) {
        Object oldValue = this.headers.get(name);
        if (oldValue instanceof List) {
            List list = (List) oldValue;
            addHeaderValue(list, value);
        }
        else if (oldValue != null) {
            List list = new LinkedList();
            list.add(oldValue);
            addHeaderValue(list, value);
            this.headers.put(name, list);
        }
        else if (value instanceof Collection || value.getClass().isArray()) {
            List list = new LinkedList();
            addHeaderValue(list, value);
            this.headers.put(name, list);
        }
        else {
            this.headers.put(name, value);
        }
    }

    private void addHeaderValue(List list, Object value) {
        if (value instanceof Collection) {
            Collection valueColl = (Collection) value;
            for (Iterator it = valueColl.iterator(); it.hasNext();) {
                Object element = it.next();
                list.add(element.toString());
            }
        }
        else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                list.add(element.toString());
            }
        }
        else {
            list.add(value);
        }
    }

    public long getDateHeader(String name) {
        Object value = this.headers.get(name);
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        else if (value != null) {
            throw new IllegalArgumentException(
                    "Value for header '" + name + "' is neither a Date nor a Number: " + value);
        }
        else {
            return -1L;
        }
    }

    public String getHeader(String name) {
        Object value = this.headers.get(name);
        if (value instanceof List) {
            return collectionToDelimitedString((List) value, ",", "", "");
        }
        else if (value != null) {
            return value.toString();
        }
        else {
            return null;
        }
    }

    /**
     * Convenience method to return a Collection as a delimited (e.g. CSV)
     * String. E.g. useful for <code>toString()</code> implementations.
     * @param coll the Collection to display
     * @param delim the delimiter to use (probably a ",")
     * @param prefix the String to start each element with
     * @param suffix the String to end each element with
     * @return the delimited String
     */
    private String collectionToDelimitedString(Collection coll, String delim, String prefix, String suffix) {
        if (coll == null || coll.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public Enumeration getHeaders(String name) {
        Object value = this.headers.get(name);
        if (value instanceof List) {
            return Collections.enumeration((List) value);
        }
        else if (value != null) {
            Vector vector = new Vector(1);
            vector.add(value.toString());
            return vector.elements();
        }
        else {
            return Collections.enumeration(Collections.EMPTY_SET);
        }
    }

    public Enumeration getHeaderNames() {
        return this.headers.keys();
    }

    public int getIntHeader(String name) {
        Object value = this.headers.get(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        else if (value != null) {
            throw new NumberFormatException("Value for header '" + name + "' is not a Number: " + value);
        }
        else {
            return -1;
        }
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getPathTranslated() {
        return (this.pathInfo != null ? getRealPath(this.pathInfo) : null);
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * @deprecated in favor of addUserRole
     * @see #addUserRole
     */
    public void addRole(String role) {
        addUserRole(role);
    }

    public void addUserRole(String role) {
        this.userRoles.add(role);
    }

    public boolean isUserInRole(String role) {
        return this.userRoles.contains(role);
    }

    public void setUserPrincipal(Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public String getRequestedSessionId() {
        HttpSession session = this.getSession();
        return (session != null ? session.getId() : null);
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer(this.scheme);
        url.append("://").append(this.serverName).append(':').append(this.serverPort);
        url.append(getRequestURI());
        return url;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setSession(HttpSession session) {
        this.session = session;
        if (session instanceof ScheduleHttpSession) {
            ScheduleHttpSession mockSession = ((ScheduleHttpSession) session);
            mockSession.access();
        }
    }

    public HttpSession getSession(boolean create) {
        // reset session if invalidated
        if (this.session instanceof ScheduleHttpSession && ((ScheduleHttpSession) this.session).isInvalid()) {
            this.session = null;
        }
        // create new session if necessary
        if (this.session == null && create) {
            this.session = new ScheduleHttpSession(this.servletContext);
        }
        return this.session;
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    /*
    @Override
    public String changeSessionId() {
        return this.session == null ? null : this.session.getId();
    }
    */

    public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
        this.requestedSessionIdValid = requestedSessionIdValid;
    }

    public boolean isRequestedSessionIdValid() {
        return this.requestedSessionIdValid;
    }

    public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
        this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return this.requestedSessionIdFromCookie;
    }

    public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
        this.requestedSessionIdFromURL = requestedSessionIdFromURL;
    }

    public boolean isRequestedSessionIdFromURL() {
        return this.requestedSessionIdFromURL;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s2) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    /*
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> tClass) throws IOException, ServletException {
        return null;
    }
    */

}