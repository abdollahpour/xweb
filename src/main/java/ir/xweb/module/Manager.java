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

import ir.xweb.server.Constants;
import org.apache.commons.fileupload.FileItem;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Manager {
	
	private final static Logger logger = LoggerFactory.getLogger("Manager");

    private final static String DEFAULT_PROPERTIES_PREFIX = "default.";

	private final LinkedHashMap<String, Module> modules = new LinkedHashMap<String, Module>();

    private ModuleParam properties;

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

            final Map<String, String> envParam = getEnvMap();

            final Element envPropertiesElement = root.getChild("properties");
            if(envPropertiesElement != null) {
                this.properties = (ModuleParam) getElement(null, envParam, envPropertiesElement);
            }

            if(this.properties != null) {
                for (String key:this.properties.keySet()) {
                    final String value = this.properties.getString(key);
                    if(value != null) {
                        envParam.put(key, value);
                    }
                }
            }

            final ModuleParam defaultParam = getDefaultProperties(this.properties);

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
                                    role.getAttributeValue("match"),
                                    role.getAttributeValue("accept"),
                                    role.getAttributeValue("reject"),
                                    role.getAttributeValue("or"),
                                    role.getAttributeValue("and")
                            );

                            boolean add = true;

                            if(r.and != null) {
                                for(Role a:roles) {
                                    if(a.and != null && a.and.equals(r.and)) {
                                        if(a.ands == null) {
                                            a.ands = new ArrayList<ModuleInfoRole>();
                                        }
                                        a.ands.add(r);
                                        add = false;
                                        break;
                                    }
                                }
                            }

                            if(r.or != null) {
                                for(Role a:roles) {
                                    if(a.or != null && a.or.equals(r.or)) {
                                        if(a.ors == null) {
                                            a.ors = new ArrayList<ModuleInfoRole>();
                                        }
                                        a.ors.add(r);
                                        add = false;
                                        break;
                                    }
                                }
                            }

                            if(add) {
                                roles.add(r);
                            }
                        }
                    }

                    final Element propertiesElement = model.getChild("properties");
                    final ModuleParam properties;
                    if(propertiesElement != null) {
                        properties = (ModuleParam) getElement(defaultParam, envParam, propertiesElement);
                    } else {
                        properties = new ModuleParam();
                    }

                    try {
                        final Info info = new Info(name, author, validators, new ArrayList<ModuleInfoRole>(roles));

                        final Class<?> c = Class.forName(className, false, classLoader);
                        final Constructor<?> cons = c.getConstructor(Manager.class, ModuleInfo.class, ModuleParam.class);
                        final Module module = (Module) cons.newInstance(this, info, properties);

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

                                final long s;
                                if(start == null) {
                                    s = 2000;
                                }
                                else {
                                    s = 2000 + getTime(start);
                                }


                                final long p;
                                if(period == null) {
                                    p = 0;
                                } else {
                                    p = getTime(period);
                                }

                                // we don't add schedule in module test
                                if(!"true".equals(System.getProperty("ir.xweb.test"))) {
                                    addSchedule(module, query, s, p);
                                }
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
            s.shutdownNow();
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
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }

        if(this.properties != null) {
            return this.properties.getString(name);
        }
        return null;
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

    private ModuleParam getDefaultProperties(final ModuleParam def) {
        if(def != null) {
            final ModuleParam param = new ModuleParam();

            for (Map.Entry<String, Object> e:def.entrySet()) {
                if(e.getKey().startsWith(DEFAULT_PROPERTIES_PREFIX)) {
                    final String k = e.getKey().substring(DEFAULT_PROPERTIES_PREFIX.length());

                    final Object v;
                    if(e.getValue() instanceof ModuleParam) {
                        v = getDefaultProperties((ModuleParam) e.getValue());
                    } else {
                        v = e.getValue();
                    }

                    param.put(k, v);
                }
            }

            if(param.size() > 0) {
                return param;
            }
        }

        return null;
    }

    /**
     * Get list of parametters from system
     * @return
     */
    private Map<String, String> getEnvMap() {
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

    public static Map<String, Object> getUrlParameters(final String stringQuery) throws IOException {
        final Map<String, Object> params = new HashMap<String, Object>();
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
                        final String requestUri = Constants.MODULE_URI_PERFIX;// + "?" + Constants.MODULE_NAME_PARAMETER + "=" + queryString;
                        final ScheduleRequest request = new ScheduleRequest(context, param, "POST", requestUri);
                        final ScheduleResponse response = new ScheduleResponse();

                        final ScheduleChain chain = new ScheduleChain();
                        chain.context = context;
                        chain.iterator = modules.values().iterator();
                        chain.module = module;
                        chain.param = param;

                        chain.doFilter(request, response);
                    }
                    catch (Exception ex) {
                        logger.error("Error to execute schedule for: " + module.getInfo().getName() + " for: " + queryString, ex);
                    }
                }
            };

            System.out.println("start: " + start + " " + period);
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

    private Object getElement(
        final ModuleParam def,
        final Map<String, String> envParams,
        final Element element)
    {
        if(element == null) {
            throw new IllegalArgumentException("null");
        }
        if(element.getChildren().size() == 0) {
            final String value = element.getAttributeValue("value");
            if(value != null) {
                return value;
            } else {
                final String text = element.getText();

                return applyEnvironmentVariable(envParams, text);
            }
        }

        final ModuleParam param = new ModuleParam(def);

        for (Object o:element.getChildren()) {
            final Element e = (Element) o;
            final String key = e.getAttributeValue("key");
            if (key == null) {
                throw new IllegalArgumentException("key attribute not found");
            }

            // for array type, none of children should have key attribute
            boolean isArray = false;
            if (e.getChildren().size() > 0) {
                isArray = true;

                for (Object _o:e.getChildren()) {
                    final Element _e = (Element) _o;
                    if (_e.getAttributeValue("key") != null) {
                        isArray = false;
                        break;
                    }
                }
            }

            if (isArray) {
                final List<Object> list = new ArrayList<Object>(e.getChildren().size());
                for (Object _o:e.getChildren()) {
                    final Element _e = (Element) _o;

                    list.add(getElement(null, envParams, _e));
                }

                param.put(key, list);
            }
            else {
                param.put(key, getElement(null, envParams, e));
            }
        }

        return param;
    }
	
	private class Info implements ModuleInfo {

        final String name;

        final String author;

        final List<Validator> validators;

        final List<ModuleInfoRole> roles;
		
		public Info(
                final String name,
                final String author,
                final List<Validator> validators,
                final List<ModuleInfoRole> roles) {

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

    private class Role implements ModuleInfoRole {

        String param;

        String match;

        String accept;

        String reject;

        String or;

        String and;

        List<ModuleInfoRole> ors;

        List<ModuleInfoRole> ands;

        public Role(
                final String param,
                final String match,
                final String accept,
                final String reject,
                final String or,
                final String and) {

            this.param = param;
            this.match = match;
            this.accept = accept;
            this.reject = reject;
            this.or = or;
            this.and = and;
        }

        @Override
        public String param() {
            return this.param;
        }

        @Override
        public String match() {
            return this.match;
        }

        @Override
        public String accept() {
            return this.accept;
        }

        @Override
        public String reject() {
            return this.reject;
        }

        @Override
        public List<ModuleInfoRole> or() {
            return ors;
        }

        @Override
        public List<ModuleInfoRole> and() {
            return ands;
        }
    }

    private long getTime(final String s) {
        final long now = System.currentTimeMillis();
        if(s.matches("[0-9]+")) {
            return Integer.parseInt(s.replaceAll("[^0-9]", "")) * 60000;
        }
        else if(s.matches("[0-9]+(s|sec|second|seconds)")) {
            return Integer.parseInt(s.replaceAll("[^0-9]", "")) * 1000;
        }
        else if(s.matches("[0-9]+(m|min|minuet|minuets)")) {
            return Integer.parseInt(s.replaceAll("[^0-9]", "")) * 60000;
        }
        else if(s.matches("[0-9]+(h|hour|hours)")) {
            return Integer.parseInt(s.replaceAll("[^0-9]", "")) * 1440000;
        }
        else if(s.matches("[0-9]+(d|day|days)")) {
            final Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            c.add(Calendar.DAY_OF_YEAR, Integer.parseInt(s.replaceAll("[^0-9]", "")));
            return c.getTimeInMillis() - now;
        }
        else if(s.matches("[0-9]+(month|months)")) {
            final Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            c.add(Calendar.MONTH, Integer.parseInt(s.replaceAll("[^0-9]", "")));
            return c.getTimeInMillis() - now;
        }
        else if(s.matches("[0-9]+(year|years)")) {
            final Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            c.add(Calendar.YEAR, Integer.parseInt(s.replaceAll("[^0-9]", "")));
            return c.getTimeInMillis() - now;
        }
        else if(s.matches("[0-9]+:[0-9]+")) {
            final String[] parts = s.split(":");
            final Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            c.add(Calendar.HOUR, Integer.parseInt(parts[0]));
            c.add(Calendar.MINUTE, Integer.parseInt(parts[1]));
            return c.getTimeInMillis() - now;
        }
        else if(s.matches("[0-9]+:[0-9]+:[0-9]+")) {
            final String[] parts = s.split(":");
            final Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            c.add(Calendar.HOUR, Integer.parseInt(parts[0]));
            c.add(Calendar.MINUTE, Integer.parseInt(parts[1]));
            c.add(Calendar.SECOND, Integer.parseInt(parts[2]));
            return c.getTimeInMillis() - now;
        }
        else if(s.matches("[0-9]+/[0-9]+/[0-9]+\\s[0-9]+:[0-9]+")) {
            try {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm");
                return format.parse(s).getTime() - now;
            } catch (Exception ex) {}
        }
        else if(s.matches("[0-9]+/[0-9]+/[0-9]+\\s[0-9]+:[0-9]+:[0-9]+")) {
            try {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
                return format.parse(s).getTime() - now;
            } catch (Exception ex) {}
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

    private class ScheduleChain implements FilterChain {

        Iterator<Module> iterator;

        ServletContext context;

        ModuleParam param;

        Module module;

        @Override
        public void doFilter(
                final ServletRequest request,
                final ServletResponse response) throws IOException, ServletException
        {
            if(iterator.hasNext()) {
                final Module next = iterator.next();
                next.doFilter(
                        context,
                        (HttpServletRequest) request,
                        (HttpServletResponse) response,
                        this);
            }
            else {
                module.process(
                        context,
                        (HttpServletRequest) request,
                        (HttpServletResponse) response,
                        param,
                        Collections.<String, FileItem>emptyMap());
            }
        }
    }

}
