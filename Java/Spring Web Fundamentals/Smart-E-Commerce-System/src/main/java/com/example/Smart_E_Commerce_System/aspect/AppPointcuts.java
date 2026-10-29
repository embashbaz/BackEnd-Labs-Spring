package com.example.Smart_E_Commerce_System.aspect;

import org.aspectj.lang.annotation.Pointcut;

/**
 * Named, reusable pointcuts. Defining them once here keeps the aspects readable
 * and means a package change only has to be fixed in one place.
 */
public class AppPointcuts {

    /** Every public method of every class in the service layer (service and service.impl). */
    @Pointcut("within(com.example.Smart_E_Commerce_System.service..*) && execution(public * *(..))")
    public void serviceLayer() {}

    /** REST endpoints, excluding the metrics endpoint itself so it doesn't measure its own calls. */
    @Pointcut("within(com.example.Smart_E_Commerce_System.controller..*)"
            + " && @within(org.springframework.web.bind.annotation.RestController)"
            + " && !within(com.example.Smart_E_Commerce_System.controller.MetricsController)")
    public void restControllers() {}

    /** GraphQL query/mutation/batch handlers (@Controller classes in the graphql package). */
    @Pointcut("within(com.example.Smart_E_Commerce_System.graphql..*) && @within(org.springframework.stereotype.Controller)")
    public void graphqlControllers() {}
}
