package com.example.pizzaconfigurator.admin.web;

class InvalidAudienceException extends RuntimeException {

    InvalidAudienceException(String value) {
        super("Unknown audience '" + value + "' — expected 'customer' or 'kitchen'");
    }
}
