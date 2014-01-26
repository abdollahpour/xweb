package ir.xweb.module;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ValidModuleParam extends ModuleParam {

    private final String name;

    protected ValidModuleParam(final Map<String, String> data, final List<String> defaults, final String name) {
        super(data, defaults);
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

}
