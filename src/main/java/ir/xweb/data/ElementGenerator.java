package ir.xweb.data;

import ir.xweb.util.Validator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

class ElementGenerator {

    private List<Method> methods;

    private List<Field> fields;

    private String name;

    private static Class byteArrayClass;

    static {
        Object o = new byte[0];
        byteArrayClass = o.getClass();
    }

    public ElementGenerator(String name, List<Method> methods, List<Field> fields) {
        this.name = name;
        this.methods = methods;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setValues(DataService service, Object object, Map<String, String> params) {
        for(Field f:fields) {
            XWebDataElement annotation = f.getAnnotation(XWebDataElement.class);
            if(annotation.writable()) {
                String key = annotation.key();
                if(key.length() == 0) {
                    key = f.getName();
                }


                String value = params.get(key);

                if(value != null && value.length() > 0) {
                    String validator = annotation.validator();
                    if(validator != null) {
                        if(Validator.validate(validator, value, true)) {
                            Class<?> type = f.getType();

                            try {
                                if(type == String.class) {
                                    f.set(object, value.toString());
                                } else if(type == Integer.class) {
                                    f.set(object, Integer.valueOf(value));
                                } else if(type == Boolean.class) {
                                    f.setBoolean(object, Boolean.parseBoolean(value));
                                } else if(type == Byte.class) {
                                    f.setByte(object, Byte.valueOf(value).byteValue());
                                } else if(type == Character.class) {
                                    if(value.length() == 0) {
                                        f.setChar(object, value.charAt(0));
                                    }
                                } else if(type == Double.class) {
                                    f.setDouble(object, Double.parseDouble(value));
                                } else if(type == Float.class) {
                                    f.setFloat(object, Float.parseFloat(value));
                                } else if(type == Long.class) {
                                    f.setLong(object, Long.parseLong(value));
                                } else if(type == Short.class) {
                                    f.setShort(object, Short.parseShort(value));
                                } else if(type == Short.class) {
                                    f.setShort(object, Short.parseShort(value));
                                }
                            } catch (Exception ex) {
                                // ignore set value
                                throw new IllegalArgumentException(
                                        "Error in set value for " + f.getName() + ": " + value, ex);
                            }
                        } else {
                            throw new IllegalArgumentException("Illegal value for " + f.getName() + ": " + value);
                        }
                    }
                }
            }
        }
    }

    public void generateValues(DataService service, Object object) throws IllegalAccessException {
        for(Method m:methods) {
            String key = m.getAnnotation(XWebDataElement.class).key();
            if(key.length() == 0) {
                key = m.getName();
            }

            Object value = null;
            try {
                value = m.invoke(object);
            } catch (Exception ex) {
                throw new IllegalAccessException();
            }

            if(value != null) {
                service._writeObject(key, value);
            }
        }

        for(Field f:fields) {
            String key = f.getAnnotation(XWebDataElement.class).key();
            if(key.length() == 0) {
                key = f.getName();
            }

            Object value = null;
            try {
                value = f.get(object);
            } catch (Exception ex) {
                throw new IllegalAccessException();
            }

            if(value != null) {
                service._writeObject(key, value);
            }
        }
    }



}
