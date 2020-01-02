package com.chenpp.spring.web.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CPRequestParam {
    String value() default "";
}
