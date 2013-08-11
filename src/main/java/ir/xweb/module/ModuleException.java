package ir.xweb.module;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ModuleException extends IOException {

	private static final long serialVersionUID = -1278866021646593493L;

	private int responceCode;

    private int errorCode = -1;

    public ModuleException(String message) {
        this(HttpServletResponse.SC_BAD_REQUEST, message, null, -1);
    }

    public ModuleException(int responceCode, String message) {
        this(responceCode, message, null, -1);
    }

    public ModuleException(int responceCode, String message, Exception ex) {
        this(responceCode, message, ex, -1);
    }

    public ModuleException(String message, Exception ex) {
        this(HttpServletResponse.SC_BAD_REQUEST, message, ex, -1);
    }

    public ModuleException(int responceCode, String message, Exception ex, int errorCode) {
        super(message, ex);
        this.responceCode = responceCode;
        this.errorCode = errorCode;
    }

    public ModuleException(String message, Exception ex, int errorCode) {
        this(HttpServletResponse.SC_BAD_REQUEST, message, ex, errorCode);
    }

    public ModuleException(String message, int errorCode) {
        this(HttpServletResponse.SC_BAD_REQUEST, message, null, errorCode);
    }

    public int getReponseCode() {
        return responceCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
