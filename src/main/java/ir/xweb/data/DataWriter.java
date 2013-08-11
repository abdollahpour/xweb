package ir.xweb.data;

import java.io.IOException;
import java.io.Writer;

public interface DataWriter {

    void write(Writer writer) throws IOException;

    void add(String key, Object value);

    void start(String name);

    void startArray(String name);

    void end();

    void release();

    String getContentType();

}
