package com.techmart.ejb.interceptor;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {

    int maxRetries() default 3;

    long delayMs() default 1000;

    double backoffMultiplier() default 2.0;
}
