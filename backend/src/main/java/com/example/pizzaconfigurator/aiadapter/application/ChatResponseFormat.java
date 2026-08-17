package com.example.pizzaconfigurator.aiadapter.application;

record ChatResponseFormat(String type) {

    static ChatResponseFormat jsonObject() {
        return new ChatResponseFormat("json_object");
    }
}
