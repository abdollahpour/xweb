package ir.xweb.module;

import java.io.File;
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
        return super.getString(this.name, def);
    }

    public String get() {
        return super.getString(this.name, null);
    }

    public String getString(String def) {
        return getString(this.name, def);
    }

    public String getString() {
        return getString(this.name, null);
    }

    public Integer getInt(Integer def) {
        return getInt(this.name, def);
    }

    public Integer getInt() {
        return getInt(this.name, null);
    }

    public Float getFloat(Float def) {
        return getFloat(this.name, def);
    }

    public Float getFloat() {
        return getFloat(this.name, null);
    }

    public Double getDouble(Double def) {
        return getDouble(this.name, def);
    }

    public Double getDouble() {
        return getDouble(this.name, null);
    }

    public Long getLong(Long def) {
        return getLong(this.name, def);
    }

    public Long getLong() {
        return getLong(this.name, null);
    }

    public Byte getByte(Byte def) {
        return getByte(this.name, def);
    }

    public Byte getByte() {
        return getByte(this.name, null);
    }

    public URL getURL(URL def) {
        return getURL(this.name, def);
    }

    public URL getURL() {
        return getURL(this.name, null);
    }

    public Locale getLocale(Locale def) {
        return getLocale(this.name, def);
    }

    public Locale getLocale() {
        return getLocale(this.name, null);
    }

    public Boolean getBoolean(Boolean def) {
        return getBoolean(def);
    }

    public Boolean getBoolean() {
        return getBoolean(this.name, null);
    }

    public Date getDate(final String pattern) {
        return getDate(this.name, pattern, null, null);
    }

    public Date getDate(final String pattern, final Date def) {
        return getDate(this.name, pattern, null, def);
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
            throw new IllegalArgumentException(
                "Value for " + this.name + " (" + s + ") is not valid for " + pattern + " pattern",
                ex);
        }
    }

    public File getFile() {
        return getFile(this.name, null);
    }

}
