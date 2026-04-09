package com.eaglepoint.workforce.audit;

import com.eaglepoint.workforce.enums.AuditAction;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    AuditAction action();
    String resource();
}
