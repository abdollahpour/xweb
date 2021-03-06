/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import ir.xweb.data.DataTools;
import ir.xweb.data.Formatter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataModule extends Module {

    public final static String PARAM_PAGE_SIZE = "paging.size";

    public final static String PARAM_PAGE_FORMAT = "paging.format";

    public final static String PARAM_DATE_FORMAT = "date_format";

    public final static int DEFAULT_PAGE_SIZE = 100;

    public final static String DEFAULT_PAGE_FORMAT = "json";

    public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final int pageSize;

    private final String format;

    private ResourceModule resourceModule;

    private final DataTools dataTools;

    public DataModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        dataTools = new DataTools();
        pageSize = properties.getInt(PARAM_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        format = properties.getString(PARAM_PAGE_FORMAT, DEFAULT_PAGE_FORMAT);
        dataTools.setDateFormat(new SimpleDateFormat(properties.getString(PARAM_DATE_FORMAT, DEFAULT_DATE_FORMAT)));
    }

    protected Formatter addFormater(final String name, final Formatter formatter) {
        return dataTools.addFormatter(name, formatter);
    }

    public void write(
            final HttpServletResponse response,
            final String format,
            final Object object) throws IOException
    {
        final WriteConfig c = new WriteConfig();
        c.format = format;
        write(response, c, object);
    }

    public void write(
            final HttpServletResponse response,
            final String format,
            final String role,
            final String template,
            final Object object) throws IOException {
        write(response, format, role, template, null, null, object);
    }

    public void write(
            final HttpServletResponse response,
            final String format,
            final String role,
            final String template,
            final String language,
            final Object object) throws IOException {
        write(response, format, role, template, language, null, object);
    }

    public void write(
            final HttpServletResponse response,
            final String format,
            final String role,
            final String template,
            final String language,
            final Map<String, ?> templateParameters,
            final Object object) throws IOException {

        if("html".equals(format)) {
            if(template == null) {
                throw new IllegalArgumentException("Template null, you need template for HTML format");
            }
            if(resourceModule == null) {
                resourceModule = getManager().getModuleOrThrow(ResourceModule.class);
            }

            final String xml = dataTools.write("xml", role, object);
            final String html = resourceModule.applyXmlTemplate(template, language, templateParameters, xml);

            if(!response.containsHeader("Content-Type")) {
                response.addHeader("Content-Type", "text/html");
                response.setCharacterEncoding("UTF-8");
            }
            response.getWriter().write(html);
        } else {
            dataTools.write(response, format, role, object);
        }
    }

    /**
     * Write XWeb object with default configs.
     * @param response Http response
     * @param object XWeb object
     * @throws IOException Any error happens
     */
    public void write(final HttpServletResponse response, final Object object) throws IOException {
        write(response, (WriteConfig) null, object);
    }

    /**
     * Serialize XWeb object to response.
     *
     * @param response http response
     * @param config write config or null for default config
     * @param object xweb object
     * @throws IOException
     */
    public void write(
        final HttpServletResponse response,
        final WriteConfig config,
        final Object object) throws IOException
    {
        final WriteConfig c;
        if(config == null) {
            c = new WriteConfig();
        } else {
            c = config;
        }

        if (c.format == null) {
            throw new IllegalArgumentException("null format");
        }

        final Formatter formatter;
        final String contentType;

        if(c.format != null) {
            formatter = dataTools.getFormatter(c.format);
            contentType = formatter.getContentType();
        } else {
            formatter = null;
            contentType = null;
        }

        if(format.startsWith("html")) {
            if(c.template == null) {
                throw new IllegalArgumentException("Template null, you need template for HTML format");
            }
            if(resourceModule == null) {
                resourceModule = getManager().getModuleOrThrow(ResourceModule.class);
            }

            final String xml = dataTools.write("xml", c.role, object);
            final String html = resourceModule.applyXmlTemplate(
                c.template, c.templateLanguage, c.templateParams, xml);

            if(!response.isCommitted() && !response.containsHeader("Content-Type")) {
                response.addHeader("Content-Type", "text/html");
                response.setCharacterEncoding("UTF-8");
            }
            response.getWriter().write(html);
        } else {
            // add content type if it does not set yet
            if(!response.isCommitted() && !response.containsHeader("Content-Type")) {
                response.addHeader("Content-Type", contentType);
            }

            dataTools.write(response.getWriter(), c.format, c.role, object);
        }
    }

    /*public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final List<?> objects) throws IOException {
        writePage(response, params, objects, null);
    }*/

    public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final String role,
            final Collection<?> objects) throws IOException {

        writePage(
                response,
                params,
                params.getString("format", this.format),
                role,
                params.getString("template", null),
                params.getString("language", null),
                null,
                objects);
    }

    public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final String format,
            final String role,
            final String template,
            final String language,
            final Collection<?> objects) throws IOException {
        writePage(response, params, format, role, template, language, null, objects);
    }

    public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final String format,
            final String role,
            final String template,
            final String language,
            final Map<String, ?> templateParameters,
            final Collection<?> objects) throws IOException {

        if(objects == null) {
            throw new IllegalArgumentException("null objects");
        }

        final int size = params.getInt("size", this.pageSize);

        int count = objects.size() / size;
        if(count * size < objects.size()) {
            count++;
        }

        final int page = Math.min(params.getInt("page", 1), count);
        //final String format = params.getString("format", this.format);

        final int from = Math.min(Math.max(page - 1, 0) * size, objects.size());
        final int to = Math.min(size, objects.size() - from) + from;

        final List<Integer> pages = new ArrayList<Integer>();
        pages.add(1);
        if(count < 5) {
            for(int i=2; i<=count; i++) {
                pages.add(i);
            }
        }
        else {
            int center = Math.min(Math.max(3, page), count - 2);

            if(center > 3) {
                pages.add(0);
            }

            pages.add(center - 1);
            pages.add(center);
            pages.add(center + 1);

            if(center < count - 2) {
                pages.add(0);
            }

            pages.add(count);
        }

        final HashMap<Object, Object> results = new HashMap<Object, Object>();
        results.put("from", from + 1);
        results.put("to", to);
        results.put("size", objects.size());

        results.put("data", new ArrayList<Object>(objects).subList(from, to));
        results.put("params", params);
        results.put("more", to != objects.size());

        results.put("page", page);
        results.put("pages", pages);
        results.put("count", count);

        write(response, format, role, template, language, templateParameters, results);
    }

    public class WriteConfig {

        String templateLanguage = null;

        String template = null;

        String role = null;

        String format = "json";

        Map<String, String> templateParams = null;

    }

}
