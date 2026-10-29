package com.example.Smart_E_Commerce_System.graphql;

import com.example.Smart_E_Commerce_System.exception.BusinessRuleException;
import com.example.Smart_E_Commerce_System.exception.DuplicateResourceException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GraphQL counterpart of GlobalExceptionHandler. GraphQL always answers HTTP 200,
 * so errors go into the "errors" array with a classification and extra details.
 * The REST handler is unaffected: it only handles @ExceptionHandler methods.
 */
@ControllerAdvice
public class GraphQlErrorHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        return error(env, ErrorType.NOT_FOUND, ex.getMessage(), Map.of("code", "NOT_FOUND"));
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConflict(DuplicateResourceException ex, DataFetchingEnvironment env) {
        return error(env, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("code", "CONFLICT"));
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBusinessRule(BusinessRuleException ex, DataFetchingEnvironment env) {
        return error(env, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("code", "CONFLICT"));
    }

    /** Bean Validation failures from @Valid @Argument inputs: returns field -> message. */
    @GraphQlExceptionHandler
    public GraphQLError handleValidation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fieldErrors.putIfAbsent(lastNodeName(v.getPropertyPath()), v.getMessage());
        }
        return error(env, ErrorType.BAD_REQUEST, "Validation failed",
                Map.of("code", "VALIDATION_FAILED", "fieldErrors", fieldErrors));
    }

    /** Bad paging values or an unknown sortBy property. */
    @GraphQlExceptionHandler({IllegalArgumentException.class,
            org.springframework.data.core.PropertyReferenceException.class})
    public GraphQLError handleBadArgument(RuntimeException ex, DataFetchingEnvironment env) {
        return error(env, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("code", "BAD_ARGUMENT"));
    }

    @GraphQlExceptionHandler
    public GraphQLError handleIntegrity(DataIntegrityViolationException ex, DataFetchingEnvironment env) {
        return error(env, ErrorType.BAD_REQUEST,
                "Operation violates a data constraint (duplicate value or record still in use)",
                Map.of("code", "CONFLICT"));
    }

    private GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message,
                               Map<String, Object> extensions) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .extensions(extensions)
                .build();
    }

    private String lastNodeName(Path path) {
        String name = path.toString();
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }
}
