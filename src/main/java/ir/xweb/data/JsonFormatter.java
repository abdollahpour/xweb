
package ir.xweb.data;

import ir.xweb.util.MimeType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

public class JsonFormatter implements Formatter {

    private final String mimeType = MimeType.get("json");

    @Override
    public void write(final Writer writer, final Object object) throws IOException {
        try {
            if(object instanceof Map) {
                JSONObject obj = (JSONObject) write(object);
                obj.write(writer);
                writer.flush();
            } else {
                JSONArray array = (JSONArray) write(object);
                array.write(writer);
                writer.flush();
            }
        } catch (JSONException ex) {
            throw new IOException(ex);
        }
    }

    protected Object write(final Object object) throws JSONException {
        if(object instanceof Map) {
            final JSONObject json = new JSONObject();

            final Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> e:map.entrySet()) {
                json.put(e.getKey().toString(), write(e.getValue()));
            }

            return json;
        } else if (object instanceof Collection) {
            final JSONArray array = new JSONArray();

            final Collection<?> list = (Collection<?>) object;
            for (Object o:list) {
                array.put(write(o));
            }

            return array;
        }
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        return mimeType;
    }

}
