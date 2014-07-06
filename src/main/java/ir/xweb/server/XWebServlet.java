/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

import ir.xweb.data.DataTools;
import ir.xweb.data.DataToolsException;
import ir.xweb.module.Manager;
import ir.xweb.module.Module;
import ir.xweb.module.ModuleException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
            }
            catch (DataToolsException ex) {
                if(!response.isCommitted()) {
                    writeException(manager, request, response, ex);
                }
            }
            catch (ModuleException ex) {

                //int errorCode = ex.getErrorCode();
                final Integer responseCode = ex.getResponseCode();

                // trace error or not
                final boolean trace = (responseCode == null) ||
                    ((responseCode != HttpServletResponse.SC_NOT_FOUND) &&
                    (responseCode != HttpServletResponse.SC_UNAUTHORIZED));

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
            }
            catch (Exception ex) {
                logger.error("Error in module process. Module: " 
                			+ module.getInfo().getName()
            				+ " : " + ex.getMessage(), ex);

                if(!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Error in module: " + api + " Cause: " + ex.getMessage());
                }
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

    /**
     * Write error response. Error support 4 format, JSON, XML, HTML and text.
     * @param request
     * @param response
     * @param ex
     */
    private void writeException(
            final Manager manager,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final DataToolsException ex)
    {
        // TODO: Maybe we can use DataTools to integrate with the other parts

        String accept = request.getHeader("Accept");
        if(accept == null) {
            accept = "";
        }

        try {
            // Write with JSON
            if (accept.indexOf("application/json") > -1) {
                final JSONObject o = new JSONObject();
                o.put("exception", ex.getClass().getSimpleName());
                o.put("param", ex.getName());
                o.put("message", "Value is not value for " + ex.getName());

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding(manager.getDefaultEncoding());
                response.getWriter().write(o.toString());
            }

            // Write XML with DOM
            else if (accept.indexOf("application/xml") > -1 || accept.indexOf("text/xml") > -1) {
                final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                final Document doc = docBuilder.newDocument();
                final Element rootElement = doc.createElement("error");
                doc.appendChild(rootElement);

                final Element exception = doc.createElement("exception");
                exception.appendChild(doc.createTextNode(ex.getClass().getSimpleName()));
                rootElement.appendChild(exception);

                final Element param = doc.createElement("param");
                param.appendChild(doc.createTextNode(ex.getName()));
                rootElement.appendChild(param);

                final Element message = doc.createElement("message");
                message.appendChild(doc.createTextNode("Value is not value for " + ex.getName()));
                rootElement.appendChild(message);

                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                final DOMSource source = new DOMSource(doc);
                final StreamResult result = new StreamResult(response.getWriter());

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/json");
                response.setCharacterEncoding(manager.getDefaultEncoding());
                transformer.transform(source, result);
            }

            else if (accept.indexOf("text/html") > -1) {
                final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                final Document doc = docBuilder.newDocument();
                final Element html = doc.createElement("html");
                doc.appendChild(html);

                final Element body = doc.createElement("body");
                html.appendChild(body);

                final Element message = doc.createElement("h2");
                message.appendChild(doc.createTextNode("Value is not value for " + ex.getName()));
                body.appendChild(message);

                final Element exception = doc.createElement("p");
                exception.appendChild(doc.createTextNode("exception: " + ex.getClass().getSimpleName()));
                body.appendChild(exception);

                final Element param = doc.createElement("p");
                param.appendChild(doc.createTextNode("param: " + ex.getName()));
                body.appendChild(param);

                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                final DOMSource source = new DOMSource(doc);
                final StreamResult result = new StreamResult(response.getWriter());

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/html");
                response.setCharacterEncoding(manager.getDefaultEncoding());
                transformer.transform(source, result);
            }

            else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/html");
                response.setCharacterEncoding(manager.getDefaultEncoding());
                response.getWriter()
                        .append("exception: ").append("ex.getClass().getSimpleName()").append('\n')
                        .append("param").append(ex.getName()).append('\n')
                        .append("message").append("Illegal value for " + ex.getName());
            }
        }
        catch (Exception ex2) {
            logger.error("Error to write error!", ex2);
        }
	}
}
