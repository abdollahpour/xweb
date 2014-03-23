package ir.xweb.data;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface XWebDataElement {

    String key() default "";

    String write() default "";

    String read() default "";

    boolean writable() default false;

    String validator() default "";

}
