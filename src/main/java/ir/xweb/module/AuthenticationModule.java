package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.server.XWebUser;
import ir.xweb.util.Base64;
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

public class AuthenticationModule extends Module {

    private Logger logger = LoggerFactory.getLogger("AuthenticationModule");

    private final static int UUID_AGE_SEC = 60 * 60 * 24 * 30; // 1 month

    private final int cookieAge;

    public AuthenticationModule(final Manager manager, final ModuleInfo info, final ModuleParam properties) {
        super(manager, info, properties);

        cookieAge = properties.getInt("cookie-age", UUID_AGE_SEC);
    }

    @Override
    public void doFilter(
            ServletContext context,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {

        HttpSession session = request.getSession();

        XWebUser user = (XWebUser) session.getAttribute(Constants.SESSION_USER);

        // pre-check to speedup checking
        if(user != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // check for permission patterns
        URI uri = null;
        try {
            uri = new URI(request.getRequestURI());
        } catch (Exception ex) {
            // It will never happen because it passed by http request
            throw new IOException(ex);
        }

        // we don't care about context path, so we trunk it
        String path = uri.getPath().substring(request.getContextPath().length());

        String redirect = getProperties().getString(Constants.AUTHENTICATION_REDIRECT, null);
        String check = getProperties().getString(Constants.AUTHENTICATION_CHECK, null);
        String ignore = getProperties().getString(Constants.AUTHENTICATION_IGNORE, null);

        //System.out.println(path + " " + path.matches(check));

        if(uri.equals(redirect)){
            filterChain.doFilter(request, response);
            return;
        }



        if(check != null && !path.matches(check)) {
            filterChain.doFilter(request, response);
            return;
        }

        if(ignore != null && path.matches(ignore)) {
            filterChain.doFilter(request, response);
            return;
        }


        // login with cookie
        String uuid = CookieTools.getCookieValue(request, Constants.COOKIE_AUTH_REMEMBER);
        if(uuid != null) {
            logger.debug("Try to login with cookie. UUID: " + uuid);
            user = getUserWithUUID(context, uuid);
            if(user != null) {
                logger.debug("User successfully login with UUID: " + uuid);
                session.setAttribute(Constants.SESSION_USER, user);
                filterChain.doFilter(request, response);
                return;
            } else {
                logger.debug("UUID not valid for login (maybe expired: " + uuid);
            }
        }

        String header = request.getHeader("Authorization");
        if (header != null) {
            logger.debug("Try to authenticate by HTTP authentication");

            if(header.startsWith("Basic")) {
                String base64Token = header.substring(6);
                String token = new String(Base64.decode(base64Token));

                user = getUserWithUUID (context, uuid);
                if(user != null) {
                    logger.debug("User successfully login with HTTP authentication: " + token);
                    session.setAttribute(Constants.SESSION_USER, user);
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    logger.debug("HTTP token not valid for login. maybe expired: " + token);
                }

            } else {
                throw new IllegalStateException("Unsupported authentication method");
            }
        }


        // We handle API requests with different authentication method (Role base)
        boolean isApiCall = path.equals(Constants.MODULE_URI_PERFIX);

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
    public void process(ServletContext context, HttpServletRequest request,
                        HttpServletResponse response, ModuleParam params,
                        HashMap<String, FileItem> files) throws IOException {

        String action  = params.validate("action", "login|check|logout", true).getString(null);
        logger.debug("Login module request for action = " + action);

        /**
         * Actions:
         * login: Fully login into system. User need to enter captcha code also
         * temp_pass: generate temporary password for simple authenticate transactions
         */

        if("login".equals(action)) {
            String identifier = params.validate(Constants.MODULE_LOGIN_PARAM_IDENTIFIER, null, true).getString(null);
            // Password hashed with MD5
            String password = params.validate(Constants.MODULE_LOGIN_PARAM_PASSWORD, null, true).getString(null);
            String captcha = params.validate(Constants.MODULE_LOGIN_PARAM_CAPTCHA, CaptchaModule.SESSION_CAPTCHA_PATTERN, true).getString(null);

            CaptchaModule.validateOrThrow(request, captcha);

            logger.info("User try to login: " + identifier);

            boolean remember = "true".equals(params.getString("remember", "false"));
            // (user, temporary password, is temporary password (false by default))
            XWebUser user = getUserWithId(context, identifier, password);

            if (user != null) {
                request.getSession().setAttribute(Constants.SESSION_USER, user);

                if(remember) {
                    String uuid = generateUUID(context, identifier);

                    if(uuid != null) {
                        CookieTools.addCookie(request, response, Constants.COOKIE_AUTH_REMEMBER, uuid, cookieAge);
                        response.getWriter().write(uuid);
                    }
                } else {
                    CookieTools.removeCookie(request, response, Constants.COOKIE_AUTH_REMEMBER);
                }

                logger.info(identifier + " successfully login into system. Remember = " + remember);

                return;
            } else {
                throw new ModuleException(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        } else if("logout".equals(action)) {
            request.getSession().invalidate();
            CookieTools.removeCookie(request, response, Constants.COOKIE_AUTH_REMEMBER);
        }
    }

    public XWebUser getUserWithUUID(ServletContext context, String uuid) {
        return null;
    }

    public XWebUser getUserWithId(ServletContext context, String userId, String pass) {
        return null;
    }

    public String generateUUID(ServletContext context, String userId) {
        return null;
    }

    public boolean createDefaultUser(ServletContext context, String username) {
        return false;
    }

}
