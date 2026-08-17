package com.example.pizzaconfigurator.admin.application;

class QrCodeGenerationException extends RuntimeException {

    QrCodeGenerationException(Throwable cause) {
        super("Failed to generate QR code", cause);
    }
}
