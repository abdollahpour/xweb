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
        if(api == null) {
            throw new ServletException(Constants.SESSION_MANAGER + " parameter not found (please set module name)");
        }
		
		Module module = manager.getModule(api);
		if(module != null) {
            //logger.trace("Call module: " + module.getInfo().getName());

            try {
                XWebUser user = (XWebUser) request.getSession().getAttribute(Constants.SESSION_USER);

			    module.process(getServletContext(), request, response, user == null ? null : user.getRole());
            } catch (ModuleException ex) {

                //int errorCode = ex.getErrorCode();
                final Integer responseCode = ex.getResponseCode();

                // trace error or not
                final boolean trace = (responseCode == null) ||
                    (responseCode != HttpServletResponse.SC_NOT_FOUND) ||
                    (responseCode != HttpServletResponse.SC_UNAUTHORIZED);

                if(trace) {
                    logger.error(responseCode + " error", ex);
                }

                // Write error to output
                if(!response.isCommitted()) {
                    if (ex.getErrorCode() != null) {
                        response.addHeader("xweb-error-code", Integer.toString(ex.getErrorCode()));
                    }

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    if(responseCode != null) {
                        response.sendError(responseCode, ex.toString());
                    } else {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.toString());
                    }
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
