package com.pingmanager.model;

/**
 * Façon d'annoncer une rencontre aux joueurs quand le juge-arbitre l'envoie sur
 * une table (cahier des charges v0, section 7) : rien, SMS, appel vocal
 * automatique, ou les deux.
 *
 * <p>Le choix est mémorisé par tournoi ({@link Tournoi#getModeAnnonce()}) et
 * reste modifiable ; sans choix mémorisé, l'écran le demande à chaque
 * lancement. Les envois réels (SMS, appel) viendront avec leurs propres tâches :
 * en attendant, l'annonce est affichée à l'écran.</p>
 *
 * <p>Pas de {@code switch} sur cette énumération : le libellé est porté par la
 * constante.</p>
 */
public enum ModeAnnonce {

    /** Aucune annonce : le juge-arbitre appelle les joueurs lui-même. */
    AUCUNE("Rien", false, false),
    /** SMS envoyé aux deux joueurs. */
    SMS("SMS", true, false),
    /** Appel automatique (voix) aux deux joueurs. */
    APPEL("Appel vocal", false, true),
    /** SMS et appel vocal. */
    SMS_ET_APPEL("SMS et appel vocal", true, true);

    private final String libelle;
    private final boolean sms;
    private final boolean appel;

    ModeAnnonce(String libelle, boolean sms, boolean appel) {
        this.libelle = libelle;
        this.sms = sms;
        this.appel = appel;
    }

    /** Libellé court : « Rien », « SMS », « Appel vocal », « SMS et appel vocal ». */
    public String getLibelle() {
        return libelle;
    }

    /** Un SMS part-il avec ce mode ? */
    public boolean avecSms() {
        return sms;
    }

    /** Un appel vocal part-il avec ce mode ? */
    public boolean avecAppel() {
        return appel;
    }
}
