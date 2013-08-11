package ir.xweb.data;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.System;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DataService {

    //private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    public final static String WRITER_XML = "xml";

    public final static String WRITER_JSON = "json";

    public final static String WRITER_JSONP = "jsonp";

    private Map<String, ElementGenerator> generators = new HashMap<String, ElementGenerator>();

    private DataWriter writer;

    private HashMap<String, DataWriter> writers = new HashMap<String, DataWriter>();

    private List<String> writedClass = new ArrayList<String>();

    private Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

    private DataService() {

    }

    public void setTimeZone(TimeZone timeZone) {
        calendar.setTimeZone(timeZone);
    }

    public TimeZone getTimeZone() {
        return calendar.getTimeZone();
    }

    private void load(String... packageName) throws IOException {
        //ArrayList<? extends XWebData> list = new ArrayList<XWebData>();

        ArrayList<Class<?>> annotations = new ArrayList<Class<?>>();
        Instrumentation inst = InstrumentHook.getInstrumentation();
        if(inst != null) {
            for (Class<?> clazz:inst.getAllLoadedClasses()) {
                String cn = clazz.getName();
                for(String pn:packageName) {
                    if(cn.matches(pn)) {
                        if(clazz.isAnnotationPresent(XWebData.class)) {
                            annotations.add(clazz);
                        }
                        break;
                    }
                }
            }
        } else {
            for(String pn:packageName) {
                // trim to just check for class name
                if(pn.endsWith(".*")) {
                    pn = pn.substring(0, pn.length() - 2);
                }

                Class<?>[] classes = InstrumentHook.getClasses(pn);
                for(Class<?> clazz:classes) {
                    if(clazz.isAnnotationPresent(XWebData.class)) {
                        annotations.add(clazz);
                    }
                }
            }
        }

        System.out.println(annotations.size() + " class for dataservice found");
        for(Class<?> c:annotations) {
            System.out.println("\t" + c);
            Method[] methods = c.getMethods();
            ArrayList<Method> methodList = new ArrayList<Method>();
            for(Method m:methods) {
                if(m.isAnnotationPresent(XWebDataElement.class)) {
                    methodList.add(m);
                }
            }

            Field[] fields = c.getFields();
            ArrayList<Field> fieldList = new ArrayList<Field>();
            for(Field f:fields) {
                if(f.isAnnotationPresent(XWebDataElement.class)) {
                    fieldList.add(f);
                }
            }

            if(methodList.size() > 0 || fieldList.size() > 0) {
                String name = c.getAnnotation(XWebData.class).name();
                ElementGenerator g = new ElementGenerator(name, methodList, fieldList);
                generators.put(c.getName(), g);
            }
        }
    }

    public static DataService newDataWriter(String... packageName) throws IOException {
        DataService dataService = new DataService();

        dataService.load(packageName);
        dataService.writers.put(WRITER_JSON, new JsonDataWriter());
        dataService.writers.put(WRITER_JSONP, new JsonPDataWriter());
        dataService.writers.put(WRITER_XML, new XmlDataWriter());

        return dataService;
    }


    public void addDataWriter(String name, DataWriter writer) {
        writers.put(name, writer);
    }

    public void setValues(Object object, Map<String, String> values, List<String> filter) {
        Map<String, String> v = new HashMap<String, String>(values.size());

        if(filter == null) {
            v = values;
        } else {
            for(String f:filter) {
                if(values.containsKey(f)) {
                    v.put(f, values.get(f));
                }
            }
        }

        ElementGenerator g = generators.get(object.getClass().getName());
        if(g != null) {
            g.setValues(this, object, v);
        } else {
            throw new IllegalArgumentException("Object type not find in dataservice: " + object.getClass());
        }
    }

    public void setValues(Object object, Map<String, String> values) {
        setValues(object, values, null);
    }

    public void write(HttpServletResponse response, String format, Object object) throws IOException {
        writer = writers.get(format);
        if(writer == null) {
            throw new IOException("format type not found: " + format);
        }
        try {
            // Set standard HTTP/1.1 no-cache headers.
            response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
            // Set standard HTTP/1.0 no-cache header.
            response.setHeader("Pragma", "no-cache");
            response.setContentType(writer.getContentType());

            writeObject("data", object);
            writer.write(response.getWriter());
        } catch (IllegalAccessException ex) {
            writer.release();
            throw new IOException(ex);
        }

        writer.release();
    }

    public void write(Writer w, String format, Object object) throws IOException {
        writer = writers.get(format);
        if(writer == null) {
            throw new IOException("format type not found: " + format);
        }
        try {
            writeObject("data", object);
            writer.write(w);
        } catch (IllegalAccessException ex) {
            writer.release();
            throw new IOException(ex);
        }

        writer.release();
    }

    public void writeObject(String key, Object object) throws IllegalAccessException {
        writedClass.clear();
        _writeObject(key, object);
    }

    protected void _writeObject(String key, Object object) throws IllegalAccessException {
        String className = object.getClass().getName();
        ElementGenerator g = generators.get(className);

        if(g != null) {
            // we can not let use recursive data
            if(writedClass.contains(className)) {
                return;
            }
            writedClass.add(className);

            writer.start(key);
            g.generateValues(this, object);
            writer.end();

            writedClass.remove(className);
        } else if(object instanceof Collection) {
            Collection<?> c = (Collection<?>)object;

            writer.startArray(key);
            for(Object o:c) {
                String _key = o.getClass().getSimpleName();

                ElementGenerator _g = generators.get(o.getClass().getName());
                if(_g != null && _g.getName() != null && _g.getName().length() > 0) {
                    _key = _g.getName();
                }

                _writeObject(_key, o);
            }
            writer.end();
        } else if(object.getClass().isArray()) {
            int size = Array.getLength(object);

            writer.startArray(key);
            for(int i=0; i<size; i++) {
                Object o = Array.get(object, i);
                String _key = o.getClass().getSimpleName();

                ElementGenerator _g = generators.get(o.getClass().getName());
                if(_g != null && _g.getName() != null && _g.getName().length() > 0) {
                    _key = _g.getName();
                }

                _writeObject(_key, o);
            }
            writer.end();
        } else if(object instanceof Map) {
            Map<?, ?> m = (Map<?, ?>)object;

            writer.start(key);
            for(Object k:m.keySet()) {
                _writeObject(k.toString(), m.get(k));
            }
            writer.end();
        } else if(object instanceof Date) {
            calendar.setTime((Date)object);
            StringBuilder s = new StringBuilder();

            s.append(calendar.get(Calendar.YEAR)).append("-");
            s.append(calendar.get(Calendar.MONTH) + 1).append("-");
            s.append(calendar.get(Calendar.DAY_OF_MONTH)).append(" ");
            s.append(calendar.get(Calendar.HOUR_OF_DAY)).append(":");
            s.append(calendar.get(Calendar.MINUTE)).append(":");
            s.append(calendar.get(Calendar.SECOND)).append(".");
            s.append(calendar.get(Calendar.MILLISECOND));

            writer.add(key, s.toString());
        } else {
            writer.add(key, object);
        }
    }

}
