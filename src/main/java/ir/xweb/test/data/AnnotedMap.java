package ir.xweb.test.data;

import java.util.HashMap;

public class AnnotedMap extends HashMap<String, Object> {

    final String name;

    AnnotedMap(String name) {
        this.name = name;
    }

}