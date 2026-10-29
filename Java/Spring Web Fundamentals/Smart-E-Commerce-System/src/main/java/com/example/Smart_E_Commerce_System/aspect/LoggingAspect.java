package com.example.Smart_E_Commerce_System.aspect;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Centralized logging for the service layer, replacing log statements that would
 * otherwise be repeated in every service method.
 *
 *  @Before          - method entry with (masked) arguments
 *  @AfterReturning  - successful result summary
 *  @AfterThrowing   - exception type and message
 *  @After           - method exit, runs whether it succeeded or failed (like finally)
 */
@Aspect
@Component
@Order(2) // runs inside PerformanceMonitoringAspect, so timing includes logging overhead consistently
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /** Record components with these names are never written to the logs. */
    private static final Set<String> SENSITIVE = Set.of("password", "passwordhash", "token", "secret");

    @Before("com.example.Smart_E_Commerce_System.aspect.AppPointcuts.serviceLayer()")
    public void logEntry(JoinPoint jp) {
        if (log.isDebugEnabled()) {
            log.debug("--> {}({})", methodName(jp), describeArgs(jp.getArgs()));
        }
    }

    @AfterReturning(pointcut = "com.example.Smart_E_Commerce_System.aspect.AppPointcuts.serviceLayer()", returning = "result")
    public void logSuccess(JoinPoint jp, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("<-- {} returned {}", methodName(jp), describeResult(result));
        }
    }

    @AfterThrowing(pointcut = "com.example.Smart_E_Commerce_System.aspect.AppPointcuts.serviceLayer()", throwing = "ex")
    public void logFailure(JoinPoint jp, Throwable ex) {
        // Message only: expected business errors (not found, duplicate) don't need stack traces.
        // Truly unexpected errors are logged with a stack trace by the exception handlers.
        log.warn("<-- {} threw {}: {}", methodName(jp), ex.getClass().getSimpleName(), ex.getMessage());
    }

    @After("com.example.Smart_E_Commerce_System.aspect.AppPointcuts.serviceLayer()")
    public void logExit(JoinPoint jp) {
        log.trace("[exit] {}", methodName(jp));
    }

    // ---------------- helpers ----------------

    private String methodName(JoinPoint jp) {
        return jp.getSignature().getDeclaringType().getSimpleName() + "." + jp.getSignature().getName();
    }

    private String describeArgs(Object[] args) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Object arg : args) {
            joiner.add(describe(arg));
        }
        return joiner.toString();
    }

    /** Readable, safe description of one argument; DTO records are printed with secrets masked. */
    private String describe(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof Pageable p) {
            return "Pageable(page=" + p.getPageNumber() + ", size=" + p.getPageSize() + ", sort=" + p.getSort() + ")";
        }
        if (arg.getClass().isRecord()) {
            return describeRecord(arg);
        }
        return String.valueOf(arg);
    }

    private String describeRecord(Object record) {
        StringJoiner joiner = new StringJoiner(", ", record.getClass().getSimpleName() + "(", ")");
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            String name = component.getName();
            if (SENSITIVE.contains(name.toLowerCase())) {
                joiner.add(name + "=****");
                continue;
            }
            try {
                joiner.add(name + "=" + component.getAccessor().invoke(record));
            } catch (ReflectiveOperationException e) {
                joiner.add(name + "=?");
            }
        }
        return joiner.toString();
    }

    /** Summarizes results instead of dumping them, so a page of 100 products is one short line. */
    private String describeResult(Object result) {
        if (result == null) {
            return "void/null";
        }
        if (result instanceof PageResponse<?> page) {
            return "PageResponse(" + page.content().size() + " of " + page.totalElements() + " items)";
        }
        if (result instanceof Collection<?> c) {
            return result.getClass().getSimpleName() + "(size=" + c.size() + ")";
        }
        if (result instanceof java.util.Map<?, ?> m) {
            return "Map(size=" + m.size() + ")";
        }
        return result.getClass().getSimpleName();
    }
}
