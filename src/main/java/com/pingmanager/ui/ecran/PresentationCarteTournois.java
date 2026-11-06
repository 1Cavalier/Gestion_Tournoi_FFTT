package com.pingmanager.ui.ecran;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.StatutCarteTournoi;
import com.pingmanager.core.reglement.Formats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Logique d'affichage de la <b>carte des tournois</b> (onglet Tournois), sans JavaFX
 * (testée) : libellés des deux présentations, pastille des quatre statuts calculés par
 * {@link StatutCarteTournoi}, contenu de chaque carte de tournoi (avec « Voir plus »),
 * compteur, et regroupement <b>par jour</b> pour la présentation agenda.
 *
 * <p>Pas de {@code switch} sur les énumérations : chaînes de {@code if}.</p>
 */
final class PresentationCarteTournois {

    /** Les deux présentations au choix. */
    enum Vue { CARTE, AGENDA }

    static final String TITRE_ONGLET = "Carte des tournois";
    static final String VOIR_PLUS = "Voir plus";
    static final String AUCUN_RESULTAT = "Aucun tournoi ne correspond à ces filtres.";
    static final String JOUR_SANS_DATE = "Dates à préciser";

    /** Pastille d'un statut : texte, couleur (charte) et classe propre au statut (pour le designer). */
    record Pastille(String texte, StatutPastille couleur, String classeCss) {
    }

    /** Une carte de tournoi, prête à afficher (liste de la carte ou ligne de l'agenda). */
    record CarteTournoi(String id, String nom, String niveau, String dateLieu, String places,
                        Pastille pastille, String motif, String remarque, boolean grise) {
    }

    /** Un jour de l'agenda et ses tournois (date {@code null} : dates à préciser, en dernier). */
    record Jour(LocalDate date, String titre, boolean aujourdhui, List<CarteTournoi> tournois) {
    }

    private PresentationCarteTournois() {
    }

    /** Libellé du bouton de bascule d'une présentation. */
    static String libelleVue(Vue v) {
        if (v == Vue.AGENDA) {
            return "Agenda par jour";
        }
        return "Carte";
    }

    /** Pastille d'un verdict (le quatrième statut précise « Complet » ou « Pas à votre niveau »). */
    static Pastille pastille(StatutCarteTournoi.Verdict v) {
        StatutCarteTournoi.Statut s = v.statut();
        if (s == StatutCarteTournoi.Statut.EN_COURS) {
            return new Pastille("En cours", StatutPastille.INFO, "statut-carte-en-cours");
        }
        if (s == StatutCarteTournoi.Statut.NON_ACCESSIBLE) {
            return new Pastille("Non accessible", StatutPastille.ERREUR, "statut-carte-non-accessible");
        }
        if (s == StatutCarteTournoi.Statut.COMPLET_OU_HORS_NIVEAU) {
            String texte = StatutCarteTournoi.MOTIF_COMPLET.equals(v.motif()) ? "Complet" : "Pas à votre niveau";
            return new Pastille(texte, StatutPastille.NEUTRE, "statut-carte-complet");
        }
        return new Pastille("À venir", StatutPastille.SUCCES, "statut-carte-a-venir");
    }

    /** Légende des quatre statuts, dans l'ordre d'affichage. */
    static List<Pastille> legende() {
        return List.of(
                new Pastille("En cours", StatutPastille.INFO, "statut-carte-en-cours"),
                new Pastille("À venir", StatutPastille.SUCCES, "statut-carte-a-venir"),
                new Pastille("Non accessible", StatutPastille.ERREUR, "statut-carte-non-accessible"),
                new Pastille("Complet ou pas à votre niveau", StatutPastille.NEUTRE, "statut-carte-complet"));
    }

    /** Contenu d'une carte de tournoi. */
    static CarteTournoi carte(CatalogueTournois.Entree e) {
        StatutCarteTournoi.Verdict v = e.statut();
        String niveau = e.tournoi().getNiveau() == null ? "Niveau à préciser" : e.tournoi().getNiveau().getLibelle();
        return new CarteTournoi(e.id(), e.tournoi().getNom(), niveau, dateLieu(e), places(e), pastille(v),
                v.motif(), v.estBloquant() ? null : e.acces().remarque(), v.estBloquant());
    }

    /** Classe du motif : {@code motif-refus} (rouge) si non accessible, sinon {@code motif-complet}. */
    static String classeMotif(CarteTournoi c) {
        return "statut-carte-non-accessible".equals(c.pastille().classeCss()) ? "motif-refus" : "motif-complet";
    }

    /** « Samedi 14 novembre 2026 · Brunoy (91) ». */
    static String dateLieu(CatalogueTournois.Entree e) {
        String p = Formats.periode(CalendrierTournoi.debut(e.tournoi()), CalendrierTournoi.fin(e.tournoi()));
        String date = p == null ? JOUR_SANS_DATE : Formats.capitale(p.replaceFirst("^(le|les) ", ""));
        String ville = e.tournoi().getAdresse() == null ? null : e.tournoi().getAdresse().getVille();
        if (ville == null || ville.isBlank()) {
            return date;
        }
        return date + " · " + ville + (e.departement() == null ? "" : " (" + e.departement().code() + ")");
    }

    /** Places restantes et distance : « 12 places restantes sur 96 · ≈ 18 km ». */
    static String places(CatalogueTournois.Entree e) {
        String txt = e.placesRestantes() < 0 ? "Places illimitées"
                : e.placesRestantes() == 0 ? "Complet (liste d'attente)"
                : e.placesRestantes() + (e.placesRestantes() > 1 ? " places restantes" : " place restante")
                + " sur " + e.placesTotales();
        return txt + (e.distanceKm() == null ? "" : " · ≈ " + Math.round(e.distanceKm()) + " km");
    }

    /** « 12 tournois · 3 non accessibles » (non accessibles = deux statuts bloquants). */
    static String compteur(List<CatalogueTournois.Entree> entrees) {
        int n = entrees.size();
        long bloques = entrees.stream().filter(e -> e.statut().estBloquant()).count();
        return n + (n > 1 ? " tournois" : " tournoi")
                + (bloques > 0 ? " · " + bloques + " non accessible" + (bloques > 1 ? "s" : "") : "");
    }

    /**
     * Agenda par jour : chaque tournoi apparaît à chacun de ses jours (à partir
     * d'aujourd'hui pour un tournoi déjà commencé) ; jours dans l'ordre, tournois dans
     * l'ordre reçu ; les tournois sans date sont regroupés à la fin.
     */
    static List<Jour> agenda(List<CatalogueTournois.Entree> entrees, LocalDate aujourdhui) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        Map<LocalDate, List<CarteTournoi>> parJour = new TreeMap<>();
        List<CarteTournoi> sansDate = new ArrayList<>();
        for (CatalogueTournois.Entree e : entrees) {
            LocalDate debut = CalendrierTournoi.debut(e.tournoi());
            if (debut == null) {
                sansDate.add(carte(e));
                continue;
            }
            LocalDate fin = CalendrierTournoi.fin(e.tournoi());
            if (fin == null || fin.isBefore(debut)) {
                fin = debut;
            }
            LocalDate d = debut.isBefore(aujourdhui) ? aujourdhui : debut;
            CarteTournoi c = carte(e);
            for (; !d.isAfter(fin); d = d.plusDays(1)) {
                parJour.computeIfAbsent(d, x -> new ArrayList<>()).add(c);
            }
        }
        List<Jour> res = new ArrayList<>();
        for (Map.Entry<LocalDate, List<CarteTournoi>> j : parJour.entrySet()) {
            res.add(new Jour(j.getKey(), titreJour(j.getKey(), aujourdhui), j.getKey().equals(aujourdhui),
                    List.copyOf(j.getValue())));
        }
        if (!sansDate.isEmpty()) {
            res.add(new Jour(null, JOUR_SANS_DATE, false, List.copyOf(sansDate)));
        }
        return res;
    }

    /** « Aujourd'hui · jeudi 5 novembre 2026 », « Demain · … », sinon « Samedi 14 novembre 2026 ». */
    static String titreJour(LocalDate d, LocalDate aujourdhui) {
        String jour = Formats.jourLong(d);
        if (d.equals(aujourdhui)) {
            return "Aujourd'hui · " + jour;
        }
        if (d.equals(aujourdhui.plusDays(1))) {
            return "Demain · " + jour;
        }
        return Formats.capitale(jour);
    }
}
