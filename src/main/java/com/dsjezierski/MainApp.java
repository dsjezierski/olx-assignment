package com.dsjezierski;

import io.vertx.core.Vertx;

public class MainApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OlxOfferVerticle());
    }
}
