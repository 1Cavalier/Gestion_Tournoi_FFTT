package com.pingmanager.ui.ecran;

import com.pingmanager.core.ListePointage;
import com.pingmanager.core.ResultatActionPointage;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.ModeReglement;
import com.pingmanager.model.TypeOperationPointage;

import java.util.Locale;
import java.util.Map;

/** Textes de l'écran autonome « Pointage » (B041), sans JavaFX. */
final class PresentationPostePointage {

    static final String TITRE = "Pointage";
    static final String TOUS = "Tous les tableaux";
    /** Au-delà, on demande d'affiner la recherche (lisibilité et fluidité). */
    static final int LIMITE_LIGNES = 150;
    static final ModeReglement MODE_PAR_DEFAUT = ModeReglement.ESPECES;
    /** Invite du mode quand un paiement a été noté sans mode (avant B041). */
    static final String MODE_NON_NOTE = "Mode non noté";
    static final String AIDE = "Tapez un nom, un prénom ou un numéro de licence. Cochez « Pointé » quand le joueur "
            + "est là, « Payé » quand il a réglé (choisissez d'abord le mode). Échap vide la recherche.";
    static final String AUCUN_TOURNOI = "Aucun tournoi de votre club à pointer. Le pointage s'ouvre ici dès "
            + "qu'un tournoi est créé.";

    private PresentationPostePointage() {
    }

    /**
     * Identifiant du poste : le nom de l'ordinateur (variables d'environnement
     * {@code COMPUTERNAME} sous Windows, {@code HOSTNAME} ailleurs), sinon « poste-local ».
     */
    static String identifiantPoste(Map<String, String> environnement) {
        for (String cle : new String[] {"COMPUTERNAME", "HOSTNAME"}) {
            String v = environnement.get(cle);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "poste-local";
    }

    static String sousTitre(String nomTournoi, String poste) {
        return nomTournoi + " · poste « " + poste + " »";
    }

    /** « MARTIN Alice ». */
    static String nomAffiche(Joueur j) {
        return j.getNom().toUpperCase(Locale.FRENCH) + " " + j.getPrenom();
    }

    /** « TT Brunoy · 1 234 pts · lic. 9112345 ». */
    static String detail(Joueur j) {
        StringBuilder sb = new StringBuilder();
        if (j.getClub() != null && !j.getClub().isBlank()) {
            sb.append(j.getClub()).append(" · ");
        }
        sb.append(String.format(Locale.FRENCH, "%,d", j.getPoints()).replaceAll("\\h", " "))
                .append(" pts · lic. ").append(j.getNumeroLicence());
        return sb.toString();
    }

    /** « Réglé 8 € » ou « À régler 10 € », « Gratuit » si le tableau ne coûte rien. */
    static String montant(ListePointage.Engagement e) {
        if (e.montant() == 0) {
            return "Gratuit";
        }
        return (e.paye() ? "Réglé " : "À régler ") + Formats.montant(e.montant());
    }

    /** « Tableau B » pour un nom court ; un nom qui commence déjà par « Tableau » reste tel quel. */
    static String libelleTableau(String nom) {
        return nom.toLowerCase(Locale.FRENCH).startsWith("tableau") ? nom : "Tableau " + nom;
    }

    /** Valeur du sélecteur de tableau → nom du tableau, ou {@code null} pour tous. */
    static String nomTableau(String choix) {
        return choix == null || TOUS.equals(choix) ? null : choix;
    }

    /** « 12 joueurs » ; « 150 premiers joueurs sur 412 : affinez la recherche ». */
    static String compte(int affiches, int total) {
        if (total == 0) {
            return "Aucun joueur ne correspond.";
        }
        if (affiches < total) {
            return affiches + " premiers joueurs sur " + total + " : affinez la recherche.";
        }
        return total + (total > 1 ? " joueurs" : " joueur");
    }

    /** « 12 / 20 » : pointés (ou payés) sur inscrits. */
    static String proportion(int n, int inscrits) {
        return n + " / " + inscrits;
    }

    static String confirmationDesistement(Joueur j, String tableau) {
        return "Désinscrire " + nomAffiche(j) + " du tableau " + tableau + " ? Sa place est libérée et le premier "
                + "de la liste d'attente est repêché. Ce désistement ne se défait pas depuis ce poste.";
    }

    /** Message après une action : le refus expliqué, ou ce qui a été enregistré. */
    static String message(ResultatActionPointage r, Joueur j, String tableau, TypeOperationPointage type) {
        if (!r.estAcceptee()) {
            return r.getMotif().getMessage();
        }
        String qui = nomAffiche(j) + ", tableau " + tableau;
        String texte;
        if (type == TypeOperationPointage.POINTER) {
            texte = "Pointé : " + qui + ".";
        } else if (type == TypeOperationPointage.DEPOINTER) {
            texte = "Pointage retiré : " + qui + ".";
        } else if (type == TypeOperationPointage.MARQUER_PAYE) {
            ModeReglement m = r.getOperation() == null ? null : r.getOperation().getModeReglement();
            texte = "Payé" + (m == null ? "" : " (" + m.getLibelle().toLowerCase(Locale.FRENCH) + ")") + " : " + qui + ".";
        } else if (type == TypeOperationPointage.MARQUER_NON_PAYE) {
            texte = "Paiement annulé : " + qui + ".";
        } else {
            texte = "Désinscrit : " + qui + ".";
        }
        if (r.getRepeche() != null) {
            texte += " " + nomAffiche(r.getRepeche()) + " est repêché de la liste d'attente : à pointer et à faire payer.";
        }
        return texte;
    }
}
