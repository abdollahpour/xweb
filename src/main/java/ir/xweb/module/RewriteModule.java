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
import org.tuckey.web.filters.urlrewrite.utils.ModRewriteConfLoader;
import org.tuckey.web.filters.urlrewrite.utils.NumberUtils;
import org.tuckey.web.filters.urlrewrite.utils.ServerNameMatcher;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

public class RewriteModule extends Module {

    private static Logger logger = LoggerFactory.getLogger("RewriteModule");

    //public static final String VERSION = "3.2.0";

    //public static final String DEFAULT_WEB_CONF_PATH = "/WEB-INF/urlrewrite.xml";

    /**
     * The conf for this filter.
     */
    private UrlRewriter urlRewriter = null;

    /**
     * A user defined setting that can enable conf reloading.
     */
    private boolean confReloadCheckEnabled = false;

    /**
     * A user defined setting that says how often to check the conf has changed.
     */
    //private int confReloadCheckInterval = 0;

    /**
     * A user defined setting that will allow configuration to be swapped via an HTTP to rewrite-status.
     */
    private boolean allowConfSwapViaHttp = false;

    /**
     * The last time that the conf file was loaded.
     */
    //private long confLastLoad = 0;
    //private Conf confLastLoaded = null;
    //private long confReloadLastCheck = 30;
    //private boolean confLoadedFromFile = true;

    /**
     * path to conf file.
     */
    //private String confPath;

    /**
     * Flag to make sure we don't bog the filter down during heavy load.
     */
    //private boolean confReloadInProgress = false;

    private boolean statusEnabled = true;
    private String statusPath = "/rewrite-status";

    //private boolean modRewriteStyleConf = false;
    //public static final String DEFAULT_MOD_REWRITE_STYLE_CONF_PATH = "/WEB-INF/.htaccess";

    private ServerNameMatcher statusServerNameMatcher;
    private static final String DEFAULT_STATUS_ENABLED_ON_HOSTS = "localhost, local, 127.0.0.1";


    /**
     *
     */
    private ServletContext context = null;

    public RewriteModule(Manager manager, ModuleInfo info, ModuleParam properties) {
        super(manager, info, properties);
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


        // get init paramerers from context web.xml file
        //String confReloadCheckIntervalStr = filterConfig.getInitParameter("confReloadCheckInterval");
        //String confPathStr = filterConfig.getInitParameter("confPath");
        //String statusPathConf = filterConfig.getInitParameter("statusPath");
        //String statusEnabledConf = filterConfig.getInitParameter("statusEnabled");
        String statusEnabledOnHosts = filterConfig.getInitParameter("statusEnabledOnHosts");

        String allowConfSwapViaHttpStr = filterConfig.getInitParameter("allowConfSwapViaHttp");
        if (!StringUtils.isBlank(allowConfSwapViaHttpStr)) {
            allowConfSwapViaHttp = "true".equalsIgnoreCase(allowConfSwapViaHttpStr);
        }

        // confReloadCheckInterval (default to null)
        /*if (!StringUtils.isBlank(confReloadCheckIntervalStr)) {
            // convert to millis
            confReloadCheckInterval = 1000 * NumberUtils.stringToInt(confReloadCheckIntervalStr);

            if (confReloadCheckInterval < 0) {
                confReloadCheckEnabled = false;
                logger.info("conf reload check disabled");

            } else if (confReloadCheckInterval == 0) {
                confReloadCheckEnabled = true;
                logger.info("conf reload check performed each request");

            } else {
                confReloadCheckEnabled = true;
                logger.info("conf reload check set to " + confReloadCheckInterval / 1000 + "s");
            }

        } else {
            confReloadCheckEnabled = false;
        }*/

        //String modRewriteConf = filterConfig.getInitParameter("modRewriteConf");
        //if (!StringUtils.isBlank(modRewriteConf)) {
        //    modRewriteStyleConf = "true".equals(StringUtils.trim(modRewriteConf).toLowerCase());
        //}

        //if (!StringUtils.isBlank(confPathStr)) {
        //    confPath = StringUtils.trim(confPathStr);
        //} else {
        //    confPath = modRewriteStyleConf ? DEFAULT_MOD_REWRITE_STYLE_CONF_PATH : DEFAULT_WEB_CONF_PATH;
        //}
        //logger.debug("confPath set to " + confPath);

        // status enabled (default true)
        //if (statusEnabledConf != null && !"".equals(statusEnabledConf)) {
        //    logger.debug("statusEnabledConf set to " + statusEnabledConf);
        //    statusEnabled = "true".equals(statusEnabledConf.toLowerCase());
        //}
        //if (statusEnabled) {
        //    // status path (default /rewrite-status)
        //    if (statusPathConf != null && !"".equals(statusPathConf)) {
        //        statusPath = statusPathConf.trim();
        //        logger.info("status display enabled, path set to " + statusPath);
        //    }
        //} else {
        //    logger.info("status display disabled");
        //}

        //if (StringUtils.isBlank(statusEnabledOnHosts)) {
            statusEnabledOnHosts = DEFAULT_STATUS_ENABLED_ON_HOSTS;
        //} else {
        //    logger.debug("statusEnabledOnHosts set to " + statusEnabledOnHosts);
        //}
        statusServerNameMatcher = new ServerNameMatcher(statusEnabledOnHosts);

        // now load conf from snippet in web.xml if modRewriteStyleConf is set
        //String modRewriteConfText = filterConfig.getInitParameter("modRewriteConfText");
        //if (!StringUtils.isBlank(modRewriteConfText)) {
        //    ModRewriteConfLoader loader = new ModRewriteConfLoader();
        //    Conf conf = new Conf();
        //    loader.process(modRewriteConfText, conf);
        //    conf.initialise();
        //    checkConf(conf);
        //    confLoadedFromFile = false;
        //
        //}   else {
        //
            loadUrlRewriter(filterConfig);
        //}
    }

    /**
     * Separate from init so that it can be overidden.
     */
    protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
        loadUrlRewriterLocal();
    }

    private byte[] generateXml() throws IOException {
        ModuleParam pro = getProperties();

        Element urlrewrite = new Element("urlrewrite");

        for(String key:pro.keySet()) {
            String value = pro.getString(key, null);

            Element rule = new Element("rule");
            urlrewrite.addContent(rule);

            Element from = new Element("from");
            from.setText(key);
            rule.addContent(from);

            Element to = new Element("to");
            to.setText(value);
            rule.addContent(to);
        }

        Document doc = new Document(urlrewrite);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getCompactFormat());
        xmlOutput.output(doc, baos);

        baos.flush();

        return baos.toByteArray();
    }

    private void loadUrlRewriterLocal() {
        //InputStream inputStream = context.getResourceAsStream(confPath);
        //URL confUrl = null;
        //try {
        //    confUrl = context.getResource(confPath);
        //} catch (MalformedURLException e) {
        //    logger.debug("", e);
        //}
        //String confUrlStr = null;
        //if (confUrl != null) {
        //    confUrlStr = confUrl.toString();
        //}
        //if (inputStream == null) {
        //    logger.error("unable to find urlrewrite conf file at " + confPath);
        //    // set the writer back to null
        //    if (urlRewriter != null) {
        //        logger.error("unloading existing conf");
        //        urlRewriter = null;
        //    }
        //
        //} else {
            //Conf conf = new Conf(context, inputStream, confPath, confUrlStr, modRewriteStyleConf);

            try{
                ByteArrayInputStream bais = new ByteArrayInputStream(generateXml());

                Conf conf = new Conf(context, bais, null, null, false);
                checkConf(conf);
            } catch (Exception ex) {
                logger.error("", ex);
            }
        //}*/
    }

    /**
     * Separate from checkConfLocal so that it can be overidden.
     */
    protected void checkConf(Conf conf) {
        checkConfLocal(conf);
    }

    private void checkConfLocal(Conf conf) {
        //if (logger.isDebugEnabled()) {
        //    if (conf.getRules() != null) {
        //        logger.debug("inited with " + conf.getRules().size() + " rules");
        //    }
        //    logger.debug("conf is " + (conf.isOk() ? "ok" : "NOT ok"));
        //}
        //confLastLoaded = conf;
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
        //confLastLoad = 0;
        //confPath = DEFAULT_WEB_CONF_PATH;
        confReloadCheckEnabled = false;
        //confReloadCheckInterval = 0;
        //confReloadInProgress = false;
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

        UrlRewriter urlRewriter = getUrlRewriter(request, response, chain);

        final HttpServletRequest hsRequest = (HttpServletRequest) request;
        final HttpServletResponse hsResponse = (HttpServletResponse) response;
        UrlRewriteWrappedResponse urlRewriteWrappedResponse = new UrlRewriteWrappedResponse(hsResponse, hsRequest,
                urlRewriter);

        boolean requestRewritten = false;
        if (urlRewriter != null) {

            // process the request
            requestRewritten = urlRewriter.processRequest(hsRequest, urlRewriteWrappedResponse, chain);

        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("urlRewriter engine not loaded ignoring request (could be a conf file problem)");
            }
        }

        // if no rewrite has taken place continue as normal
        if (!requestRewritten) {
            chain.doFilter(hsRequest, urlRewriteWrappedResponse);
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

    /**
     * Is it time to reload the configuration now.  Depends on is conf reloading is enabled.
     */
    //public boolean isTimeToReloadConf() {
    //    if (!confLoadedFromFile) return false;
    //    long now = System.currentTimeMillis();
    //    return confReloadCheckEnabled && !confReloadInProgress && (now - confReloadCheckInterval) > confReloadLastCheck;
    //}

    /**
     * Forcibly reload the configuration now.
     */
    /*public void reloadConf() {
        long now = System.currentTimeMillis();
        confReloadInProgress = true;
        confReloadLastCheck = now;

        logger.debug("starting conf reload check");
        long confFileCurrentTime = getConfFileLastModified();
        if (confLastLoad < confFileCurrentTime) {
            // reload conf
            confLastLoad = System.currentTimeMillis();
            logger.info("conf file modified since last load, reloading");
            loadUrlRewriterLocal();
        } else {
            logger.debug("conf is not modified");
        }
        confReloadInProgress = false;
    }*/

    /**
     * Gets the last modified date of the conf file.
     *
     * @return time as a long
     */
    //private long getConfFileLastModified() {
    //    File confFile = new File(context.getRealPath(confPath));
    //    return confFile.lastModified();
    //}


    //public boolean isConfReloadCheckEnabled() {
    //    return confReloadCheckEnabled;
    //}

    /**
     * The amount of seconds between reload checks.
     *
     * @return int number of millis
     */
    //public int getConfReloadCheckInterval() {
    //    return confReloadCheckInterval / 1000;
    //}

    //public Date getConfReloadLastCheck() {
    //    return new Date(confReloadLastCheck);
    //}

    //public boolean isStatusEnabled() {
    //    return statusEnabled;
    //}

    //public String getStatusPath() {
    //    return statusPath;
    //}

    //public boolean isLoaded() {
    //    return urlRewriter != null;
    //}

    /*public static String getFullVersionString() {
        Properties props = new Properties();
        String buildNumberStr = "";
        try {
            InputStream is = UrlRewriteFilter.class.getResourceAsStream("build.number.properties");
            if ( is != null ) {
                props.load(is);
                String buildNumber = (String) props.get("build.number");
                if (!StringUtils.isBlank(buildNumber)){
                    buildNumberStr =  " build " + props.get("build.number");
                }
            }
        } catch (IOException e) {
            logger.error("", e);
        }
        return VERSION + buildNumberStr;
    }*/

}
