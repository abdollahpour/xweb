/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

import ir.xweb.module.DataSource;
import ir.xweb.module.Manager;
import ir.xweb.module.Module;
import ir.xweb.module.UserDataSource;
import ir.xweb.util.Base64;
import ir.xweb.util.CookieTools;

import java.io.IOException;
import java.lang.String;
import java.net.URI;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Authentication implements Filter {
	
	private Logger logger = LoggerFactory.getLogger(Authentication.class);
	
	private ServletContext context;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		context = filterConfig.getServletContext();
	}

	@SuppressWarnings("unused")
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest  request = (HttpServletRequest)  req;
		HttpServletResponse  response = (HttpServletResponse) resp;
		HttpSession  session = request.getSession();

        // Damn servlet!
        // http://stackoverflow.com/questions/469874/how-do-i-correctly-decode-unicode-parameters-passed-to-a-servlet
        // We should to add to server.xml too:
        // <Connector    URIEncoding="UTF-8"    connectionTimeout="20000" port="8080" protocol="HTTP/1.1" ...>
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        XWebUser user = (XWebUser) session.getAttribute(Constants.SESSION_USER);

        // pre-check to speedup checking
        if(user != null) {
            chain.doFilter(request, response);
            return;
        }
		
		Manager manager = (Manager) context.getAttribute(Constants.SESSION_MANAGER);
		DataSource dataSource = manager.getDataSource(Constants.DATA_SOURCE_USER);

		if(dataSource == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Data sources don't available!");
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

        String redirect = manager.getProperty(Constants.AUTHENTICATION_REDIRECT);
        String check = manager.getProperty(Constants.AUTHENTICATION_CHECK);
        String ignore = manager.getProperty(Constants.AUTHENTICATION_IGNORE);

        //System.out.println(path + " " + path.matches(check));

        if(uri.equals(redirect)){
            chain.doFilter(request, response);
            return;
        }



        if(check != null && !path.matches(check)) {
            chain.doFilter(request, response);
            return;
        }

        if(ignore != null && path.matches(ignore)) {
            chain.doFilter(request, response);
            return;
        }


		// login with cookie
        String uuid = CookieTools.getCookieValue(request, Constants.COOKIE_AUTH_REMEMBER);
        if(uuid != null) {
            logger.debug("Try to login with cookie. UUID: " + uuid);
            user = (XWebUser) dataSource.getData(context, UserDataSource.DATA_SOURCE_USER_AUTH_REMEMBER_CODE, uuid);
            if(user != null) {
                logger.debug("User successfully login with UUID: " + uuid);
                session.setAttribute(Constants.SESSION_USER, user);
                chain.doFilter(request, response);
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

                user = (XWebUser) dataSource.getData(context, UserDataSource.DATA_SOURCE_USER_AUTH_REMEMBER_CODE, uuid);
                if(user != null) {
                    logger.debug("User successfully login with HTTP authentication: " + token);
                    session.setAttribute(Constants.SESSION_USER, user);
                    chain.doFilter(request, response);
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
            chain.doFilter( request, response );
            return;

            /*String role = user == null ? Constants.ROLE_GUEST : user.getRole();

			String moduleName = request.getParameter(Constants.MODULE_NAME_PARAMETER);
            Module module = moduleName == null ? null : manager.getModule(moduleName);

            if(module !=  null) {
				String role = user == null ? Constants.ROLE_GUEST : user.getRole();

				if(module.hasRole(role, request.getParameter("action"))) {
                    //logger.debug("Access to API. role: " + role + " module: " + moduleName + " action: " + request.getParameter("action"));
					chain.doFilter(request, response);
					return;
				} else {
                    String message = "Illegal request for:  " + role + " URI: " + uri + "?" + request.getQueryString();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
                    logger.info(message);
                }
			} else {
				String message = "Call unknown module: " + moduleName + " URI: " + uri + "?" + request.getQueryString();
				logger.info(message);
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			}*/
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
                chain.doFilter( request, response );
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

    /*private String makePathRelative(String redirect, String url) {
        int index = 0;
        int pos = 0;
        while((index = redirect.indexOf('/', pos)) > -1) {
            if(index < url.length()) {
                System.out.println(pos + ":" + index);
                if(redirect.substring(pos, index).equals(url.substring(pos, index))) {
                    pos = index + 1;
                }
            }
        }

        return url.substring(pos);
    }*/

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

}
