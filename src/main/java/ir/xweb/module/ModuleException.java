
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Module custom exception.
 */
public class ModuleException extends IOException {

    /**
     * Unique serial.
     */
    private static final long serialVersionUID = -1278866021646593493L;

    /**
     * <code>HttpServletResponse</code> response code.
     */
    private final Integer responseCode;

    /**
     * Error code for the client.
     */
    private final Integer errorCode;

    /**
     * Error message for client.
     */
    private final String errorMessage;


    /**
     * Error message.
     */
    private final Throwable throwable;

    /**
     *
     * @param responseCode <code>HttpServletResponse</code> response code
     * @param errorCode Error code for the client
     * @param errorMessage Error message for client
     * @param throwable Error message
     */
    public ModuleException(
        final Integer responseCode,
        final Integer errorCode,
        final String errorMessage,
        final Throwable throwable)
    {
        this.responseCode = responseCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.throwable = throwable;
    }

    /**
     *
     * @param responseCode <code>HttpServletResponse</code> response code
     * @param errorCode Error code for the client
     * @param errorMessage Error message for client
     */
    public ModuleException(final int responseCode, final int errorCode, final String errorMessage)
    {
        this(responseCode, errorCode, errorMessage, null);
    }

    /**
     *
     * @param responseCode <code>HttpServletResponse</code> response code
     * @param errorMessage Error message for client
     */
    public ModuleException(final int responseCode, final String errorMessage)
    {
        this(responseCode, null, errorMessage, null);
    }

    /**
     *
     * @param errorMessage Error message for client
     * @param throwable Error message
     */
    public ModuleException(final String errorMessage, final Throwable throwable)
    {
        this(null, null, errorMessage, throwable);
    }

    /**
     *
     * @param errorMessage Error message for client
     */
    public ModuleException(final String errorMessage)
    {
        this(null, null, errorMessage, null);
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        final JSONObject object = new JSONObject();

        if(errorCode != null) {
            object.put("code", errorCode);
        }
        if(errorMessage != null) {
            object.put("message", errorMessage);
        }
        else if(throwable != null && throwable.getMessage() != null) {
            object.put("message", throwable.getMessage());
        }
        if(throwable != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);

            object.put("trace", sw.toString());
        }
        return object.toString();
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
