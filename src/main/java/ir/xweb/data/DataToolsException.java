package ir.xweb.data;

import java.io.IOException;

public class DataToolsException extends IOException {

    private final String name;

    public DataToolsException(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
