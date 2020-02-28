package org.lndroid.framework.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FieldInfo {
    String name();
    String help();
    Class<? extends IFieldConvertor>[] convertors() default {};
}
