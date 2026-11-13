package com.pingmanager.ui.ecran;

import com.pingmanager.core.reglement.Formats;
import com.pingmanager.core.ServiceAgenda;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.DateFederale;
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
                    etiquette(e), e.estAutomatique(), passee));
        }
        return res;
    }

    /** Étiquette de « Mes tournois » : Tournoi, Date fédérale ou Date personnelle. */
    static String etiquette(EvenementAgenda e) {
        if (e.estAutomatique()) {
            return "Tournoi";
        }
        if (e.getOrigine() == OrigineEvenement.FEDERAL) {
            return "Date fédérale";
        }
        return "Date personnelle";
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
            if (origine == OrigineEvenement.FEDERAL) {
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
     * @param dansMonAgenda vrai pour une date fédérale déjà copiée dans l'agenda
     */
    record Element(String id, LocalDate date, LocalDate dateFin, String titre, String heure, String detail,
                   TypeDate type, boolean modifiable, boolean passee, boolean suggestion, String statut,
                   boolean aVerifier, boolean dansMonAgenda) {

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
                    !e.estAutomatique(), e.getDateFin().isBefore(aujourdhui), false, null, false, false));
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

    // ==================================================================
    // Calendrier fédéral (B045) : suggestions
    // ==================================================================

    /** Libellé de la pastille d'une date fédérale déjà copiée. */
    static final String DANS_MON_AGENDA = "Dans mon agenda";

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Les dates fédérales d'une catégorie, pour « Tout ajouter ». */
    record CategorieFederale(String nom, List<DateFederale> dates, int dejaAjoutees) {

        /** @return le nombre de dates pas encore dans l'agenda. */
        int restantes() {
            return dates.size() - dejaAjoutees;
        }
    }

    /**
     * Ajoute aux éléments de l'agenda les dates du calendrier fédéral : une date déjà
     * copiée garde sa ligne d'agenda, marquée « dans mon agenda » avec son statut ;
     * les autres deviennent des <b>suggestions</b> (rien n'est ajouté à l'agenda).
     */
    static List<Element> avecCalendrier(List<Element> elementsAgenda, List<DateFederale> federales,
                                        AgendaCompte agenda, LocalDate aujourdhui) {
        Map<String, DateFederale> parIdAgenda = new LinkedHashMap<>();
        for (DateFederale d : federales) {
            parIdAgenda.put(ServiceAgenda.identifiantFederal(d), d);
        }
        List<Element> res = new ArrayList<>();
        for (Element e : elementsAgenda) {
            DateFederale d = parIdAgenda.get(e.id());
            if (d != null) {
                res.add(new Element(e.id(), e.date(), e.dateFin(), e.titre(), e.heure(), e.detail(), e.type(),
                        e.modifiable(), e.passee(), false, d.getStatut().libelle(), d.estAVerifier(), true));
            } else {
                res.add(e);
            }
        }
        for (DateFederale d : federales) {
            if (ServiceAgenda.estDansAgenda(agenda, d)) {
                continue;
            }
            res.add(new Element(d.getId(), d.getDate(), d.getDateFin(), d.getTitre(), null, detailSuggestion(d),
                    TypeDate.FEDERAL, false, d.getDateFin().isBefore(aujourdhui), true, d.getStatut().libelle(),
                    d.estAVerifier(), false));
        }
        res.sort(ORDRE_ELEMENTS);
        return res;
    }

    /** « Suggestion · source fftt.com, relue le 13/11/2026 ». */
    static String detailSuggestion(DateFederale d) {
        return "Suggestion · source " + domaine(d.getSource()) + ", relue le " + JJ_MM_AAAA.format(d.getReluLe());
    }

    /** Domaine d'une adresse (« https://www.fftt.com/site/… » → « fftt.com »), sinon le texte tel quel. */
    static String domaine(String source) {
        String s = source.trim().replaceFirst("^[a-zA-Z]+://", "");
        int fin = s.indexOf('/');
        if (fin >= 0) {
            s = s.substring(0, fin);
        }
        return s.startsWith("www.") ? s.substring(4) : s;
    }

    /** Dates fédérales regroupées par catégorie (ordre de première apparition). */
    static List<CategorieFederale> categories(List<DateFederale> federales, AgendaCompte agenda) {
        Map<String, List<DateFederale>> parNom = new LinkedHashMap<>();
        for (DateFederale d : federales) {
            parNom.computeIfAbsent(d.getCategorie(), k -> new ArrayList<>()).add(d);
        }
        List<CategorieFederale> res = new ArrayList<>();
        for (Map.Entry<String, List<DateFederale>> c : parNom.entrySet()) {
            int deja = 0;
            for (DateFederale d : c.getValue()) {
                if (ServiceAgenda.estDansAgenda(agenda, d)) {
                    deja++;
                }
            }
            res.add(new CategorieFederale(c.getKey(), List.copyOf(c.getValue()), deja));
        }
        return res;
    }

    /** « 3 dates, 1 dans mon agenda, 2 à vérifier ». */
    static String resumeCategorie(CategorieFederale c) {
        int aVerifier = 0;
        for (DateFederale d : c.dates()) {
            if (d.estAVerifier()) {
                aVerifier++;
            }
        }
        StringBuilder sb = new StringBuilder(pluriel(c.dates().size(), "date", "dates"));
        if (c.dejaAjoutees() > 0) {
            sb.append(", ").append(c.dejaAjoutees()).append(" dans mon agenda");
        }
        if (aVerifier > 0) {
            sb.append(", ").append(aVerifier).append(" à vérifier");
        }
        return sb.toString();
    }

    /** Question de confirmation de « Tout ajouter ». */
    static String texteConfirmationToutAjouter(CategorieFederale c) {
        return "Ajouter à votre agenda " + pluriel(c.restantes(), "date", "dates") + " « " + c.nom() + " » ? "
                + "Ce sont des repères : vérifiez les dates marquées « à vérifier » auprès de la fédération, "
                + "de votre ligue ou de votre comité.";
    }

    /** Message après « Tout ajouter ». */
    static String texteApresToutAjouter(int ajoutees) {
        if (ajoutees == 0) {
            return "Ces dates étaient déjà toutes dans votre agenda.";
        }
        return pluriel(ajoutees, "date ajoutée", "dates ajoutées") + " à votre agenda.";
    }

    /** Titre du bloc du calendrier fédéral. */
    static String titreCalendrier(List<DateFederale> federales) {
        java.util.TreeSet<String> saisons = new java.util.TreeSet<>();
        for (DateFederale d : federales) {
            saisons.add(Formats.saison(d.getDate()));
        }
        return "Calendrier fédéral" + (saisons.isEmpty() ? "" : " " + String.join(", ", saisons));
    }

    /** Explication sous le titre du bloc du calendrier fédéral. */
    static String aideCalendrier() {
        return "Dates repères de la fédération, proposées en suggestions : rien n'est ajouté sans vous. "
                + "Une date « à vérifier » n'a pas encore été confirmée sur une source officielle.";
    }

    private static String pluriel(int n, String un, String plusieurs) {
        return n + " " + (n > 1 ? plusieurs : un);
    }
}
