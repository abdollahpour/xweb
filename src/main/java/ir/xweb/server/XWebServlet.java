/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

import ir.xweb.module.Manager;
import ir.xweb.module.Module;
import ir.xweb.module.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XWebServlet extends HttpServlet {

	private static final long serialVersionUID = -2953215151807131181L;
	
	private final static Logger logger = LoggerFactory.getLogger("XWebServlet");

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		Manager manager = (Manager)getServletContext().getAttribute(Constants.SESSION_MANAGER);
		
		String api = request.getParameter("api");
		
		Module module = manager.getModule(api);
		if(module != null) {
            try {
                XWebUser user = (XWebUser) request.getSession().getAttribute(Constants.SESSION_USER);

			    module.process(getServletContext(), request, response, user == null ? null : user.getRole());
            } catch (ModuleException ex) {

                //int errorCode = ex.getErrorCode();
                int responseCode = ex.getReponseCode();

                if(responseCode == HttpServletResponse.SC_NOT_FOUND) {
                    logger.debug("Data not found. " +
                            "Module: " + module.getInfo().getName() +
                            " Code: " + ex.getErrorCode(), ex);
                } else if(module.redirectAuthFail() && responseCode == HttpServletResponse.SC_UNAUTHORIZED) {
                    // OK!
                    // When Authentication exception happen, we it be possible (module have this feature and
                    // redirect URL was available) we will redirect connection to authentication page

                    // TODO: Not support after modular Authentication
                    /*String redirect = manager.getProperty(Constants.AUTHENTICATION_REDIRECT);
                    if(redirect != null) {
                        String uri = request.getRequestURI().substring(request.getContextPath().length());

                        response.sendRedirect(getServletContext().getContextPath()
                                + redirect + "?url="
                                + getServletContext().getContextPath()
                                + URLEncoder.encode(uri + "?" + request.getQueryString(), "UTF-8"));

                        return;
                    }*/
                } else {
                    logger.error("Error in module process." +
                            " Module: " + module.getInfo().getName() +
                            " Code: " + ex.getErrorCode(), ex);
                }

                if(!response.isCommitted()) {
                    response.sendError(
                            responseCode > 0 ? responseCode: HttpServletResponse.SC_BAD_REQUEST,
                            "Error in module: " + api + " Cause: " + ex.getMessage());
                }
            } catch (Exception ex) {
                logger.error("Error in module process. Module: " 
                			+ module.getInfo().getName() 
            				+ " : " + ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Error in module: " + api + " Cause: " + ex.getMessage());
            }
		} else {
            logger.info("Call illegal API: " + api);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Call illegal API: " + api);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		doGet(req, resp);
	}
}
