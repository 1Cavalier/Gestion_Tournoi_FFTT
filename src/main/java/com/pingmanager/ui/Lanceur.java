package com.pingmanager.ui;

import javafx.application.Application;

/**
 * Point d'entrée de l'application.
 *
 * <p>Cette classe <b>n'étend pas</b> {@link Application} volontairement : quand
 * la classe {@code main} étend {@code Application} et que l'application est
 * lancée en tant que jar classique, la JVM exige les composants JavaFX sur le
 * <i>module-path</i> et échoue avec « JavaFX runtime components are missing ».
 * En passant par un lanceur distinct, ce problème disparaît. Pour le
 * développement, {@code mvn javafx:run} fonctionne également.</p>
 */
public final class Lanceur {

    private Lanceur() {
    }

    public static void main(String[] args) {
        Application.launch(AppPingManager.class, args);
    }
}
