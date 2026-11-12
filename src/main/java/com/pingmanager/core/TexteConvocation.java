package com.pingmanager.core;

import com.pingmanager.model.Joueur;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Textes des convocations par SMS (B019), toujours ramenés à l'alphabet GSM
 * 7 bits et à <b>un seul SMS</b> (160 caractères) :
 * <ul>
 *   <li>« Hugo, vous etes attendu(e) table 4 contre Léa DURAND (TT Brunoy) - Tableau C, poule B. »</li>
 *   <li>« … - Tableau C, 8es de finale. » ; « … - Tableau C, match de classement (places 17 à 32). »</li>
 *   <li>« Hugo, vous arbitrez la partie table 4 : Léa DURAND contre Paul MARTIN - Tableau C, poule B. »</li>
 * </ul>
 * Message transactionnel : aucune publicité ni mention STOP. Si le texte est trop long, on
 * retire d'abord le club, puis on abrège les prénoms et le nom du tableau.
 */
public final class TexteConvocation {

    private TexteConvocation() {
    }

    /** Convocation d'un joueur à son match ; {@code contexte} nul : sans tableau ni tour. */
    public static String joueur(Joueur destinataire, Joueur adversaire, int table, ContexteConvocation contexte) {
        Objects.requireNonNull(destinataire, "destinataire");
        Objects.requireNonNull(adversaire, "adversaire");
        String debut = salutation(destinataire.getPrenom()) + "vous êtes attendu(e) table " + table + " contre ";
        String club = adversaire.getClub() == null || adversaire.getClub().isBlank()
                ? "" : " (" + adversaire.getClub().trim() + ")";
        List<String> candidats = new ArrayList<>();
        candidats.add(debut + nom(adversaire) + club + suite(contexte, false) + ".");
        candidats.add(debut + nom(adversaire) + suite(contexte, false) + ".");
        candidats.add(debut + nomCourt(adversaire) + suite(contexte, true) + ".");
        return premierQuiTient(candidats);
    }

    /** Convocation pour arbitrer une partie ; {@code contexte} nul : sans tableau ni tour. */
    public static String arbitre(String prenomArbitre, Joueur joueurA, Joueur joueurB, int table,
                                 ContexteConvocation contexte) {
        Objects.requireNonNull(joueurA, "joueurA");
        Objects.requireNonNull(joueurB, "joueurB");
        String debut = salutation(prenomArbitre) + "vous arbitrez la partie table " + table + " : ";
        List<String> candidats = new ArrayList<>();
        candidats.add(debut + nom(joueurA) + " contre " + nom(joueurB) + suite(contexte, false) + ".");
        candidats.add(debut + nomCourt(joueurA) + " contre " + nomCourt(joueurB) + suite(contexte, true)
                + ".");
        return premierQuiTient(candidats);
    }

    private static String premierQuiTient(List<String> candidats) {
        String dernier = "";
        for (String c : candidats) {
            dernier = TexteSms.versGsm7(c.replaceAll("\\s+", " "));
            if (TexteSms.longueur(dernier) <= TexteSms.LONGUEUR_GSM7) {
                return dernier;
            }
        }
        // Noms hors normes : coupe franche, le début (table et adversaire) reste lisible.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dernier.length() && TexteSms.longueur(sb.toString() + dernier.charAt(i)) <= 157; i++) {
            sb.append(dernier.charAt(i));
        }
        return sb + "...";
    }

    /** « - Tableau C, poule B », vide sans contexte connu. */
    private static String suite(ContexteConvocation contexte, boolean abrege) {
        if (contexte == null) {
            return "";
        }
        return " - " + (abrege ? contexte.libelle(court(contexte.tableau())) : contexte.libelle());
    }

    private static String salutation(String prenom) {
        return prenom == null || prenom.isBlank() ? "Bonjour, " : prenom.trim() + ", ";
    }

    private static String nom(Joueur j) {
        return j.estADeterminer() ? "à déterminer" : j.getNomComplet().trim();
    }

    /** « L. DURAND » : initiale du prénom et nom. */
    private static String nomCourt(Joueur j) {
        if (j.estADeterminer()) {
            return "à déterminer";
        }
        String p = j.getPrenom().trim();
        return (p.isEmpty() ? "" : p.charAt(0) + ". ") + j.getNom().trim();
    }

    private static String court(String tableau) {
        String t = tableau.trim();
        return t.length() <= 20 ? t : t.substring(0, 19).trim() + ".";
    }
}
