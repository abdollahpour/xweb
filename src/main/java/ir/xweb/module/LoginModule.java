/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.module;

import ir.xweb.server.Constants;
import ir.xweb.server.XWebUser;
import ir.xweb.util.CookieTools;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginModule extends Module {
	
	private final static int UUID_AGE_SEC = 60 * 60 * 24 * 30; // 1 month
	
	private Logger logger = LoggerFactory.getLogger(LoginModule.class);

    private int cookieAge;

    public LoginModule(Manager manager, ModuleInfo info, ModuleParam properties) {
        super(manager, info, properties);
        cookieAge = properties.getInt("cookie-age", UUID_AGE_SEC);
    }

	@Override
	public void process(ServletContext context, HttpServletRequest request,
			HttpServletResponse response, ModuleParam params,
			HashMap<String, FileItem> files) throws IOException {
		
		Manager manager = (Manager) context.getAttribute(Constants.SESSION_MANAGER);
		DataSource dataSource = manager.getDataSource(Constants.DATA_SOURCE_USER);

		if(dataSource == null) {
            logger.error("Data sources not found! Please make sure that you have proper xweb.xml config");
			throw new ModuleException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Data sources don't available!");
		}
		
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
			XWebUser user = (XWebUser) dataSource.getData(context, UserDataSource.DATA_SOURCE_USER_AUTH_PASS, identifier, password);

			if (user != null) {
                request.getSession().setAttribute(Constants.SESSION_USER, user);

                if(remember) {
                    String uuid = (String) dataSource.getData(context, UserDataSource.DATA_SOURCE_USER_REMEMBER, identifier, true);

                    CookieTools.addCookie(request, response, Constants.COOKIE_AUTH_REMEMBER, uuid, cookieAge);
                    response.getWriter().write(uuid);
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

}
