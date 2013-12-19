package ir.xweb.module;

import java.io.File;
import java.lang.String;
import java.net.URL;
import java.util.*;

public class ModuleParam implements Map<String, String> {

    private Map<String, String> data;

    public ModuleParam(Map<String, String> data) {
        this.data = data;
    }

    public String get(String name, String def) {
        return getString(name, def);
    }

    public String getString(String name, String def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : s;
    }

    public Integer getInt(String name, Integer def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Integer.parseInt(s);
    }

    public Float getFloat(String name, Float def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Float.parseFloat(s);
    }

    public Double getDouble(String name, Double def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Double.parseDouble(s);
    }

    public Long getLong(String name, Long def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Long.parseLong(s);
    }

    public Byte getByte(String name, Byte def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Byte.parseByte(s);
    }

    public Boolean getBoolean(String name, Boolean def) {
        String s = data.get(name);
        return (s == null || s.length() == 0) ? def : Boolean.parseBoolean(s);
    }

    public URL getURL(String name, URL def) {
        String s = data.get(name);
        try {
            return (s == null || s.length() == 0) ? def : new URL(s);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal URL: " + s);
        }
    }

    public File getFile(String name, File def) {
        String path = getString(name, null);
        return path == null ? def : new File(path);
    }

    public File getFile(String name, String defPath) {
        String path = getString(name, defPath);
        return path == null ? null : new File(path);
    }

    public Locale getLocale(String name, Locale def) {
        String s = data.get(name);
        if((s == null || s.length() == 0)) {
            return def;
        }

        Locale locale = parseLocale(s);
        if(!isValid(locale)) {
            throw new IllegalArgumentException("Locale is not valid. Name:" + name + ", Value: " + s);
        }
        return locale;
    }

    public <T> T get(Class<T> clazz, String name) {
        String value = get(name);
        if(value == null) {
            return null;
        }

        if(clazz.equals(Integer.class)) {
            return clazz.cast(Integer.parseInt(value));
        } else if(clazz.equals(Float.class)) {
            return clazz.cast(Float.parseFloat(value));
        } else if(clazz.equals(Double.class)) {
            return clazz.cast(Double.parseDouble(value));
        } else if(clazz.equals(Long.class)) {
            return clazz.cast(Long.parseLong(value));
        } else if(clazz.equals(Boolean.class)) {
            return clazz.cast(Boolean.parseBoolean(value));
        } else if(clazz.equals(String.class)) {
            return clazz.cast(value);
        }

        throw new IllegalArgumentException("type does not support: " + clazz);
    }

    /**
     * http://stackoverflow.com/questions/3684747/how-to-validate-a-locale-in-java
     * @param locale
     * @return
     */
    private boolean isValid(Locale locale) {
        try {
            return locale.getISO3Language() != null && locale.getISO3Country() != null;
        } catch (MissingResourceException e) {
            return false;
        }
    }

    /**
     * http://stackoverflow.com/questions/3684747/how-to-validate-a-locale-in-java
     * @param locale
     * @return
     */
    private Locale parseLocale(String locale) {
        if(locale == null) {
            throw new IllegalArgumentException("null locale");
        }

        String[] parts = locale.split("_");
        switch (parts.length) {
            case 3: return new Locale(parts[0], parts[1], parts[2]);
            case 2: return new Locale(parts[0], parts[1]);
            case 1: return new Locale(parts[0]);
            default: throw new IllegalArgumentException("Invalid locale: " + locale);
        }
    }

    public ValidModuleParam validate2(String name, Collection<?> values, boolean required) throws ModuleException {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        String value = data.get(name);
        if(required && (value == null || value.length() == 0)) {
            throw new ModuleException("Illegal parameter: " + name);
        }

        if(values != null && (value != null && value.length() > 0)) {
            if(!values.contains(value)) {
                throw new ModuleException("Invalid parameter. name: " + name + ", value: " + value);
            }
        }

        return new ValidModuleParam(data, name);
    }

    public ValidModuleParam exists(String name) throws ModuleException {
        return validate(name, null, true);
    }

    public ValidModuleParam validate(String name, String regex, boolean required) throws ModuleException {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        String value = data.get(name);
        if(required && (value == null || value.length() == 0)) {
            throw new ModuleException("Illegal parameter: " + name);
        }

        if(regex != null && (value != null && value.length() > 0)) {
            if(!value.matches(regex)) {
                throw new ModuleException("Invalid parameter. name: " + name + ", value: " + value);
            }
        }

        return new ValidModuleParam(data, name);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return data.get(key);
    }

    @Override
    public String put(String key, String value) {
        return data.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        data.putAll(m);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<String> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return data.entrySet();
    }
}
