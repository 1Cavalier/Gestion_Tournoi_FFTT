package com.pingmanager.ui.ecran;

import com.pingmanager.core.MotifsInscription;
import com.pingmanager.core.RemplissageTableaux;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.core.reglement.LibellesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Tableau;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Briques d'affichage du remplissage des tableaux, partagées par la vue
 * « Inscriptions » du club et la fiche d'un tournoi (liste des participants) :
 * jauge des places, libellés de tableau, listes d'inscrits (triés par points) et
 * liste d'attente.
 */
final class BlocsInscriptions {

    private BlocsInscriptions() {
    }

    /** « sam. 10 avr. · 8h30 · ≤ 600 pts ». */
    static String sousTitreTableau(Tableau t) {
        StringBuilder sb = new StringBuilder();
        if (t.getHoraires() != null && t.getHoraires().getDebutMatchs() != null) {
            sb.append(Formats.jourCourt(t.getHoraires().getJour())).append(" · ")
                    .append(Formats.heure(t.getHoraires().getDebutMatchs().toLocalTime())).append(" · ");
        }
        sb.append(LibellesTableau.points(t.getConfiguration()));
        String pub = LibellesTableau.publicVise(t.getConfiguration());
        if (!"Mixte".equals(pub)) {
            sb.append(" · ").append(pub);
        }
        return sb.toString();
    }

    /** Période sans article : « Samedi 5 décembre 2026 », « Samedi 10 et dimanche 11 avril 2027 ». */
    static String periode(com.pingmanager.model.Tournoi t) {
        String p = Formats.periode(com.pingmanager.core.CalendrierTournoi.debut(t), com.pingmanager.core.CalendrierTournoi.fin(t));
        if (p == null) {
            return "Dates à préciser";
        }
        return Formats.capitale(p.replaceFirst("^(le|les) ", ""));
    }

    /** « sam. 10 avr. · 8h30 », vide sans horaire. */
    static String horaire(Tableau t) {
        if (t.getHoraires() == null || t.getHoraires().getDebutMatchs() == null) {
            return "";
        }
        return Formats.jourCourt(t.getHoraires().getJour()) + " · "
                + Formats.heure(t.getHoraires().getDebutMatchs().toLocalTime());
    }

    /** Jauge des places : barre + « 48 / 60 places · 3 en attente ». */
    static VBox jauge(RemplissageTableaux.EtatTableau e) {
        ProgressBar barre = new ProgressBar(e.capacite() <= 0 ? 0 : e.taux());
        barre.getStyleClass().add("jauge");
        if (e.estComplet()) {
            barre.getStyleClass().add("jauge-complete");
        }
        barre.setMaxWidth(Double.MAX_VALUE);
        barre.setPrefHeight(10);
        Label l = Ui.discret(texteJauge(e));
        l.getStyleClass().add("jauge-texte");
        VBox v = new VBox(4, barre, l);
        v.setFillWidth(true);
        return v;
    }

    static String texteJauge(RemplissageTableaux.EtatTableau e) {
        String places = e.capacite() <= 0 ? e.inscrits().size() + " inscrits (places illimitées)"
                : e.inscrits().size() + " / " + e.capacite() + " places";
        if (e.estComplet()) {
            places += " · complet";
        }
        if (!e.attente().isEmpty() || e.estComplet()) {
            places += " · " + e.attente().size() + " en attente"
                    + (e.capaciteAttente() > 0 ? " (sur " + e.capaciteAttente() + ")" : "");
        }
        return places;
    }

    /**
     * État du paiement d'un participant : montant à régler (tableaux obtenus non payés),
     * « Réglé », ou « Liste d'attente — rien à régler » quand il n'a encore aucune place.
     */
    static Label pastillePaiement(com.pingmanager.model.Tournoi t, RemplissageTableaux.Participant p) {
        double du = RemplissageTableaux.montantARegler(t, p.joueur());
        if (du > 0) {
            return Ui.pastille(Formats.montant(du) + " à régler", Ui.ETAT_ATTENTION);
        }
        if (p.inscrit().isEmpty()) {
            return Ui.pastille("Liste d'attente — rien à régler", Ui.ETAT_RECOMMANDE);
        }
        return Ui.pastille("Réglé", Ui.ETAT_OK);
    }

    /** Places restantes en clair, pour le joueur : « 12 places », « Complet — liste d'attente ». */
    static String placesPourJoueur(RemplissageTableaux.EtatTableau e) {
        if (e.capacite() <= 0) {
            return "Places illimitées";
        }
        if (!e.estComplet()) {
            int r = e.placesRestantes();
            return r + (r > 1 ? " places restantes" : " place restante");
        }
        int att = e.capaciteAttente() - e.attente().size();
        return att > 0 ? "Complet — liste d'attente (" + att + " place" + (att > 1 ? "s" : "") + ")"
                : "Complet, liste d'attente pleine";
    }

    /**
     * Liste virtualisée des inscrits (rang, nom, club, points), puis un séparateur
     * et la liste d'attente numérotée. {@code licenceMiseEnAvant} : ligne surlignée
     * (le joueur connecté), ou {@code null}.
     */
    static ListView<Object> liste(RemplissageTableaux.EtatTableau e, String licenceMiseEnAvant) {
        ListView<Object> lv = new ListView<>();
        lv.getStyleClass().add("liste-joueurs");
        lv.getItems().addAll(e.inscrits());
        if (!e.attente().isEmpty()) {
            lv.getItems().add("Liste d'attente (" + e.attente().size() + ")");
            for (int i = 0; i < e.attente().size(); i++) {
                lv.getItems().add(new EnAttente(i + 1, e.attente().get(i)));
            }
        }
        if (lv.getItems().isEmpty()) {
            lv.setPlaceholder(Ui.discret("Aucun inscrit pour l'instant."));
        }
        List<Joueur> inscrits = e.inscrits();
        lv.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean vide) {
                super.updateItem(item, vide);
                getStyleClass().removeAll("ligne-moi", "ligne-separateur");
                if (vide || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setText(null);
                if (item instanceof String s) {
                    getStyleClass().add("ligne-separateur");
                    setGraphic(Ui.gras(s));
                    return;
                }
                int rang;
                Joueur j;
                boolean attente = item instanceof EnAttente;
                if (attente) {
                    rang = ((EnAttente) item).position();
                    j = ((EnAttente) item).joueur();
                } else {
                    j = (Joueur) item;
                    rang = inscrits.indexOf(j) + 1;
                }
                if (j.getNumeroLicence().equals(licenceMiseEnAvant)) {
                    getStyleClass().add("ligne-moi");
                }
                setGraphic(ligneJoueur(attente ? "att. " + rang : String.valueOf(rang), j));
            }
        });
        return lv;
    }

    /** Une ligne joueur : rang, nom (+ licence et catégorie), club, points. */
    static HBox ligneJoueur(String rang, Joueur j) {
        Label r = Ui.discret(rang);
        r.setMinWidth(44);
        Label nom = Ui.gras(j.getNom() + " " + j.getPrenom());
        // Joueur importé sans licence : identifiant interne masqué ; catégorie affichée si connue.
        String licence = j.getNumeroLicence().startsWith("IMP-") ? "sans licence" : j.getNumeroLicence();
        Label det = Ui.discret(licence + (j.getCategorie() == null ? "" : " · " + MotifsInscription.categorie(j.getCategorie())));
        VBox identite = new VBox(1, nom, det);
        identite.setMinWidth(170);
        Label club = Ui.discret(j.getClub());
        club.setWrapText(false);
        HBox.setHgrow(club, Priority.ALWAYS);
        club.setMaxWidth(Double.MAX_VALUE);
        Label pts = Ui.gras(j.getPoints() + " pts");
        pts.setMinWidth(Region.USE_PREF_SIZE);
        HBox h = new HBox(12, r, identite, club, pts);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Puce d'un tableau : nom court (lettre) ; en attente : ambre + position. */
    static Label puceTableau(Tableau t, Integer positionAttente) {
        String nom = t.getNom();
        int i = nom.indexOf(" – ");
        String court = i > 0 ? nom.substring(0, i) : nom;
        Label l = Ui.badge(positionAttente == null ? court : court + " · att. " + positionAttente);
        l.getStyleClass().add(positionAttente == null ? "puce-tableau" : "puce-attente");
        l.setTooltip(new javafx.scene.control.Tooltip(nom + " — " + sousTitreTableau(t)));
        return l;
    }

    /** Un joueur en liste d'attente et sa position. */
    record EnAttente(int position, Joueur joueur) {
    }

    /** Petite légende sous les puces de la vue par joueur. */
    static Node legendePuces() {
        Label ins = Ui.badge("A");
        ins.getStyleClass().add("puce-tableau");
        Label att = Ui.badge("A · att. 2");
        att.getStyleClass().add("puce-attente");
        return Ui.ligne(8, ins, Ui.discret("inscrit"), att, Ui.discret("en liste d'attente (position)"));
    }
}
