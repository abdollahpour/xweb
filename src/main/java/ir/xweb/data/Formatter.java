
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;

/**
 * XWeb object data formatter.
 */
public interface Formatter {

    /**
     * Write XWeb object to writer.
     * @param writer Writer
     * @param object XWeb object
     * @throws IOException If error happens in writing
     */
    void write(final Writer writer, final Object object) throws IOException;

    /**
     * Get data content type <a href="http://www.rfc-editor.org/rfc/rfc3023.txt">RFC3023</a>.
     * @return content type
     */
    String getContentType();

    /**
     * Check that this formatter support by request or not.
     * @param request Request
     * @return True if it support
     */
    boolean isSupported(HttpServletRequest request);

    /**
     * Check that formatter support by accept HTTP header.
     * @param accept Accept header value
     * @return True if it support
     */
    boolean isSupported(String accept);

}
