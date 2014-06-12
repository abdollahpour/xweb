package ir.xweb.module;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * Mock implementation of the HttpSession interface.
 *
 * <p>Used for testing the web framework; also useful
 * for testing application controllers.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class ScheduleHttpSession implements HttpSession {

    public static final String SESSION_COOKIE_NAME = "JSESSION";

    private static int nextId = 1;


    private final String id = Integer.toString(nextId++);

    private final long creationTime = System.currentTimeMillis();

    private int maxInactiveInterval;

    private long lastAccessedTime = System.currentTimeMillis();

    private final ServletContext servletContext;

    private final Hashtable attributes = new Hashtable();

    private boolean invalid = false;

    private boolean isNew = true;


    /**
     * Create a new MockHttpSession.
     * @param servletContext the ServletContext that the session runs in
     */
    public ScheduleHttpSession(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getId() {
        return id;
    }

    public void access() {
        this.lastAccessedTime = System.currentTimeMillis();
        this.isNew = false;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("getSessionContext");
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return this.attributes.keys();
    }

    public String[] getValueNames() {
        return (String[]) this.attributes.keySet().toArray(new String[this.attributes.size()]);
    }

    public void setAttribute(String name, Object value) {
        if (value != null) {
            this.attributes.put(name, value);
        }
        else {
            this.attributes.remove(name);
        }
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public void removeValue(String name) {
        removeAttribute(name);
    }

    public void invalidate() {
        this.invalid = true;
        this.attributes.clear();
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setNew(boolean value) {
        isNew = value;
    }

    public boolean isNew() {
        return isNew;
    }

}