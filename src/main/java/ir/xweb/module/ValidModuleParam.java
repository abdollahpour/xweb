package ir.xweb.module;

import java.net.URL;
import java.util.Locale;
import java.util.Map;

public class ValidModuleParam extends ModuleParam {

    //private Map<String, String> data = null;

    private String name;

    protected ValidModuleParam(Map<String, String> data, String name) {
        super(data);
        this.name = name;
    }

    public String get(String def) {
        return super.getString(name, def);
    }

    public String getString(String def) {
        return getString(name, def);
    }

    public Integer getInt(Integer def) {
        return getInt(name, def);
    }

    public Float getFloat(Float def) {
        return getFloat(name, def);
    }

    public Double getDouble(Double def) {
        return getDouble(name, def);
    }

    public Long getLong(Long def) {
        return getLong(name, def);
    }

    public Byte getByte(Byte def) {
        return getByte(name, def);
    }

    public URL getURL(URL def) {
        return getURL(name, def);
    }

    public Locale getLocale(Locale def) {
        return getLocale(name, def);
    }

    public Boolean getBoolean(Boolean def) {
        return getBoolean(def);
    }

}
