package com.lfms;

import javafx.application.Application;

/**
 * Application entry point.
 *
 * <p>This launcher intentionally does <strong>not</strong> extend {@link Application}.
 * Launching a class that extends {@code Application} directly (for example via
 * {@code java -jar}) triggers the well-known "JavaFX runtime components are missing"
 * error on modular JDKs. Delegating to {@link Application#launch(Class, String...)} from a
 * plain class avoids that, and works identically under {@code mvn javafx:run} and a
 * packaged jar.</p>
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
