/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.*;
import org.tuckey.web.filters.urlrewrite.utils.ServerNameMatcher;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RewriteModule extends Module {

    private static Logger logger = LoggerFactory.getLogger("RewriteModule");

    /**
     * The conf for this filter.
     */
    private UrlRewriter urlRewriter = null;

    /**
     * A user defined setting that can enable conf reloading.
     */
    private boolean confReloadCheckEnabled = false;

    /**
     * A user defined setting that will allow configuration to be swapped via an HTTP to rewrite-status.
     */
    private boolean allowConfSwapViaHttp = false;

    private ServerNameMatcher statusServerNameMatcher;

    private static final String DEFAULT_STATUS_ENABLED_ON_HOSTS = "localhost, local, 127.0.0.1";

    private final List<MapEntry> maps;

    private final List<ParamEntry> params;

    private final List<MatchEntry> matches;

    private final List<RedirectEntry> redirects;

    private ServletContext context = null;

    public RewriteModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        // Map "from url" to "to url"
        final ModuleParam[] mapParams = properties.getParams("map", new ModuleParam[0]);
        maps = new ArrayList<MapEntry>(mapParams.length);
        for(ModuleParam p:mapParams) {
            final MapEntry e = new MapEntry();
            // TODO: Validate value with regex
            e.from = p.exists("from").getString();
            e.to = p.exists("to").getString();

            maps.add(e);
        }

        // Map 'from url" to "to url" (matches)
        final ModuleParam[] paramParams = properties.getParams("param", new ModuleParam[0]);
        params = new ArrayList<ParamEntry>(paramParams.length);
        for(ModuleParam p:paramParams) {
            final ParamEntry e = new ParamEntry();
            // TODO: Validate value with regex
            e.from = p.exists("from").getString();
            e.to = p.exists("to").getString();
            e.matcher = e.from.replaceAll("/\\$", "/[^/]*");

            params.add(e);
        }

        final ModuleParam[] matchParams = properties.getParams("match", new ModuleParam[0]);
        matches = new ArrayList<MatchEntry>(matchParams.length);
        for(ModuleParam p:matchParams) {
            final MatchEntry e = new MatchEntry();
            // TODO: Validate value with regex
            e.matcher = p.exists("from").getString();
            e.to = p.exists("to").getString();

            matches.add(e);
        }

        final ModuleParam[] redirectParams = properties.getParams("redirect", new ModuleParam[0]);
        redirects = new ArrayList<RedirectEntry>(redirectParams.length);
        for(ModuleParam p:redirectParams) {
            final RedirectEntry e = new RedirectEntry();
            // TODO: Validate value with regex
            e.from = p.exists("from").getString();
            e.to = p.exists("to").getString();

            redirects.add(e);
        }

    }

    @Override
    public void initFilter(FilterConfig filterConfig) throws ServletException {
        logger.debug("filter init called");
        if (filterConfig == null) {
            logger.error("unable to init filter as filter config is null");
            return;
        }

        logger.debug("init: calling destroy just in case we are being re-inited uncleanly");
        destroyActual();

        context = filterConfig.getServletContext();
        if (context == null) {
            logger.error("unable to init as servlet context is null");
            return;
        }

        final String allowConfSwapViaHttpStr = filterConfig.getInitParameter("allowConfSwapViaHttp");
        if (!StringUtils.isBlank(allowConfSwapViaHttpStr)) {
            allowConfSwapViaHttp = "true".equalsIgnoreCase(allowConfSwapViaHttpStr);
        }

        final String statusEnabledOnHosts = DEFAULT_STATUS_ENABLED_ON_HOSTS;
        statusServerNameMatcher = new ServerNameMatcher(statusEnabledOnHosts);

        loadUrlRewriter(filterConfig);
    }

    /**
     * Separate from init so that it can be overidden.
     */
    protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
        loadUrlRewriterLocal();
    }

    private byte[] generateTuckeyXmlConfig() throws IOException {
        final ModuleParam pro = getProperties();

        final Element urlrewrite = new Element("urlrewrite");

        for(MatchEntry e: matches) {
            //if(!pro.isDefaultProperties(key)) {
                final Element rule = new Element("rule");
                urlrewrite.addContent(rule);

                final Element from = new Element("from");
                from.setText(e.matcher);
                rule.addContent(from);

                final Element to = new Element("to");
                to.setText(e.to);
                rule.addContent(to);
            //}
        }

        final Document doc = new Document(urlrewrite);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getCompactFormat());
        xmlOutput.output(doc, baos);

        baos.flush();

        return baos.toByteArray();
    }

    private void loadUrlRewriterLocal() {
        try{
            final ByteArrayInputStream bais = new ByteArrayInputStream(generateTuckeyXmlConfig());

            final Conf conf = new Conf(context, bais, null, null, false);
            checkConf(conf);
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    /**
     * Separate from checkConfLocal so that it can be overidden.
     */
    protected void checkConf(Conf conf) {
        checkConfLocal(conf);
    }

    private void checkConfLocal(Conf conf) {
        if (conf.isOk() && conf.isEngineEnabled()) {
            urlRewriter = new UrlRewriter(conf);
            logger.info("loaded (conf ok)");

        } else {
            if (!conf.isOk()) {
                logger.error("Conf failed to load");
            }
            if (!conf.isEngineEnabled()) {
                logger.error("Engine explicitly disabled in conf"); // not really an error but we want ot to show in logs
            }
            if (urlRewriter != null) {
                logger.error("unloading existing conf");
                urlRewriter = null;
            }
        }
    }

    @Override
    public void destroyFilter() {
        logger.info("destroy called");
        destroyActual();
    }

    public void destroyActual() {
        destroyUrlRewriter();
        context = null;
        confReloadCheckEnabled = false;
    }

    protected void destroyUrlRewriter() {
        if (urlRewriter != null) {
            urlRewriter.destroy();
            urlRewriter = null;
        }
    }

    @Override
    public void doFilter(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain) throws IOException, ServletException {


        final String uri = request.getRequestURI();

        final String[] uriParts = uri.split("[#?]");
        final String path = uriParts[0];
        final String rest = uri.substring(uriParts[0].length());

        /**
         * Handle redirects
         */
        if(redirects.size() > 0) {
            for(RedirectEntry e:redirects) {
                if(e.from.equals(path)) {
                    response.sendRedirect(e.to + rest);
                    return;
                }
            }
        }

        /**
         * Handle maps
         */
        if(maps.size() > 0) {
            for(MapEntry e:maps) {
                if(e.from.equals(path)) {
                    request.getRequestDispatcher(e.to).forward(request, response);
                    return;
                }
            }
        }

        /**
         * Handle params
         */
        if(params.size() > 0) {
            for(ParamEntry e:params) {
                if(path.matches(e.matcher)) {
                    final String[] parts = path.split("");

                    String to = e.to;
                    for(int i=1; i<parts.length; i++) {
                        System.out.println(parts[i]);
                        to = to.replaceAll("$" + (i - 1), parts[i]);
                    }
                    System.out.println(to);

                    request.getRequestDispatcher(to).forward(request, response);
                    return;
                }
            }
        }

        /**
         * Handle matches
         */
        if(matches.size() > 0) {
            final UrlRewriter urlRewriter = getUrlRewriter(request, response, chain);
            final UrlRewriteWrappedResponse urlRewriteWrappedResponse = new UrlRewriteWrappedResponse(
                    response, request, urlRewriter);

            boolean requestRewritten = false;
            if (urlRewriter != null) {

                // process the request
                requestRewritten = urlRewriter.processRequest(request, urlRewriteWrappedResponse, chain);
            }

            // if no rewrite has taken place continue as normal
            if (!requestRewritten) {
                chain.doFilter(request, urlRewriteWrappedResponse);
            }
        }
    }

    /**
     * Called for every request.
     * <p/>
     * Split from doFilter so that it can be overriden.
     */
    protected UrlRewriter getUrlRewriter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // check to see if the conf needs reloading
        //if (isTimeToReloadConf()) {
        //    reloadConf();
        //}
        return urlRewriter;
    }

    private class MapEntry {

        String from;

        String to;

    }

    private class ParamEntry {

        String matcher;

        String from;

        String to;

    }

    private class MatchEntry {

        String matcher;

        String to;

    }

    private class RedirectEntry {

        String from;

        String to;

    }

}
