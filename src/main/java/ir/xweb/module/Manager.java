/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.module;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.xweb.data.InstrumentHook;
import ir.xweb.data.XWebData;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;

public class Manager {
	
	private final static Logger logger = LoggerFactory.getLogger("Manager");

    private final static String VALIDATOR_NAME = "[a-zA-Z0-9_]{1,30}";

	private Map<String, DataSource> dataSources = new HashMap<String, DataSource>();
	
	private Map<String, Module> modules = new HashMap<String, Module>();

    private Map<String, String> properties = null;

    private ServletContext context;

    private List<ScheduledExecutorService> schedulers = new ArrayList<ScheduledExecutorService>();

    public Manager(ServletContext context) {
        this.context = context;
    }

    public void load(InputStream in) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        load(classLoader, in);
    }

    public void load(ClassLoader classLoader, InputStream in) throws IOException {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(in);
            Element root = document.getRootElement();

            List<?> datasources = root.getChild("datasources").getChildren("datasource");
            for(Object o:datasources) {
                Element element = (Element)o;

                String name = element.getChildText("name");
                String className = element.getChildText("class");

                try {
                    Class<?> c = Class.forName(className, false, classLoader);
                    DataSource datasource = (DataSource) c.newInstance();
                    dataSources.put(name, datasource);
                } catch (Exception ex) {
                    throw new IOException("Error in load datasource", ex);
                }
            }

            List<?> moduleElements = root.getChild("modules").getChildren("module");
            for(Object o:moduleElements) {
                Element model = (Element)o;

                String name = model.getChildText("name");
                String author = model.getChildText("author");
                String className = model.getChildText("class");

                List<Validator> validators = new ArrayList<Validator>();
                Element validatorsElement = model.getChild("validators");
                if(validatorsElement != null) {
                    List<?> validatorElements = validatorsElement.getChildren("validator");
                    for(Object o2:validatorElements) {
                        Element v = (Element)o2;

                        validators.add(new Validator(
                                v.getAttributeValue("param"),
                                v.getAttributeValue("regex"),
                                "true".equalsIgnoreCase(v.getAttributeValue("require"))
                        ));
                    }
                }

                List<Role> roles = new ArrayList<Role>();
                Element rolesElement = model.getChild("roles");
                if(rolesElement != null) {
                    List<?> roleElements = rolesElement.getChildren("role");
                    for(Object o2:roleElements) {
                        Element role = (Element)o2;

                        Role r = new Role(
                                role.getAttributeValue("param"),
                                role.getAttributeValue("eval"),
                                role.getAttributeValue("value"),
                                role.getAttributeValue("definite")
                        );

                        roles.add(r);
                    }
                }

                Map<String, String> properties = new HashMap<String, String>();
                Element propertiesElement = model.getChild("properties");
                if(propertiesElement != null) {
                    List<?> propertyElements = propertiesElement.getChildren("property");
                    for(Object o2:propertyElements) {
                        Element property = (Element)o2;
                        String key = property.getAttribute("key").getValue();
                        String text = applyEnvironmentVariable(property.getText());
                        properties.put(key, text);
                    }
                }

                try {
                    Info info = new Info(name, author, validators, roles);

                    Class<?> c = Class.forName(className, false, classLoader);
                    Constructor<?> cons = c.getConstructor(Manager.class, ModuleInfo.class, ModuleParam.class);
                    Module module = (Module) cons.newInstance(this, info, new ModuleParam(properties));


                    // Add schedules now
                    Element schedulesElement = model.getChild("schedules");
                    if(schedulesElement != null) {
                        List<?> roleElements = schedulesElement.getChildren("schedule");
                        for(Object o2:roleElements) {
                            Element schedule = (Element)o2;

                            String unit = schedule.getAttributeValue("unit");
                            int start = Integer.parseInt(schedule.getAttributeValue("start"));
                            int period = Integer.parseInt(schedule.getAttributeValue("period"));
                            String query = schedule.getAttributeValue("query");

                            if("month".equalsIgnoreCase(unit)) {
                                start = start * 30 * 24 * 60 * 60 * 1000;
                                period = period * 30 * 24 * 60 * 60 * 1000;
                            } else if("day".equalsIgnoreCase(unit)) {
                                start = start * 24 * 60 * 60 * 1000;
                                period = period * 24 * 60 * 60 * 1000;
                            } else if("hour".equalsIgnoreCase(unit)) {
                                start = start * 60 * 60 * 1000;
                                period = period * 60 * 60 * 1000;
                            } else if("minuet".equalsIgnoreCase(unit)) {
                                start = start * 60 * 1000;
                                period = period * 60 * 1000;
                            } else if(unit == null) { // hour by default
                                start = start * 24 * 60 * 60 * 1000;
                                period = period * 24 * 60 * 60 * 1000;
                            }

                            // system load
                            start += 2000;

                            addSchedule(module, query, start, period);
                        }
                    }


                    modules.put(name, module);
                } catch (Exception ex) {
                    throw new IOException("Error in load module", ex);
                }
            }

            properties = new HashMap<String, String>();
            Element propertiesElement = root.getChild("properties");
            if(propertiesElement != null) {
                List<?> propertyElements = propertiesElement.getChildren("property");
                for(Object o2:propertyElements) {
                    Element property = (Element)o2;
                    String key = property.getAttribute("key").getValue();
                    String text = applyEnvironmentVariable(property.getText());
                    properties.put(key, text);
                }
            }
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
    }

    public void load(URL url) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        load(url.openStream());
    }

    public void load(ClassLoader classLoader, URL url) throws IOException {
        load(classLoader, url.openStream());
    }

    /**
     * @deprecated
     * @param context
     * @param in
     * @throws IOException
     */
	public void load(ServletContext context, InputStream in) throws IOException {
        this.context = context;

		load(in);
	}
	
	public void unload() {
        for(ScheduledExecutorService s:schedulers) {
            s.shutdown();
        }

		if(modules == null) {
			for(Module m:modules.values()) {
				try {
					m.unload();
				} catch(Exception ex) {
					logger.error("Error to unload module: " + m ,ex);
				}
			}
		}
	}

    public String getProperty(String name) {
        return properties == null ? null : properties.get(name);
    }
	
	public Map<String, Module> getModules() {
		return modules;
	}
	
	public Module getModule(String name) {
		return modules.get(name);
	}

    @SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> clazz) {
        //String name = clazz.getName();
        for(Module m:modules.values()) {
            if(m.getClass() == clazz) {
                return (T)m;
            }
        }
        return null;
    }

    public DataSource getDataSource(String name) {
        return dataSources.get(name);
    }
	
	public Map<String, DataSource> getDataSources() {
		return dataSources;
	}

    private String applyEnvironmentVariable(String s) {
        Map<String, String> env = new HashMap<String, String>();
        if(System.getenv() == null) {
            env.putAll(System.getenv());
        }

        // system environment path
        Enumeration<Object> keys = System.getProperties().keys();
        while(keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String value = System.getProperties().get(key).toString();
            if(value != null) {
                env.put(key, value);
            }
        }

        // application parameters
        Enumeration<?> initNames = context.getInitParameterNames();
        while(initNames.hasMoreElements()) {
            String name = initNames.nextElement().toString();
            String value = context.getInitParameter(name);

            env.put(name, value);
        }

        // custom items
        env.put("xweb.dir", context.getRealPath("."));


        Pattern pattern = Pattern.compile("\\$\\{([^}]*)\\}");

        Matcher m = pattern.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String text = m.group(1);
            String value = env.get(text);
            m.appendReplacement(sb, value == null ? "" : value);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, String> getUrlParameters(String stringQuery) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        for (String param : stringQuery.split("&")) {
            String pair[] = param.split("=");
            String key = URLDecoder.decode(pair[0], "UTF-8");
            String value = "";
            if (pair.length > 1) {
                value = URLDecoder.decode(pair[1], "UTF-8");
            }
            params.put(new String(key), new String(value));
        }
        return params;
    }

    private void addSchedule(final Module module, final String queryString, int start, int period) throws IOException {
        //System.out.println(" >>>>>>>>>> " + module.getInfo().getName() + " " + queryString + " " + start + " " + period);

        final ModuleParam param = new ModuleParam(getUrlParameters(queryString));

        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    //System.out.println("run " + module.getInfo().getName());
                    try {
                        module.process(context, null, null, param, null);
                    } catch (Exception ex) {
                        logger.error("Error to execute schedule for: " + module.getInfo().getName() + " for: " + queryString, ex);
                    }
                }
            };

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(runnable, start, period, TimeUnit.MILLISECONDS);

            schedulers.add(scheduler);
        } catch (Exception ex) {
            logger.error("Error in maintenance task", ex);
        }
    }
	
	class Info implements ModuleInfo {
		
		String name;
		
		String author;

        List<Validator> validators;

        List<Role> roles;
		
		public Info(String name, String author, List<Validator> validators, List<Role> roles) {
			this.name = name;
			this.author = author;
			this.validators = validators;
            this.roles = roles;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getAuthor() {
			return author;
		}

        @Override
        public List<ModuleInfoValidator> getValidators() {
            return new ArrayList<ModuleInfoValidator>(validators);
        }

        @Override
        public List<ModuleInfoRole> getRoles() {
            return new ArrayList<ModuleInfoRole>(roles);
        }
    }

    public ServletContext getContext() {
        return context;
    }

    private class Validator implements ModuleInfoValidator {

        String name;

        String regex;

        boolean require;

        Validator(String name, String regex, boolean require) {
            this.name = name;
            this.regex = regex;
            this.require = require;
        }

        @Override
        public String getParam() {
            return name;
        }

        @Override
        public String getRegex() {
            return regex;
        }

        @Override
        public boolean isRequire() {
            return require;
        }
    }

    private class Role implements ModuleInfoRole {

        List<String> params;

        String role;

        String eval;

        Boolean definite;

        Role(String param, String eval, String role, String definite) {
            if(param == null) {
                param = "";
            }
            if(eval == null || role == null) {
                throw new IllegalArgumentException(param + ", " + eval + ", " + role);
            }


            this.params = param.length() == 0 ? Collections.EMPTY_LIST : Arrays.asList(param.split("[,;]"));
            this.eval = eval;
            this.role = role;
            this.definite = "true".equalsIgnoreCase(definite);
        }

        @Override
        public List<String> getParams() {
            return params;
        }

        @Override
        public String getRole() {
            return role;
        }

        @Override
        public String getEval() {
            return eval;
        }

        @Override
        public boolean definite() {
            return definite;
        }
    }

}
