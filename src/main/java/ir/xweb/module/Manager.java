/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.module;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

public class Manager {
	
	private final static Logger logger = LoggerFactory.getLogger("Manager");

    private final static String DEFAULT_PEROPERTOES_PERFIX = "default.";

	private final LinkedHashMap<String, Module> modules = new LinkedHashMap<String, Module>();

    private final Map<String, String> properties = new HashMap<String, String>();

    private final ServletContext context;

    private final List<ScheduledExecutorService> schedulers = new ArrayList<ScheduledExecutorService>();

    public Manager(final ServletContext context) {
        this.context = context;
    }

    public void load(final InputStream in) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        load(classLoader, in);
    }

    public void load(final ClassLoader classLoader, final InputStream in) throws IOException {
        try {
            final SAXBuilder builder = new SAXBuilder();
            final Document document = builder.build(in);
            final Element root = document.getRootElement();

            Map<String, String> env = getEnvMap(properties);

            final Element envPropertiesElement = root.getChild("properties");
            if(envPropertiesElement != null) {
                final List<?> propertyElements = envPropertiesElement.getChildren("property");
                for(Object o2:propertyElements) {
                    Element property = (Element) o2;
                    String key = property.getAttributeValue("key");
                    String value = property.getAttributeValue("value");
                    if(value != null) {
                        properties.put(key, value);
                    } else {
                        String text = applyEnvironmentVariable(env, property.getText());
                        properties.put(key, text);
                    }
                }
            }

            // Update environment variables with new system properties
            env = getEnvMap(properties);

            Element modulesElement = root.getChild("modules");
            if(modulesElement != null) {
                final List<?> moduleElements = modulesElement.getChildren("module");
                for(Object o:moduleElements) {
                    final Element model = (Element)o;

                    final String name = model.getChildText("name");
                    final String author = model.getChildText("author");
                    final String className = model.getChildText("class");

                    final List<Validator> validators = new ArrayList<Validator>();
                    final Element validatorsElement = model.getChild("validators");
                    if(validatorsElement != null) {
                        final List<?> validatorElements = validatorsElement.getChildren("validator");
                        for(Object o2:validatorElements) {
                            final Element v = (Element)o2;

                            validators.add(new Validator(
                                    v.getAttributeValue("param"),
                                    v.getAttributeValue("regex"),
                                    "true".equalsIgnoreCase(v.getAttributeValue("require"))
                            ));
                        }
                    }

                    final List<Role> roles = new ArrayList<Role>();
                    final Element rolesElement = model.getChild("roles");
                    if(rolesElement != null) {
                        final List<?> roleElements = rolesElement.getChildren("role");
                        for(Object o2:roleElements) {
                            final Element role = (Element)o2;

                            final Role r = new Role(
                                    role.getAttributeValue("param"),
                                    role.getAttributeValue("eval"),
                                    role.getAttributeValue("value"),
                                    role.getAttributeValue("definite")
                            );

                            roles.add(r);
                        }
                    }

                    final Map<String, String> properties = new HashMap<String, String>();

                    // add default parameters first. These parameters can override by module properties
                    for(Map.Entry<String, String> e:this.properties.entrySet()) {
                        if(e.getKey().startsWith(DEFAULT_PEROPERTOES_PERFIX)) {
                            properties.put(e.getKey().substring(DEFAULT_PEROPERTOES_PERFIX.length()), e.getValue());
                        }
                    }

                    final Element propertiesElement = model.getChild("properties");
                    if(propertiesElement != null) {
                        final List<?> propertyElements = propertiesElement.getChildren("property");
                        for(Object o2:propertyElements) {
                            final Element property = (Element)o2;
                            final String key = property.getAttributeValue("key");
                            if(key != null) {
                                final String value = property.getAttributeValue("value");
                                if(value != null) {
                                    properties.put(key, value);
                                } else {
                                    final String text = applyEnvironmentVariable(env, property.getText());
                                    properties.put(key, text);
                                }
                            } else {
                                logger.warn("Key not found for property (" + name + ")");
                            }
                        }
                    }

                    try {
                        final Info info = new Info(name, author, validators, roles);

                        final Class<?> c = Class.forName(className, false, classLoader);
                        final Constructor<?> cons = c.getConstructor(Manager.class, ModuleInfo.class, ModuleParam.class);
                        final Module module = (Module) cons.newInstance(this, info, new ModuleParam(properties));


                        // Add schedules now
                        final Element schedulesElement = model.getChild("schedules");
                        if(schedulesElement != null) {
                            final List<?> roleElements = schedulesElement.getChildren("schedule");
                            for(Object o2:roleElements) {
                                final Element schedule = (Element)o2;

                                final String start = schedule.getAttributeValue("start");
                                final String period = schedule.getAttributeValue("period");
                                final String query = schedule.getAttributeValue("query");

                                if(start == null && period == null) {
                                    throw new IllegalArgumentException("On of start or period should define for schedule");
                                }

                                long s = 0;
                                long p = 0;

                                if(start != null) {
                                    s = getStart(start);
                                }
                                // system load
                                s += 2000;

                                if(period != null) {
                                    p = getPeriod(period);
                                }

                                addSchedule(module, query, s, p);
                            }
                        }


                        modules.put(name, module);
                    } catch (Exception ex) {
                        throw new IOException("Error in load module: " + className + " (" + name + ")", ex);
                    }
                }
            }

            // now init modules with same order
            for(Map.Entry<String, Module> m:modules.entrySet()) {
                try {
                    m.getValue().init(context);
                } catch (Exception ex) {
                    logger.error("Error to init module: " + m.getClass().getName(), ex);
                }
            }
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
    }

    public void load(final URL url) throws IOException {
        load(url.openStream());
    }

    public void load(final ClassLoader classLoader, final URL url) throws IOException {
        load(classLoader, url.openStream());
    }
	
	public void destroy() {
        for(ScheduledExecutorService s:schedulers) {
            s.shutdown();
        }

		if(modules != null) {
			for(Module m:modules.values()) {
				try {
					m.destroy();
				} catch(Exception ex) {
					logger.error("Error to destroy module: " + m ,ex);
				}
			}
		}
	}

    public String getProperty(final String name) {
        return properties == null ? null : properties.get(name);
    }
	
	public Map<String, Module> getModules() {
		return modules;
	}
	
	public Module getModule(final String name) {
		return modules.get(name);
	}

	public <T extends Module> T getModule(final Class<T> clazz) {
        //String name = clazz.getName();
        for(Module m:modules.values()) {
            if(clazz.isAssignableFrom(m.getClass())) {
                return (T)m;
            }
        }
        return null;
    }

    public <T extends Module> T getModuleOrThrow(final Class<T> clazz) {
        //String name = clazz.getName();
        for(Module m:modules.values()) {
            if(clazz.isAssignableFrom(m.getClass())) {
                return (T)m;
            }
        }
        throw new IllegalArgumentException("Module " + clazz.getName() + " not find. But it's require");
    }

    private Map<String, String> getEnvMap(Map<String, String> properties) {
        final Map<String, String> env = new HashMap<String, String>();
        if(System.getenv() != null) {
            env.putAll(System.getenv());
        }

        // system environment path
        final Enumeration<Object> keys = System.getProperties().keys();
        while(keys.hasMoreElements()) {
            final String key = keys.nextElement().toString();
            final String value = System.getProperties().get(key).toString();
            if(value != null) {
                env.put(key, value);
            }
        }

        // application parameters
        final Enumeration<?> initNames = context.getInitParameterNames();
        while(initNames.hasMoreElements()) {
            final String name = initNames.nextElement().toString();
            final String value = context.getInitParameter(name);

            env.put(name, value);
        }

        // custom items
        env.put("xweb.dir", context.getRealPath("."));
        String base = System.getProperty("catalina.base");
        if(base == null) {
            base = System.getProperty("jetty.home");
        }
        if(base == null) {
            base = System.getProperty("user.dir");
        }
        env.put("xweb.base", base);

        env.put("xweb.root", context.getRealPath("/"));

        // add global properties
        env.putAll(properties);

        return env;
    }

    private String applyEnvironmentVariable(Map<String, String> env, final String s) {
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

    public static Map<String, String> getUrlParameters(final String stringQuery) throws IOException {
        final Map<String, String> params = new HashMap<String, String>();
        for (String param : stringQuery.split("&")) {
            final String pair[] = param.split("=");
            final String key = URLDecoder.decode(pair[0], "UTF-8");
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], "UTF-8");
            }
            params.put(new String(key), new String(value));
        }
        return params;
    }

    private void addSchedule(final Module module, final String queryString, long start, long period) throws IOException {
        final ModuleParam param = new ModuleParam(getUrlParameters(queryString));

        try {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        module.process(context, null, null, param, null);
                    } catch (Exception ex) {
                        logger.error("Error to execute schedule for: " + module.getInfo().getName() + " for: " + queryString, ex);
                    }
                }
            };

            final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            if(period > 0) {
                scheduler.scheduleAtFixedRate(runnable, start, period, TimeUnit.MILLISECONDS);
            } else {
                scheduler.schedule(runnable, start, TimeUnit.MILLISECONDS);
            }

            schedulers.add(scheduler);
        } catch (Exception ex) {
            logger.error("Error in maintenance task", ex);
        }
    }
	
	class Info implements ModuleInfo {

        final String name;

        final String author;

        final List<Validator> validators;

        final List<Role> roles;
		
		public Info(final String name, final String author, final List<Validator> validators, final List<Role> roles) {
			this.name = name;
			this.author = author;
			this.validators = validators;
            this.roles = roles;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getAuthor() {
			return this.author;
		}

        @Override
        public List<ModuleInfoValidator> getValidators() {
            return new ArrayList<ModuleInfoValidator>(this.validators);
        }

        @Override
        public List<ModuleInfoRole> getRoles() {
            return new ArrayList<ModuleInfoRole>(this.roles);
        }
    }

    private long getStart(final String text) {
        final long now = new Date().getTime();

        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        Calendar c1;

        c1 = dateFor("HH:mm", text);
        if(c1 != null) {
            c.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));

            if(c.getTime().before(new Date(now))) {
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            return c.getTimeInMillis() - now;
        }

        c1 = dateFor("EEE HH:mm", text);
        if(c1 != null) {
            c.set(Calendar.DAY_OF_WEEK, c1.get(Calendar.DAY_OF_WEEK));
            c.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));

            if(c.getTime().before(new Date(now))) {
                c.add(Calendar.WEEK_OF_MONTH, 1);
            }
            return c.getTimeInMillis() - now;
        }

        c1 = dateFor("dd HH:mm", text);
        if(c1 != null) {
            c.set(Calendar.DAY_OF_MONTH, c1.get(Calendar.DAY_OF_MONTH));
            c.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));

            if(c.getTime().before(new Date(now))) {
                c.add(Calendar.MONTH, 1);
            }
            return c.getTimeInMillis() - now;
        }

        c1 = dateFor("MM-dd HH:mm", text);
        if(c1 != null) {
            c.set(Calendar.MONTH, c1.get(Calendar.MONTH));
            c.set(Calendar.DAY_OF_MONTH, c1.get(Calendar.DAY_OF_MONTH));
            c.set(Calendar.HOUR_OF_DAY, c1.get(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c1.get(Calendar.MINUTE));

            if(c.getTime().before(new Date(now))) {
                c.add(Calendar.YEAR, 1);
            }
            return c.getTimeInMillis() - now;
        }

        return 0;
    }

    private long getPeriod(final String text) {
        try {
            return (long) (Float.parseFloat(text) * 60 * 1000D);
        } catch (Exception ex) {}

        if(text.endsWith("hour")) {
            final String t = text.substring(0, text.length() - 4).trim();
            int hour = t.length() == 0 ? 1 : Integer.parseInt(t);
            return hour * 60 * 60 * 1000L;
        }

        if(text.endsWith("week")) {
            final String t = text.substring(0, text.length() - 4).trim();
            int week = t.length() == 0 ? 1 : Integer.parseInt(t);
            return week * 7 * 24 * 60 * 60 * 1000L;
        }

        // TODO: We can not handle month in this way, month should calculate (29~31),
        // we need to remove the schedule and setup new one every month
        if(text.endsWith("month")) {
            final String t = text.substring(0, text.length() - 5).trim();
            int month = t.length() == 0 ? 1 : Integer.parseInt(t);
            return month * 30 * 24 * 60 * 60 * 1000L;
        }

        // TODO: We can not handle year in this way (365~366),
        // we need to remove the schedule and setup new one every year
        if(text.endsWith("year")) {
            final String t = text.substring(0, text.length() - 4).trim();
            int year = t.length() == 0 ? 1 : Integer.parseInt(t);
            return year * 365 * 24 * 60 * 60 * 1000L;
        }

        return 0;
    }

    private Calendar dateFor(final String pattern, final String text) {
        try {
            final SimpleDateFormat f = new SimpleDateFormat(pattern);
            final Date date = f.parse(text);
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            return c;
        } catch (Exception ex) {
            return null;
        }
    }

    public ServletContext getContext() {
        return this.context;
    }

    private class Validator implements ModuleInfoValidator {

        final String name;

        final String regex;

        final boolean require;

        Validator(final String name, final String regex, final boolean require) {
            this.name = name;
            this.regex = regex;
            this.require = require;
        }

        @Override
        public String getParam() {
            return this.name;
        }

        @Override
        public String getRegex() {
            return this.regex;
        }

        @Override
        public boolean isRequire() {
            return this.require;
        }
    }

    private class Role implements ModuleInfoRole {

        final List<String> params;

        final String role;

        final String eval;

        final Boolean definite;

        Role(final String param, final String eval, final String role, final String definite) {
            if(eval == null || role == null) {
                throw new IllegalArgumentException(param + ", " + eval + ", " + role);
            }


            this.params = (param == null || param.length() == 0) ? Collections.EMPTY_LIST : Arrays.asList(param.split("[,;]"));
            this.eval = eval;
            this.role = role;
            this.definite = "true".equalsIgnoreCase(definite);
        }

        @Override
        public List<String> getParams() {
            return this.params;
        }

        @Override
        public String getRole() {
            return this.role;
        }

        @Override
        public String getEval() {
            return this.eval;
        }

        @Override
        public boolean definite() {
            return this.definite;
        }
    }

}
