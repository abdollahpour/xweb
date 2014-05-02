package ir.xweb.module;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class ValidModuleParam extends ModuleParam {

    private final String name;

    protected ValidModuleParam(final ModuleParam param, final String name) {
        super(param);
        this.name = name;
    }

    public String get(String def) {
        return super.getString(name, def);
    }

    public String get() {
        return super.getString(name, null);
    }

    public String getString(String def) {
        return getString(name, def);
    }

    public String getString() {
        return getString(name, null);
    }

    public Integer getInt(Integer def) {
        return getInt(name, def);
    }

    public Integer getInt() {
        return getInt(name, null);
    }

    public Float getFloat(Float def) {
        return getFloat(name, def);
    }

    public Float getFloat() {
        return getFloat(name, null);
    }

    public Double getDouble(Double def) {
        return getDouble(name, def);
    }

    public Double getDouble() {
        return getDouble(name, null);
    }

    public Long getLong(Long def) {
        return getLong(name, def);
    }

    public Long getLong() {
        return getLong(name, null);
    }

    public Byte getByte(Byte def) {
        return getByte(name, def);
    }

    public Byte getByte() {
        return getByte(name, null);
    }

    public URL getURL(URL def) {
        return getURL(name, def);
    }

    public URL getURL() {
        return getURL(name, null);
    }

    public Locale getLocale(Locale def) {
        return getLocale(name, def);
    }

    public Locale getLocale() {
        return getLocale(name, null);
    }

    public Boolean getBoolean(Boolean def) {
        return getBoolean(def);
    }

    public Boolean getBoolean() {
        return getBoolean(name, null);
    }

    public Date getDate(final String pattern) {
        return getDate(name, pattern, null, null);
    }

    public Date getDate(final String pattern, final Date def) {
        return getDate(name, pattern, null, def);
    }

    public Date getDate(final String pattern, final TimeZone zone) {
        return null;
    }

    public Date getDate(final String pattern, final TimeZone zone, final Date def) {
        final SimpleDateFormat format = new SimpleDateFormat(pattern);
        if(zone != null) {
            format.setTimeZone(zone);
        }
        final String s = getString();
        try {
            return (s == null || s.length() == 0) ? def : format.parse(s);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Value for " + name + " (" + s + ") is not valid for " + pattern + " pattern", ex);
        }
    }

}
