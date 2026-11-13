package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;

import com.pingmanager.model.OrigineEvenement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Logique d'affichage de l'agenda du compte, sans JavaFX (testée) : lignes de
 * « Mes tournois » et, pour la page Agenda (B045), éléments typés, filtres,
 * regroupement par mois, grille du mois et prochain événement.
 */
final class PresentationAgenda {

    /** Une date de l'agenda, mise en forme. */
    record LigneAgenda(String id, String jour, String heure, String titre, String detail, String etiquette,
                       boolean automatique, boolean passee) {
    }

    private PresentationAgenda() {
    }

    /**
     * Lignes de l'agenda dans l'ordre chronologique. Une date est « passée » quand
     * son dernier jour est avant {@code aujourdhui} ; les dates passées ne sont
     * gardées que si {@code avecPassees}.
     */
    static List<LigneAgenda> lignes(AgendaCompte agenda, LocalDate aujourdhui, boolean avecPassees) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        List<LigneAgenda> res = new ArrayList<>();
        if (agenda == null) {
            return res;
        }
        for (EvenementAgenda e : agenda.evenements()) {
            boolean passee = e.getDateFin().isBefore(aujourdhui);
            if (passee && !avecPassees) {
                continue;
            }
            res.add(new LigneAgenda(e.getId(), jour(e), heure(e), e.getTitre(), detail(e),
                    e.estAutomatique() ? "Tournoi" : "Date personnelle", e.estAutomatique(), passee));
        }
        return res;
    }

    /** « samedi 17 avril 2027 », ou « du samedi 10 au dimanche 11 avril 2027 ». */
    static String jour(EvenementAgenda e) {
        if (!e.estSurPlusieursJours()) {
            return Formats.jourLong(e.getDate());
        }
        LocalDate d = e.getDate();
        LocalDate f = e.getDateFin();
        String debut = Formats.jourLong(d);
        if (d.getYear() == f.getYear()) {
            // retire l'année (et le mois s'il est commun) du premier jour
            String suffixe = d.getMonth() == f.getMonth()
                    ? " " + Formats.jourLong(f).replaceFirst("^\\S+ \\S+ ", "")
                    : " " + f.getYear();
            if (debut.endsWith(suffixe)) {
                debut = debut.substring(0, debut.length() - suffixe.length());
            }
        }
        return "du " + debut + " au " + Formats.jourLong(f);
    }

    /** « 18h30 », ou « Toute la journée ». */
    static String heure(EvenementAgenda e) {
        return e.getHeure() == null ? "Toute la journée" : Formats.heure(e.getHeure());
    }

    /** Lieu puis note (première ligne), séparés par « · » ; vide s'il n'y a rien. */
    static String detail(EvenementAgenda e) {
        List<String> morceaux = new ArrayList<>();
        if (e.getLieu() != null) {
            morceaux.add(e.getLieu());
        }
        if (e.getNote() != null) {
            morceaux.add(e.getNote().lines().findFirst().orElse(""));
        }
        return String.join(" · ", morceaux);
    }

    /** Texte d'état vide de l'agenda. */
    static String texteVide() {
        return "Aucune date à venir. Vos tournois s'ajoutent ici dès votre inscription.";
    }

    // ==================================================================
    // Page Agenda (B045)
    // ==================================================================

    /** Type d'une date, avec son libellé de pastille et sa classe de style (pas de switch). */
    enum TypeDate {
        PERSONNEL("Personnel", "agenda-type-personnel"),
        TOURNOI("Tournoi", "agenda-type-tournoi"),
        FEDERAL("Fédéral", "agenda-type-federal");

        private final String libelle;
        private final String classeCss;

        TypeDate(String libelle, String classeCss) {
            this.libelle = libelle;
            this.classeCss = classeCss;
        }

        String libelle() {
            return libelle;
        }

        String classeCss() {
            return classeCss;
        }

        /** Type d'une date de l'agenda d'après son origine. */
        static TypeDate de(OrigineEvenement origine) {
            if (origine == OrigineEvenement.TOURNOI) {
                return TOURNOI;
            }
            if (origine != null && "FEDERAL".equals(origine.name())) {
                return FEDERAL;
            }
            return PERSONNEL;
        }
    }

    /**
     * Un élément affiché par la page Agenda : une date de l'agenda, ou une
     * suggestion du calendrier fédéral ({@code suggestion} vrai, pas encore dans l'agenda).
     *
     * @param heure      « 18h30 », ou {@code null} pour une date sans heure
     * @param modifiable vrai pour une date que l'on peut modifier et retirer à la main
     * @param statut     pour une date fédérale : « Confirmée » ou « À vérifier » ; sinon {@code null}
     * @param aVerifier  vrai si le statut fédéral est « à vérifier » (pastille d'attention)
     */
    record Element(String id, LocalDate date, LocalDate dateFin, String titre, String heure, String detail,
                   TypeDate type, boolean modifiable, boolean passee, boolean suggestion, String statut,
                   boolean aVerifier) {

        /** @return vrai si l'élément couvre ce jour. */
        boolean couvre(LocalDate jour) {
            return !jour.isBefore(date) && !jour.isAfter(dateFin);
        }
    }

    /** Les dates d'un mois, pour la vue Liste. */
    record GroupeMois(YearMonth mois, String libelle, List<Element> elements) {
    }

    /** Une case de la grille du mois (vue Mois). */
    record CaseJour(LocalDate jour, boolean dansLeMois, boolean aujourdhui, List<TypeDate> types) {

        boolean occupe() {
            return !types.isEmpty();
        }
    }

    /** Ordre des éléments : premier jour, sans heure avant les heures, puis titre. */
    static final Comparator<Element> ORDRE_ELEMENTS = Comparator.comparing(Element::date)
            .thenComparing(Element::heure, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Element::titre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Element::id);

    private static final DateTimeFormatter MOIS_ANNEE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH);
    private static final String[] ENTETES_SEMAINE = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    /** Éléments de toutes les dates de l'agenda (passées comprises), dans l'ordre. */
    static List<Element> elements(AgendaCompte agenda, LocalDate aujourdhui) {
        Objects.requireNonNull(aujourdhui, "aujourdhui");
        List<Element> res = new ArrayList<>();
        if (agenda == null) {
            return res;
        }
        for (EvenementAgenda e : agenda.evenements()) {
            TypeDate type = TypeDate.de(e.getOrigine());
            res.add(new Element(e.getId(), e.getDate(), e.getDateFin(), e.getTitre(),
                    e.getHeure() == null ? null : Formats.heure(e.getHeure()), detail(e), type,
                    !e.estAutomatique(), e.getDateFin().isBefore(aujourdhui), false, null, false));
        }
        res.sort(ORDRE_ELEMENTS);
        return res;
    }

    /**
     * Garde les éléments des types choisis ; les dates passées seulement si
     * {@code avecPassees}. L'ordre est conservé.
     */
    static List<Element> filtrer(List<Element> elements, Set<TypeDate> types, boolean avecPassees) {
        List<Element> res = new ArrayList<>();
        for (Element e : elements) {
            if (types.contains(e.type()) && (avecPassees || !e.passee())) {
                res.add(e);
            }
        }
        return res;
    }

    /** Regroupe par mois du premier jour, mois croissants, éléments triés. */
    static List<GroupeMois> parMois(List<Element> elements) {
        List<Element> tries = new ArrayList<>(elements);
        tries.sort(ORDRE_ELEMENTS);
        Map<YearMonth, List<Element>> groupes = new LinkedHashMap<>();
        for (Element e : tries) {
            groupes.computeIfAbsent(YearMonth.from(e.date()), m -> new ArrayList<>()).add(e);
        }
        List<GroupeMois> res = new ArrayList<>();
        for (Map.Entry<YearMonth, List<Element>> g : groupes.entrySet()) {
            res.add(new GroupeMois(g.getKey(), libelleMois(g.getKey()), List.copyOf(g.getValue())));
        }
        return res;
    }

    /** « Novembre 2026 ». */
    static String libelleMois(YearMonth mois) {
        return Formats.capitale(MOIS_ANNEE.format(mois));
    }

    /** En-têtes des colonnes de la grille (la semaine commence le lundi). */
    static List<String> entetesSemaine() {
        return List.of(ENTETES_SEMAINE);
    }

    /**
     * Grille du mois : semaines complètes du lundi au dimanche (35 ou 42 cases),
     * chaque case avec les types des éléments qui couvrent ce jour (sans doublon,
     * dans l'ordre Personnel, Tournoi, Fédéral).
     */
    static List<CaseJour> grille(YearMonth mois, List<Element> elements, LocalDate aujourdhui) {
        LocalDate debut = mois.atDay(1);
        while (debut.getDayOfWeek() != DayOfWeek.MONDAY) {
            debut = debut.minusDays(1);
        }
        LocalDate fin = mois.atEndOfMonth();
        while (fin.getDayOfWeek() != DayOfWeek.SUNDAY) {
            fin = fin.plusDays(1);
        }
        List<CaseJour> res = new ArrayList<>();
        for (LocalDate j = debut; !j.isAfter(fin); j = j.plusDays(1)) {
            Set<TypeDate> presents = new LinkedHashSet<>();
            for (TypeDate t : TypeDate.values()) {
                for (Element e : elements) {
                    if (e.type() == t && e.couvre(j)) {
                        presents.add(t);
                        break;
                    }
                }
            }
            res.add(new CaseJour(j, YearMonth.from(j).equals(mois), j.equals(aujourdhui), List.copyOf(presents)));
        }
        return res;
    }

    /** Éléments qui couvrent ce jour, dans l'ordre. */
    static List<Element> duJour(List<Element> elements, LocalDate jour) {
        List<Element> res = new ArrayList<>();
        for (Element e : elements) {
            if (e.couvre(jour)) {
                res.add(e);
            }
        }
        res.sort(ORDRE_ELEMENTS);
        return res;
    }

    /** Prochaine date de l'agenda (en cours ou à venir), hors suggestions. */
    static Optional<Element> prochain(List<Element> elements, LocalDate aujourdhui) {
        Element meilleur = null;
        for (Element e : elements) {
            if (e.suggestion() || e.dateFin().isBefore(aujourdhui)) {
                continue;
            }
            if (meilleur == null || ORDRE_ELEMENTS.compare(e, meilleur) < 0) {
                meilleur = e;
            }
        }
        return Optional.ofNullable(meilleur);
    }

    /** « Prochaine date : National B, samedi 10 avril 2027 à 9h00 ». */
    static String texteProchain(Element e) {
        String jour = e.date().equals(e.dateFin()) ? Formats.jourLong(e.date())
                : Formats.periode(e.date(), e.dateFin());
        return "Prochaine date : " + e.titre() + ", " + jour + (e.heure() == null ? "" : " à " + e.heure());
    }

    /** Jour court de la vue Liste : « sam. 10 », ou « sam. 10 → dim. 11 ». */
    static String jourCourt(Element e) {
        String debut = JOUR_COURT.format(e.date());
        if (e.date().equals(e.dateFin())) {
            return debut;
        }
        return debut + " → " + JOUR_COURT.format(e.dateFin());
    }

    /** Détail d'une ligne : heure facultative puis lieu ou note, séparés par « · ». */
    static String detailLigne(Element e) {
        List<String> morceaux = new ArrayList<>();
        morceaux.add(jourCourt(e));
        if (e.heure() != null) {
            morceaux.add(e.heure());
        }
        if (e.detail() != null && !e.detail().isBlank()) {
            morceaux.add(e.detail());
        }
        return String.join(" · ", morceaux);
    }

    /** Titre du détail d'un jour (vue Mois) : « Samedi 10 avril 2027 ». */
    static String titreJour(LocalDate jour) {
        return Formats.capitale(Formats.jourLong(jour));
    }

    /**
     * Message d'état vide de la page Agenda.
     *
     * @param agendaVide vrai si l'agenda n'a aucune date (sinon : rien ne passe les filtres)
     */
    static String texteVidePage(boolean agendaVide) {
        if (agendaVide) {
            return "Votre agenda est vide. Ajoutez vos entraînements et rendez-vous ; "
                    + "les tournois où vous vous inscrivez s'y ajoutent tout seuls.";
        }
        return "Aucune date ne correspond aux filtres choisis. Cochez d'autres types, "
                + "ou affichez les dates passées.";
    }

    /** Texte du jour sans date (vue Mois). */
    static String texteJourLibre() {
        return "Rien de prévu ce jour-là.";
    }

    /** Texte de la page hors connexion. */
    static String texteHorsConnexion() {
        return "Connectez-vous pour tenir votre agenda : vos dates, et les tournois où vous êtes inscrit.";
    }
}
