
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import ir.xweb.module.ModuleParam;
import ir.xweb.util.MimeType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTools {

    private final static Logger logger = LoggerFactory.getLogger("DataTools");

    public final static String PROPERTY_JSONP_CALLBACK = "jsonp.callback";

    public final static String FORMAT_JSON = "json";

    public final static String FORMAT_JSONP = "jsonp";

    public final static String FORMAT_XML = "xml";

    public final static String FORMAT_XML1 = "xml1";

    public final static String FORMAT_XML2 = "xml2";

    private final Map<String, ir.xweb.data.Formatter> formatters = new HashMap<String, ir.xweb.data.Formatter>();

    private final HashMap<String, String> properties = new HashMap<String, String>();

    private DateFormat dateFormat = new SimpleDateFormat();

    public DataTools() {
        formatters.put(FORMAT_JSON, new JsonFormatter());
        formatters.put(FORMAT_JSONP, new JsonpFormatter());
        formatters.put(FORMAT_XML, new XmlFormatter());
        formatters.put(FORMAT_XML1, new XmlFormatter1());
        formatters.put(FORMAT_XML2, new XmlFormatter2());
    }

    public ir.xweb.data.Formatter addFormatter(final String name, final ir.xweb.data.Formatter formatter) {
        return formatters.put(name, formatter);
    }

    public ir.xweb.data.Formatter getFormatter(final String name) {
        final ir.xweb.data.Formatter formatter = formatters.get(name);
        return formatter;
    }

    public void setDateFormat(final DateFormat dateFormat) {
        if(dateFormat == null) {
            throw new IllegalArgumentException("null dataFormat");
        }
        this.dateFormat = dateFormat;
    }

    public String addProperties(String property, String value) {
        if(value == null) {
            return properties.remove(property);
        } else {
            return properties.put(property, value);
        }
    }

    /**
     * Write parameter to an specific object
     * @param object
     * @param data
     * @param role
     * @throws IOException
     */
    public void write(
            final Object object,
            final Map<String, ?> data,
            final String role) throws IOException
    {

        try {
            final List<?> keys = new ArrayList<Object>(data.keySet());

            final Field[] fields = object.getClass().getFields();

            for(Field f:fields) {
                String name = f.getName();
                String validator = "";

                if(f.isAnnotationPresent(XWebDataElement.class)) {
                    final String aName = f.getAnnotation(XWebDataElement.class).key();
                    final String aRole = f.getAnnotation(XWebDataElement.class).write();
                    validator = f.getAnnotation(XWebDataElement.class).validator();

                    if(aName.length() > 0) {
                        if(role != null && aRole.length() > 0 && !role.matches(aRole)) {
                            throw new IllegalAccessException("No sufficient role. " + role + " not in " + aRole);
                        }
                        name = aName;
                    }
                }


                if(keys.contains(name)) {
                    final Object value = data.get(name);

                    if(value != null) {
                        final String s = value.toString();

                        // validate of require
                        if(validator.length() > 0 && !s.matches(validator)) {
                            logger.trace("Illegal value: " + value + " not match " + validator);
                            throw new DataToolsException(name);
                        }

                        Class<?> type = f.getType();

                        if(type == String.class) {
                            f.set(object, value.toString());
                        }
                        else if(type == Integer.class || type == int.class) {
                            if(value instanceof Integer) {
                                f.set(object, value);
                            } else {
                                f.set(object, Integer.valueOf(s));
                            }
                        }
                        else if(type == Boolean.class || type == boolean.class) {
                            if(value instanceof Boolean) {
                                f.setBoolean(object, (Boolean)value);
                            } else {
                                f.setBoolean(object, Boolean.parseBoolean(s));
                            }
                        }
                        else if(type == Byte.class || type == byte.class) {
                            f.set(object, (value instanceof Byte) ? value : Byte.parseByte(s));
                        }
                        else if(type == Character.class || type == char.class) {
                            if(s.length() != 0) {
                                f.set(object, (value instanceof Character) ? value : s.charAt(0));
                            }
                        }
                        else if(type == Double.class || type == double.class) {
                            f.set(object, (value instanceof Double) ? value : Double.parseDouble(s));
                        }
                        else if(type == Float.class || type == float.class) {
                            f.set(object, (value instanceof Float) ? value : Float.parseFloat(s));
                        }
                        else if(type == Long.class || type == long.class) {
                            f.set(object, (value instanceof Long) ? value : Long.parseLong(s));
                        }
                        else if(type == Short.class || type == short.class) {
                            f.set(object, (value instanceof Short) ? value : Short.parseShort(s));
                        }

                        keys.remove(name);
                    }
                }
            }

            final Method[] methods = object.getClass().getMethods();
            for(Method m:methods) {
                String name = null;
                String validator = "";

                if(m.isAnnotationPresent(XWebDataElement.class)) {
                    final String aName = m.getAnnotation(XWebDataElement.class).key();
                    final String aRole = m.getAnnotation(XWebDataElement.class).write();
                    validator = m.getAnnotation(XWebDataElement.class).validator();

                    if(aName.length() > 0) {
                        if(role != null && aRole.length() > 0 && !role.matches(aRole)) {
                            throw new IllegalAccessException("No sufficient role. " + role + " not in " + aRole);
                        }
                        name = aName;
                    }
                }

                Object value = null;
                if(name == null) {
                    name = m.getName();
                    if(keys.contains(name)) {
                        value = data.get(name);
                    } else if(name.startsWith("set")) {
                        // make setter function
                        String setterName = name.substring(3);
                        setterName = setterName.substring(0, 1).toLowerCase() + setterName.substring(1);

                        value = data.get(setterName);
                    }
                }
                else {
                    value = data.get(name);
                }

                if(value != null) {
                    final String s = value.toString();

                    // validate of require
                    if(validator.length() > 0 && !s.matches(validator)) {
                        logger.trace("Illegal value: " + value + " not match " + validator);
                        throw new DataToolsException(name);
                    }

                    Class<?> type = m.getParameterTypes()[0];

                    if(type == String.class) {
                        m.invoke(object, value.toString());
                    }

                    else if(type == Integer.class || type == int.class) {
                        m.invoke(object, (value instanceof Integer) ? value : Integer.parseInt(s));
                    }
                    else if(type == Boolean.class || type == boolean.class) {
                        m.invoke(object, (value instanceof Boolean) ? value : Boolean.parseBoolean(s));
                    }
                    else if(type == Byte.class || type == byte.class) {
                        m.invoke(object, (value instanceof Byte) ? value : Byte.valueOf(s).byteValue());
                    }
                    else if(type == Character.class || type == char.class) {
                        if(s.length() == 1) {
                            m.invoke(object, s.charAt(0));
                        }
                    }
                    else if(type == Double.class || type == double.class) {
                        m.invoke(object, (value instanceof Double) ? value : Double.parseDouble(s));
                    }
                    else if(type == Float.class || type == float.class) {
                        m.invoke(object, (value instanceof Float) ? value : Float.parseFloat(s));
                    }
                    else if(type == Long.class || type == long.class) {
                        m.invoke(object, (value instanceof Long) ? value : Long.parseLong(s));
                    }
                    else if(type == Short.class || type == short.class) {
                        m.invoke(object, (value instanceof Short) ? value : Short.parseShort(s));
                    }

                    keys.remove(name);
                }
            }
        }
        catch (DataToolsException ex) {
            throw ex;
        }
        catch (InvocationTargetException ex) {
            throw new IOException(ex);
        }
        catch (IllegalAccessException ex) {
            throw new IOException(ex);
        }
    }

    public void write(
            final HttpServletResponse response,
            final String format,
            final String role,
            final Object object) throws IOException {
        if(response == null) {
            throw new IllegalArgumentException("null response");
        }

        ir.xweb.data.Formatter formatter = formatters.get(format);
        if(formatter != null) {
            if(!response.containsHeader("Content-Type")) {
                response.addHeader("Content-Type", formatter.getContentType());
                response.setCharacterEncoding("UTF-8");
            }
        }

        write(response.getWriter(), format, role, object);
    }

    public String write(
            final String format,
            final String role,
            final Object object) throws IOException {

        final StringWriter writer = new StringWriter();
        write(writer, format, role, object);
        return writer.getBuffer().toString();
    }

    public void write(
            final Writer writer,
            final String format,
            final String role,
            final Object object) throws IOException {
        write(writer, format, role, object, false);
    }

    public void write(
            final Writer writer,
            final String format,
            final String role,
            final Object object,
            final boolean expandListToParent) throws IOException {

        if(writer == null) {
            throw new IllegalArgumentException("null writer");
        }
        if(format == null) {
            throw new IllegalArgumentException("null format");
        }
        if(object == null) {
            throw new IllegalArgumentException("null object");
        }

        final ir.xweb.data.Formatter formatter = formatters.get(format);
        if(formatter == null) {
            throw new IllegalArgumentException("Illegal formatter");
        }

        try {
            // it's much faster to search for 'ROLE,' to split and check in array
            final Object formattedObject = convert(object, role == null ? null : (role + ","));
            formatter.write(writer, formattedObject);
            writer.flush();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private Object convert(
            final Object object,
            final String role) throws IOException, InvocationTargetException, IllegalAccessException {

        if(object == null) {
            return null;
        }

        final Class c = object.getClass();
        final boolean isAnnoted = c.isAnnotationPresent(XWebData.class);

        if(isAnnoted) {
            final String name = object.getClass().getAnnotation(XWebData.class).name();

            final AnnotedMap data = new AnnotedMap(name.length() == 0 ? object.getClass().getSimpleName() : name);

            final Method[] methods = c.getMethods();
            for(Method m:methods) {
                if(m.isAnnotationPresent(XWebDataElement.class)) {
                    if(m.getParameterTypes().length == 0) {
                        final String aName = m.getAnnotation(XWebDataElement.class).key();
                        final String aRole = m.getAnnotation(XWebDataElement.class).read();

                        if(role == null || aRole.length() == 0 || role.matches(aRole)) {
                            Object value = m.invoke(object);

                            if(aName == null || aName.length() == 0) {
                                data.put(m.getName(), convert(value, role));
                            } else {
                                data.put(aName, convert(value, role));
                            }
                        }
                    } else {
                        logger.warn("Error to get Method value, the method should not have any parameter");
                    }
                }
            }

            final Field[] fields = c.getFields();
            for(Field f:fields) {
                if(f.isAnnotationPresent(XWebDataElement.class)) {
                    final String aName = f.getAnnotation(XWebDataElement.class).key();
                    final String aRole = f.getAnnotation(XWebDataElement.class).read();

                    if(role == null || aRole.length() == 0 || role.matches(aRole)) {
                        final Object value = f.get(object);

                        if(value != null) {
                            if(aName == null || aName.length() == 0) {
                                data.put(f.getName(), convert(value, role));
                            } else {
                                data.put(aName, convert(value, role));
                            }
                        }
                    }

                }
            }

            return data;
        } else if(object instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) object;
            final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();

            for(Map.Entry<?, ?> e:map.entrySet()) {
                if(e.getValue() != null) {
                    data.put(e.getKey().toString(), convert(e.getValue(), role));
                } else {
                    data.put(e.getKey().toString(), "null");
                }
            }

            return data;
        } else if(object instanceof Collection) {
            final Collection<?> Collection = (Collection<?>) object;

            final List<Object> data = new ArrayList<Object>();

            for(Object o:Collection) {
                data.add(convert(o, role));
            }

            return data;
        } else if(object instanceof Date) {
            return dateFormat.format((Date) object);
        }

        return object;
    }

    public ModuleParam read(
            final String html) throws IOException {
        return read(html, null);
    }

    public ModuleParam read(
            final String html,
            final Map<String, String> env) throws IOException {

        try {
            final SAXBuilder builder = new SAXBuilder();
            final Document document = builder.build(new StringReader(html));
            final Element root = document.getRootElement();

            return read(root, env);
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
    }

    public ModuleParam read(
        final Element element,
        final Map<String, String> env) {

        final List<Element> children = element.getChildren();
        final HashMap<String, Object> data = new HashMap<String, Object>();

        for(Element e:children) {
            String name = e.getAttributeValue("key");
            if(name == null) {
                name = e.getName();
            }

            Object value = e.getAttributeValue("value");
            if(value == null) {
                if(e.getChildren().size() == 0) {
                    final String text = e.getText();
                    if(env != null) {
                        value = applyEnvironmentVariable(text, env);
                    } else {
                        value = text;
                    }
                }
                else {
                    value = read(e, env);
                }
            }
            final Object old = data.get(name);

            if(old != null) {
                if(old instanceof List) {
                    ((List)old).add(value);
                }
                else {
                    final List list = new ArrayList(2);
                    list.add(old);
                    list.add(value);
                    data.put(name, list);
                }
            }
            else {
                data.put(name, value);
            }
        }

        return new ModuleParam(data);
    }

    private String applyEnvironmentVariable(final String s, final Map<String, String> env) {
        final Pattern pattern = Pattern.compile("\\$\\{([^}]*)\\}");

        final Matcher m = pattern.matcher(s);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String text = m.group(1);
            final String value = env.get(text);
            m.appendReplacement(sb, value == null ? "" : value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private class JsonpFormatter extends JsonFormatter {

        private final String mimeType = MimeType.get("json");

        @Override
        public void write(final Writer writer, final Object object) throws IOException {
            try {
                if(object instanceof Map) {
                    final String callback = properties.get(PROPERTY_JSONP_CALLBACK);
                    if(callback == null) {
                        writer.write("jsonCallback(");
                    } else {
                        writer.append(callback).write("(");
                    }
                    ((JSONObject) write(object)).write(writer);
                    writer.write(");");

                } else {
                    writer.write("jsonCallback(");
                    ((JSONArray) write(object)).write(writer);
                    writer.write(");");
                }
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public String getContentType() {
            return mimeType;
        }

    }

}
