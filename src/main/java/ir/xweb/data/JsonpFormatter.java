package ir.xweb.data;

import ir.xweb.util.MimeType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JsonpFormatter extends JsonFormatter {

    public final static String PROPERTY_JSONP_CALLBACK = "jsonp.callback";

    private final String mimeType = MimeType.get("jsonp");

    private final HashMap<String, String> properties;

    public JsonpFormatter(final HashMap<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public void write(final Writer writer, final Object object) throws IOException {
        try {
            if(object instanceof Map) {
                final String callback = properties.get(PROPERTY_JSONP_CALLBACK);
                if(callback == null) {
                    writer.write("jsonCallback(");
                } else {
                    writer.append(callback).write("(");
                }
                ((JSONObject) write(object)).write(writer);
                writer.write(");");

            } else {
                writer.write("jsonCallback(");
                ((JSONArray) write(object)).write(writer);
                writer.write(");");
            }
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String getContentType() {
        return mimeType;
    }

    @Override
    public boolean isSupported(final HttpServletRequest request) {
        return isSupported(request.getHeader("Accept"));
    }

    @Override
    public boolean isSupported(final String accept) {
        if(accept != null) {
            if(accept.indexOf(mimeType) > -1) {
                return true;
            }
        }
        return false;
    }

}
