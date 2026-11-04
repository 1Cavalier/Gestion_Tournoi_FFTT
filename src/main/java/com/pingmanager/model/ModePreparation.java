package com.pingmanager.model;

/**
 * Comment le club prépare son tournoi dans PingManager, choisi juste après la création du
 * brouillon (choix imposé) :
 * <ul>
 *   <li>{@link #COMPLETE} : rédaction complète dans l'application (sections, règlement,
 *       homologation, publication, inscriptions en ligne) ;</li>
 *   <li>{@link #IMPORT} : tournoi préparé ailleurs (règlement, tableaux et inscriptions faits
 *       par le club) ; PingManager reçoit les tableaux et les joueurs par fichier, puis gère
 *       le tournoi comme les autres le jour J ;</li>
 *   <li>{@link #A_CHOISIR} : brouillon tout juste créé, choix pas encore fait.</li>
 * </ul>
 * Les fichiers antérieurs, sans ce champ, sont relus en préparation complète.
 */
public enum ModePreparation {
    A_CHOISIR("À choisir"),
    COMPLETE("Préparation complète"),
    IMPORT("Tournoi importé");

    private final String libelle;

    ModePreparation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
