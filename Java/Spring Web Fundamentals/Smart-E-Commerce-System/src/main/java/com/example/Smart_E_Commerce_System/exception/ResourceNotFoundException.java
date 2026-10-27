package com.example.Smart_E_Commerce_System.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id " + id);
    }
}
