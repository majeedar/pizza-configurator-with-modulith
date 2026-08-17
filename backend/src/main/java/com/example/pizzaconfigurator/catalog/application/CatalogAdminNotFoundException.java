package com.example.pizzaconfigurator.catalog.application;

import java.util.UUID;

public class CatalogAdminNotFoundException extends RuntimeException {

    public CatalogAdminNotFoundException(String entityName, UUID id) {
        super("No " + entityName + " found for id " + id);
    }

    public CatalogAdminNotFoundException(String entityName, String code) {
        super("No " + entityName + " found for code " + code);
    }
}
