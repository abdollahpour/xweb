/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.server.XWebUser;
import ir.xweb.util.CookieTools;
import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationModule extends Module {

    private Logger logger = LoggerFactory.getLogger("AuthenticationModule");

    public final static String SESSION_USER = "xweb_user";

    public final static String PARAM_COOKIE_AGE = "cookie-age";

    public final static String PARAM_DEFAULT = "default";

    public final static String PARAM_REDIRECT = "redirect";

    public final static String PARAM_CHECK = "check";

    public final static String PARAM_IGNORE = "ignore";

    public final static String PARAM_NO_LOGIN = "nologin";

    public final static String PARAM_DATA = "data";

    private final static int DEFAULT_COOKIE_AGE = 60 * 60 * 24 * 30; // 1 month

    private final int cookieAge;

    private final String redirect;

    private final String check;

    private final String ignore;

    private final String nologin;

    private AuthenticationModuleData dataSource;

    private final String dataName;

    public AuthenticationModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        cookieAge = properties.getInt(PARAM_COOKIE_AGE, DEFAULT_COOKIE_AGE);

        this.redirect = properties.getString(PARAM_REDIRECT, null);
        this.check = properties.getString(PARAM_CHECK, null);
        this.ignore = properties.getString(PARAM_IGNORE, null);
        this.nologin = properties.getString(PARAM_NO_LOGIN, null);
        this.dataName = properties.getString(PARAM_DATA);
    }

    @Override
    public void doFilter(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        XWebUser user = getUser(request);

        // pre-check to speedup checking
        if(user != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // check for permission patterns
        URI uri;
        try {
            uri = new URI(request.getRequestURI());
        } catch (Exception ex) {
            // It will never happen because it passed by http request
            throw new IOException(ex);
        }

        /*
        * Login with Cookie
        * No matter it's defined with check pattern or not, we try to login with cookie first!
         */
        final String uuid = CookieTools.getCookieValue(request, Constants.COOKIE_AUTH_REMEMBER);
        if(uuid != null) {
            logger.trace("Try to login with cookie. UUID: " + uuid);
            user = getDataSource().getUserWithUUID(uuid);
            if(user != null) {
                logger.trace("User successfully login with UUID: " + uuid);
                setUser(request, user);

                filterChain.doFilter(request, response);
                return;
            } else {
                logger.trace("UUID not valid for login (maybe expired: " + uuid);
            }
        }

        // we don't care about context path, so we trunk it
        final String path = uri.getPath().substring(request.getContextPath().length());

        // We handle API requests with different authentication method (Role base)
        boolean isApiCall = path.equals(Constants.MODULE_URI_PERFIX);

        if(redirect != null && uri.equals(redirect)){
            filterChain.doFilter(request, response);
            return;
        }

        // We also check all the API calls, but permission management check by module itself
        if(check != null && !isApiCall && !path.matches(check)) {
            filterChain.doFilter(request, response);
            return;
        }

        if(ignore != null && path.matches(ignore)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader("Authorization");
        if (header != null) {
            logger.debug("Try to authenticate by HTTP authentication");

            if(header.startsWith("Basic")) {
                final String token = header.substring(6);

                user = getDataSource().getUserWithUUID(token);
                if(user != null) {
                    logger.debug("User successfully login with HTTP authentication: " + token);
                    setUser(request, user);

                    filterChain.doFilter(request, response);
                    return;
                } else {
                    logger.debug("HTTP token not valid for login. maybe expired: " + token);
                }

            } else {
                throw new IllegalStateException("Unsupported authentication method");
            }
        }


        // for API call there's 3 choice for login
        // 1: Session (already login)
        // 2: Cookie (UUID)
        // 3: HTTP authentication
        if(isApiCall) {
            // After Iteration 4 we will process for permission inside of Module
            filterChain.doFilter( request, response );
            return;
        } else if(redirect != null) {
            if(!redirect.equals(path)) {
                // redirect to redirect page.
                // on redirect page we male relative path (not absolute)

                String redirectPath = context.getContextPath() + redirect;
                String urlPath = context.getContextPath() + uri;

                int lastIndex = redirectPath.lastIndexOf('/');
                if(lastIndex > -1) {
                    urlPath = urlPath.substring(lastIndex + 1);
                }

                response.sendRedirect(redirectPath + "?url=" + URLEncoder.encode(urlPath, "UTF-8"));
            } else {
                filterChain.doFilter( request, response );
                return;
            }
        } else {
            // Set standard HTTP/1.1 no-cache headers.
            response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
            // Set standard HTTP/1.0 no-cache header.
            response.setHeader("Pragma", "no-cache");

            response.setHeader( "WWW-Authenticate", "Basic realm=\"" + "jaRk79" + "\"" );
            response.sendError( HttpServletResponse.SC_UNAUTHORIZED );
        }
    }

    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam params,
            final Map<String, FileItem> files) throws IOException {

        /**
         * Actions:
         * login: Fully login into system. User need to enter captcha code also
         * temp_pass: generate temporary password for simple authenticate transactions
         */

        if(params.hasValueFor("login")) {
            final String identifier = params.getString("login");
            // Password hashed with MD5
            final String password = params.exists("password").getString();
            final String captcha = params.validate("captcha", CaptchaModule.SESSION_CAPTCHA_PATTERN, true).getString();

            getManager().getModuleOrThrow(CaptchaModule.class).validateOrThrow(request, captcha);

            logger.info("User try to login: " + identifier);

            final boolean remember = "true".equals(params.getString("remember", "false"));
            // (user, temporary password, is temporary password (false by default))
            final XWebUser user = dataSource.getUserWithId(identifier, password);

            // Check for login, you can not login with no login role
            if (user != null) {
                setUser(request, user);

                if(remember) {
                    final String uuid = getDataSource().generateUUID(identifier);

                    if(uuid != null) {
                        response.setContentType("text/plain");
                        CookieTools.addCookie(request, response, Constants.COOKIE_AUTH_REMEMBER, uuid, cookieAge);
                        response.getWriter().write(uuid);
                        response.getWriter().flush();
                    }
                } else {
                    CookieTools.removeCookie(request, response, Constants.COOKIE_AUTH_REMEMBER);
                }

                logger.info(identifier + " successfully login into system. Remember = " + remember);
            } else {
                throw new ModuleException(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        }
        else if(params.containsKey("logout")) {
            request.getSession().invalidate();
            CookieTools.removeCookie(request, response, Constants.COOKIE_AUTH_REMEMBER);
        }

    }

    public XWebUser getUser(final HttpServletRequest request) {
        if (request != null) {
            final HttpSession session = request.getSession();
            if(session != null) {
                final XWebUser user = (XWebUser) session.getAttribute(SESSION_USER);
                return user;
            }
        }
        return null;
    }

    /**
     * Set user as authenticated user
     * @param request
     * @param user
     */
    public void setUser(final HttpServletRequest request, final XWebUser user) {
        request.getSession().setAttribute(SESSION_USER, user);
    }

    public boolean isAuthenticated(final HttpServletRequest request) {
        return request.getSession().getAttribute(SESSION_USER) != null;
    }

    /**
     * Get data source. It will load datasource if it's require.
     * @return
     */
    private AuthenticationModuleData getDataSource() {
        if(dataSource == null) {
            dataSource = getManager().getImplementedOrThrow(AuthenticationModuleData.class, dataName);
        }
        return dataSource;
    }

}
