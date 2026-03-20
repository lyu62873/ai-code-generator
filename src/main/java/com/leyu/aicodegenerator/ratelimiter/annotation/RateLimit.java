package com.leyu.aicodegenerator.ratelimiter.annotation;

import com.leyu.aicodegenerator.ratelimiter.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Rate limit key prefix.
     */
    String key() default "";
    
    /**
     * Max requests per time window.
     */
    int rate() default 10;
    
    /**
     * Time window (seconds).
     */
    int rateInterval() default 1;
    
    /**
     * Rate limit type.
     */
    RateLimitType limitType() default RateLimitType.USER;
    
    /**
     * Rate limit hint message.
     */
    String message() default "Request too frequently, try again later";
}
