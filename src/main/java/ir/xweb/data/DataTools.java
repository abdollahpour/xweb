/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class DataTools {

    private final static Logger logger = LoggerFactory.getLogger("DataTools");

    public void write(
            final Object object,
            final Map<String, String> data,
            final String role) throws IOException {

        try {
            final String r = (role == null ? null : (role + ","));
            final List<?> keys = new ArrayList<Object>(data.keySet());

            Field[] fields = object.getClass().getFields();

            for(Field f:fields) {
                String name = f.getName();
                String validator = "";

                if(f.isAnnotationPresent(XWebDataElement.class)) {
                    final String aName = f.getAnnotation(XWebDataElement.class).key();
                    final String aRole = f.getAnnotation(XWebDataElement.class).role();
                    validator = f.getAnnotation(XWebDataElement.class).validator();

                    if(aName.length() > 0) {
                        if(r != null && aRole.length() > 0 && (aRole + ",").indexOf(r) == -1) {
                            throw new IllegalAccessException("No sufficient role. " + role + " not in " + aRole);
                        }
                        name = aName;
                    }
                }


                if(keys.contains(name)) {
                    final String value = data.get(name);

                    // validate of require
                    if(validator.length() > 0 && !value.matches(validator)) {
                        throw new IllegalAccessException("Illegal value: " + value + " not match " + validator);
                    }

                    Class<?> type = f.getType();

                    if(type == String.class) {
                        f.set(object, value.toString());
                    } else if(type == Integer.class || type == int.class) {
                        f.set(object, Integer.valueOf(value));
                    } else if(type == Boolean.class || type == boolean.class) {
                        f.setBoolean(object, Boolean.parseBoolean(value));
                    } else if(type == Byte.class || type == byte.class) {
                        f.setByte(object, Byte.valueOf(value).byteValue());
                    } else if(type == Character.class || type == char.class) {
                        if(value.length() == 0) {
                            f.setChar(object, value.charAt(0));
                        }
                    } else if(type == Double.class || type == double.class) {
                        f.setDouble(object, Double.parseDouble(value));
                    } else if(type == Float.class || type == float.class) {
                        f.setFloat(object, Float.parseFloat(value));
                    } else if(type == Long.class || type == long.class) {
                        f.setLong(object, Long.parseLong(value));
                    } else if(type == Short.class || type == short.class) {
                        f.setShort(object, Short.parseShort(value));
                    }

                    keys.remove(name);
                }
            }

            Method[] methods = object.getClass().getMethods();
            for(Method m:methods) {
                String name = null;
                String validator = "";

                if(m.isAnnotationPresent(XWebDataElement.class)) {
                    final String aName = m.getAnnotation(XWebDataElement.class).key();
                    final String aRole = m.getAnnotation(XWebDataElement.class).role();
                    validator = m.getAnnotation(XWebDataElement.class).validator();

                    if(aName.length() > 0) {
                        if(r != null && aRole.length() > 0 && (aRole + ",").indexOf(r) == -1) {
                            throw new IllegalAccessException("No sufficient role. " + role + " not in " + aRole);
                        }
                        name = aName;
                    }
                }

                String value = null;
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
                } else {
                    value = data.get(name);
                }

                if(value != null) {
                    // validate of require
                    if(validator.length() > 0 && !value.matches(validator)) {
                        throw new IllegalAccessException("Illegal value: " + value + " not match " + validator);
                    }

                    Class<?> type = m.getParameterTypes()[0];

                    if(type == String.class) {
                        m.invoke(object, value.toString());
                    } else if(type == Integer.class || type == int.class) {
                        m.invoke(object, Integer.valueOf(value));
                    } else if(type == Boolean.class || type == boolean.class) {
                        m.invoke(object, Boolean.parseBoolean(value));
                    } else if(type == Byte.class || type == byte.class) {
                        m.invoke(object, Byte.valueOf(value).byteValue());
                    } else if(type == Character.class || type == char.class) {
                        if(value.length() == 0) {
                            m.invoke(object, value.charAt(0));
                        }
                    } else if(type == Double.class || type == double.class) {
                        m.invoke(object, Double.parseDouble(value));
                    } else if(type == Float.class || type == float.class) {
                        m.invoke(object, Float.parseFloat(value));
                    } else if(type == Long.class || type == long.class) {
                        m.invoke(object, Long.parseLong(value));
                    } else if(type == Short.class || type == short.class) {
                        m.invoke(object, Short.parseShort(value));
                    }

                    keys.remove(name);
                }
            }
        } catch (InvocationTargetException ex) {
            throw new IOException(ex);
        } catch (IllegalAccessException ex) {
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
        if(format == null) {
            throw new IllegalArgumentException("null format");
        }
        if(object == null) {
            throw new IllegalArgumentException("null object");
        }

        final Formatter formatter;
        if("json".equals(format)) {
            formatter = new JsonFormatter();
        } else if("xml".equals(format)) {
            formatter = new XmlFormatter();
        } else {
            throw new IllegalArgumentException("Illegal formatter");
        }

        try {
            // it's much faster to search for 'ROLE,' to split and check in array
            final Object formattedObject = convert(object, role == null ? null : (role + ","));
            Writer writer = response.getWriter();
            formatter.write(writer, formattedObject);
            writer.flush();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private Object convert(
            final Object object,
            final String role) throws IOException, InvocationTargetException, IllegalAccessException {

        final Class c = object.getClass();
        final boolean isAnnoted = c.isAnnotationPresent(XWebData.class);

        if(isAnnoted) {
            String name = object.getClass().getAnnotation(XWebData.class).name();

            final AnnotedMap data = new AnnotedMap(name);

            final Method[] methods = c.getMethods();
            for(Method m:methods) {
                if(m.isAnnotationPresent(XWebDataElement.class)) {
                    if(m.getParameterTypes().length == 0) {
                        final String aName = m.getAnnotation(XWebDataElement.class).key();
                        final String aRole = m.getAnnotation(XWebDataElement.class).role();

                        if(role == null || aRole.length() > 0 || (aRole + ",").indexOf(role) > -1) {
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
                    final String aRole = f.getAnnotation(XWebDataElement.class).role();

                    if(role == null || aRole.length() > 0 || (aRole + ",").indexOf(role) > -1) {
                        final Object value = f.get(object);

                        if(aName == null || aName.length() == 0) {
                            data.put(f.getName(), convert(value, role));
                        } else {
                            data.put(aName, convert(value, role));
                        }
                    }

                }
            }

            return data;
        } else if(object instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) object;
            final Map<String, Object> data = new HashMap<String, Object>();

            for(Map.Entry<?, ?> e:map.entrySet()) {
                data.put(e.getKey().toString(), convert(e.getValue(), role));
            }

            return data;
        } else if(object instanceof Collection) {
            final Collection<?> Collection = (Collection<?>) object;

            final List<Object> data = new ArrayList<Object>();

            for(Object o:Collection) {
                data.add(convert(o, role));
            }

            return data;
        }

        return object;
    }

    private interface Formatter {

        public void write(final Writer writer, final Object object) throws IOException;

    }

    private class JsonFormatter implements Formatter {

        @Override
        public void write(final Writer writer, final Object object) throws IOException {
            try {
                if(object instanceof Map) {
                    writer.write(write((Map)object).toString());
                } else {
                    writer.write(object.toString());
                }
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
        }

        private Object write(final Object object) throws JSONException {
            if(object instanceof Map) {
                final JSONObject json = new JSONObject();

                final Map<?, ?> map = (Map<?, ?>) object;
                for(Map.Entry<?, ?> e:map.entrySet()) {
                    json.put(e.getKey().toString(), write(e.getValue()));
                }

                return json;
            } else if(object instanceof List) {
                final JSONArray array = new JSONArray();

                final List<?> list = (List<?>) object;
                for(Object o:list) {
                    array.put(write(o));
                }

                return array;
            }
            return object;
        }

    }

    private class XmlFormatter implements Formatter {

        public void write(final Writer writer, final Object object) throws IOException {
            //final String key = map.keySet().iterator().next().toString();
            //final Object value = map.get(key);
            if(object instanceof Map) {
                final Map<?, ?> map = (Map) object;
                if(map.size() > 0) {
                    Element root;
                    if(map.size() == 1) {
                        String key = map.keySet().iterator().next().toString();
                        root = new Element(key);
                        write(root, map.values().iterator().next());
                    } else {
                        root = new Element("data");
                        write(root, map);
                    }

                    final Document document = new Document(root);
                    final XMLOutputter xmlOutput = new XMLOutputter();
                    xmlOutput.setFormat(Format.getCompactFormat());
                    xmlOutput.output(document, writer);
                }
            } else {
                writer.write(object.toString());
            }
        }

        private void write(Element parent, Object object) throws IOException {
            if(object instanceof Map) {

                final Map<?, ?> map =  (Map<?, ?>) object;
                for(Map.Entry<?, ?> e:map.entrySet()) {
                    final Element element = new Element(e.getKey().toString());
                    write(element, e.getValue());
                    parent.addContent(element);
                }
            } else if(object instanceof List) {
                final List<?> list = (List<?>) object;

                for(Object o:list) {
                    String key;
                    if(o instanceof AnnotedMap) {
                        key = ((AnnotedMap)o).name;
                    } else if(o instanceof String) {
                        key = "string";
                    } else if(o instanceof Integer) {
                        key = "integer";
                    } else if(o instanceof Float) {
                        key = "float";
                    } else if(o instanceof Long) {
                        key = "long";
                    } else if(o instanceof Double) {
                        key = "double";
                    } else {
                        key = o.getClass().getName();
                    }

                    Element e = new Element(key);
                    write(e, o);
                    parent.addContent(e);
                }
            } else {
                parent.setText(object.toString());
            }
        }

    }

    private class AnnotedMap extends HashMap<String, Object> {

        final String name;

        AnnotedMap(String name) {
            this.name = name;
        }

    }

}
