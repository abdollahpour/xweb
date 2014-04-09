package ir.xweb.data;

import java.io.IOException;
import java.io.Writer;

public interface Formatter {

    public void write(final Writer writer, final Object object) throws IOException;

    public String getMimeType();

}
