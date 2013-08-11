package ir.xweb.data;


import java.io.IOException;
import java.io.Writer;

public class JsonPDataWriter implements DataWriter {

    private JsonDataWriter json = new JsonDataWriter();

    @Override
    public void write(Writer writer) throws IOException {
        writer.write("jsonCallback(");
        json.write(writer);
        writer.write(");");
    }

    @Override
    public void add(String key, Object value) {
        json.add(key, value);
    }

    @Override
    public void start(String name) {
        json.start(name);
    }

    @Override
    public void startArray(String name) {
        json.startArray(name);
    }

    @Override
    public void end() {
        json.end();
    }

    @Override
    public void release() {
        json.release();
    }

    @Override
    public String getContentType() {
        return json.getContentType();
    }
}
