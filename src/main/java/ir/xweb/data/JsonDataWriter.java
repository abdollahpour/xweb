package ir.xweb.data;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

class JsonDataWriter implements DataWriter {

    private Object root;

    private Object object;

    private ArrayList<Object> parents = new ArrayList<Object>();

    public void write(Writer w) throws IOException {
        if(root != null) {
            try {
                if(root instanceof JSONObject) {
                    ((JSONObject)root).write(w);
                } else if(root instanceof JSONArray) {
                    ((JSONArray)root).write(w);
                }
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            throw new IOException("null root");
        }
    }

    @Override
    public void add(String key, Object value) {
        if(object == null) {
            throw new IllegalArgumentException("Please start first");
        } else if(object instanceof JSONObject) {
            try {
                ((JSONObject)object).put(key, value);
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
            }
        } else if(object instanceof JSONArray) {
            try {
                ((JSONArray)object).put(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }

    @Override
    public void start(String name) {
        Object newObject = new JSONObject();
        if(root == null) {
            root = newObject;
        }
        parents.add(newObject);

        if(object != null) {
            add(name, newObject);
        }
        object = newObject;
    }

    @Override
    public void startArray(String name) {
        Object newObject = new JSONArray();
        if(root == null) {
            root = newObject;
        }
        parents.add(newObject);

        if(object != null) {
            add(name, newObject);
        }
        object = newObject;
    }

    @Override
    public void end() {
        if(parents.size() > 0) {
            parents.remove(parents.size() - 1);
            if(parents.size() > 0) {
                object = parents.get(parents.size() - 1);
            } else {
                object = null;
            }
        } else {
            throw new IllegalStateException("No active node");
        }
    }

    @Override
    public void release() {
        parents = new ArrayList<Object>();
        root = null;
        object = null;
    }

    /*private Object getParent() {
        if(parents.size() > 0) {
            return parents.get(parents.size() - 1);
        }
        return null;
    }*/

    @Override
    public String getContentType() {
        return "application/json;charset=utf-8";
    }

}
