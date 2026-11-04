package com.pingmanager.core.reglement;

import com.pingmanager.core.ServiceCycleTournoi;
import com.pingmanager.core.reglement.DocumentReglement.Article;
import com.pingmanager.core.reglement.DocumentReglement.Bloc;
import com.pingmanager.core.reglement.DocumentReglement.Grille;
import com.pingmanager.core.reglement.DocumentReglement.Liste;
import com.pingmanager.core.reglement.DocumentReglement.Paragraphe;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.ArticleOptionnel;
import com.pingmanager.model.ConditionsMaintien;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.QuotasTableaux;
import com.pingmanager.model.AffectationArbitre;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.CouleurBalles;
import com.pingmanager.model.EchelonHomologation;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.OptionsReglement;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.RegleTableauxParJour;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeRecompense;
import com.pingmanager.model.format.FormatTournoi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Construit le règlement du tournoi à partir des informations saisies dans les
 * autres sections. Le texte <b>s'adapte</b> aux données : une phrase n'apparaît
 * que si elle s'applique (liste d'attente, inscriptions sur place, balles des
 * joueurs, tableau dames supplémentaire…). Ce qui manque est laissé en clair
 * sous la forme {@code [à compléter : …]} et repris dans une check-list finale
 * « à faire avant le dépôt de la demande d'homologation ».
 *
 * <p>Les articles cités sont ceux des règlements sportifs FFTT 2026-2027
 * (IX.101 à IX.110) et des règles du jeu (2.x, 3.x).</p>
 */
public final class GenerateurReglement {

    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH);

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    // État d'une génération
    private Tournoi t;
    private OptionsReglement o;
    private final List<Article> articles = new ArrayList<>();
    private final Set<String> aCompleter = new LinkedHashSet<>();
    private int numero;
    private int numeroArticleTableaux;

    /** Génère le règlement du tournoi avec ses options enregistrées. */
    public synchronized DocumentReglement generer(Tournoi tournoi) {
        return generer(tournoi, tournoi.getOptionsReglement());
    }

    /**
     * Génère le règlement avec des options données (par exemple en cours de
     * saisie, pas encore enregistrées).
     */
    public synchronized DocumentReglement generer(Tournoi tournoi, OptionsReglement options) {
        this.t = Objects.requireNonNull(tournoi, "tournoi");
        this.o = options == null ? OptionsReglement.parDefaut() : options;
        articles.clear();
        aCompleter.clear();
        numero = 0;

        organisation();
        homologation();
        participants();
        tableaux();
        nombreDeTableaux(t.getQuotasTableaux());
        deroulement();
        inscriptions();
        droitsEngagement();
        dotations();
        pointage();
        jugeArbitrage();
        materiel();
        tenue();
        resultats();
        responsabilite();
        articlesOptionnels();
        acceptation();
        contact();
        resoudreRenvois();

        return new DocumentReglement(titre(), sousTitre(), List.copyOf(articles), checkList());
    }

    // ==================================================================
    // En-tête
    // ==================================================================

    private String titre() {
        String nom = t.getNom() == null || t.getNom().isBlank() ? "du tournoi" : t.getNom();
        return "Règlement — " + nom;
    }

    private String sousTitre() {
        String p = Formats.periode(t.getDateDebut(), t.getDateFin());
        String quand = p == null ? "Dates à compléter" : Formats.capitale(p.replaceFirst("^(le|les|du) ", ""));
        AdresseTournoi a = t.getAdresse();
        return a != null && a.getVille() != null ? quand + ", " + a.getVille() : quand;
    }

    // ==================================================================
    // Articles
    // ==================================================================

    private void organisation() {
        String club = t.getClubOrganisateur() == null ? trou("club organisateur") : t.getClubOrganisateur();
        String affil = t.getNumeroAffiliationClub() == null ? "" : " (n° d'affiliation " + t.getNumeroAffiliationClub() + ")";
        String quand = Formats.periode(t.getDateDebut(), t.getDateFin());
        String categorie = t.getNiveau() == null ? "un tournoi " + trou("catégorie du tournoi")
                : "un tournoi " + t.getNiveau().getLibelleMinuscule();
        StringBuilder sb = new StringBuilder();
        sb.append(club).append(affil).append(" organise ")
                .append(quand == null ? trou("dates du tournoi") : quand)
                .append(' ').append(categorie)
                .append(t.getNom() != null ? " intitulé « " + t.getNom() + " »" : "")
                .append(", ").append(lieu()).append('.');
        double dotation = dotationTotale();
        if (dotation > 0) {
            sb.append(" Le tournoi est doté de ").append(Formats.montant(dotation)).append(" en espèces")
                    .append(aDesLots() ? ", ainsi que de lots" : "").append('.');
        }
        article("Organisation", p(sb.toString()));
    }

    private String lieu() {
        AdresseTournoi a = t.getAdresse();
        if (a == null || !a.estComplete()) {
            return "à " + trou("adresse de la salle (voie, code postal, ville)");
        }
        String salle = salle();
        return (salle != null ? "à la salle « " + salle + " », " : "au ") + a.getVoie() + ", "
                + a.getCodePostal() + " " + a.getVille();
    }

    private String salle() {
        InfosReglement ir = t.getInfosReglement();
        return ir == null ? null : ir.getSalleNom();
    }

    private void homologation() {
        EchelonHomologation e = t.getEchelonHomologation();
        String par = e == null ? trou("catégorie du tournoi (elle fixe l'échelon d'homologation)")
                : (e == EchelonHomologation.FFTT ? "la FFTT" : "la ligue régionale");
        String num = t.getNumeroHomologation() == null || t.getNumeroHomologation().isBlank()
                ? trou("numéro d'homologation, délivré après la demande") : t.getNumeroHomologation();
        article("Homologation", p("Le tournoi est homologué par " + par + " sous le numéro " + num
                + ". Il se déroule selon les règlements sportifs de la FFTT et le présent règlement."));
    }

    private void participants() {
        List<Bloc> blocs = new ArrayList<>();
        NiveauTournoi n = t.getNiveau();
        String ouvert;
        if (n == null) {
            ouvert = "Le tournoi est ouvert aux licenciés FFTT " + trou("public admis, selon la catégorie du tournoi") + ".";
        } else if (n == NiveauTournoi.INTERNATIONAL) {
            ouvert = "Le tournoi est ouvert aux licenciés FFTT et aux titulaires d'un pass'tournoi"
                    + (t.accepteJoueursEtrangers() ? ", ainsi qu'aux licenciés des fédérations étrangères affiliées à l'ITTF" : "")
                    + " (IX.102).";
        } else if (n == NiveauTournoi.REGIONAL) {
            ouvert = "Le tournoi est ouvert aux licenciés FFTT des associations de la région du club organisateur "
                    + "et aux titulaires d'un pass'tournoi (IX.102).";
        } else if (n == NiveauTournoi.DEPARTEMENTAL) {
            ouvert = "Le tournoi est ouvert aux licenciés FFTT des associations du département du club organisateur "
                    + "et aux titulaires d'un pass'tournoi (IX.102).";
        } else {
            ouvert = "Le tournoi est ouvert aux licenciés FFTT et aux titulaires d'un pass'tournoi (IX.102).";
        }
        if (n == NiveauTournoi.PROMOTIONNEL) {
            ouvert += " Les résultats ne sont pas pris en compte pour les classements individuels.";
        }
        blocs.add(p(ouvert));

        StringBuilder sb = new StringBuilder();
        if (t.accepteLicencePromotionnelle()) {
            sb.append(n == NiveauTournoi.PROMOTIONNEL
                    ? "Les titulaires d'une licence promotionnelle sont admis. "
                    : "Les titulaires d'une licence promotionnelle sont admis dans les tableaux qui leur sont réservés (IX.101). ");
        } else if (n != NiveauTournoi.PROMOTIONNEL) {
            sb.append("La licence promotionnelle n'est pas admise. ");
        }
        // Catégories d'âge non admises (choix du club).
        if (!t.getCategoriesExclues().isEmpty()) {
            List<String> cats = t.getCategoriesExclues().stream()
                    .map(c -> "les " + c.getLibelle().toLowerCase(Locale.FRENCH)).toList();
            String liste = joindre(cats);
            sb.append(Character.toUpperCase(liste.charAt(0))).append(liste.substring(1))
                    .append(" (garçons et filles) ne sont pas admis. ");
        }
        String saison = t.getDateDebut() == null ? trou("saison") : Formats.saison(t.getDateDebut());
        sb.append("Au pointage, chaque joueur présente son attestation de licence de la saison ").append(saison)
                .append(" (papier ou application FFTT).");
        String points = t.getPhasePoints() == null
                ? trou("phase de référence des points (section Général)")
                : "de la " + t.getPhasePoints().getLibelle() + " " + saison;
        sb.append(" Les points retenus pour les inscriptions sont ceux ").append(points).append('.');
        blocs.add(p(sb.toString()));
        article("Participants", blocs);
    }

    private void tableaux() {
        List<Tableau> liste = tableauxTries();
        if (liste.isEmpty()) {
            numeroArticleTableaux = article("Tableaux", p(trou("tableaux du tournoi")));
            return;
        }
        List<List<String>> lignes = new ArrayList<>();
        for (Tableau tab : liste) {
            HorairesTableau h = tab.getHoraires();
            ConfigurationTableau c = tab.getConfiguration();
            lignes.add(List.of(
                    tab.getNom(),
                    h == null || h.getJour() == null ? trou("jour") : Formats.capitale(Formats.jourLong(h.getJour()).replaceFirst(" \\d{4}$", "")),
                    h == null || h.getFinPointage() == null ? "—" : Formats.heure(h.getFinPointage().toLocalTime()),
                    h == null || h.getDebutMatchs() == null ? trou("heure") : Formats.heure(h.getDebutMatchs().toLocalTime()),
                    LibellesTableau.points(c),
                    joueursAdmis(c),
                    c.getPlacesMax() > 0 ? String.valueOf(c.getPlacesMax()) : trou("places"),
                    tarifTableau(tarif(tab))));
        }
        boolean enLigne = liste.stream().anyMatch(x -> tarif(x).isEnLigneProposee());
        String note = "* " + (enLigne ? "Tarif de l'inscription en ligne" : "Tarif de l'inscription") + " ; voir les "
                + "modalités à l'article " + RENVOI_DROITS + " « Droits d'engagement ». Les horaires sont indicatifs ; "
                + "le juge-arbitre peut les adapter pour la bonne marche de la compétition.";
        ConditionsMaintien cm = t.getConditionsMaintien();
        if (cm.getMinimumInscrits() > 0) {
            note += " Un tableau comptant moins de " + cm.getMinimumInscrits() + " inscrits"
                    + (cm.getDateDecision() != null ? " le " + Formats.jourLong(cm.getDateDecision()) : "")
                    + " pourra être annulé ou regroupé ; les droits d'engagement sont alors remboursés.";
        }
        boolean auMoinsUnFinPointage = liste.stream()
                .anyMatch(x -> x.getHoraires() != null && x.getHoraires().getFinPointage() != null);
        if (!auMoinsUnFinPointage) {
            aCompleter.add("Indiquer l'heure de fin du pointage de chaque tableau");
        }
        Grille g = new Grille(List.of("Tableau", "Jour", "Fin du pointage", "Début", "Points", "Joueurs admis", "Places",
                "Tarif d'inscription*"),
                lignes, note);
        String intro = "Le tournoi comprend " + liste.size() + (liste.size() > 1 ? " tableaux :" : " tableau :");
        numeroArticleTableaux = article("Tableaux", p(intro), g);
    }

    /** Repère remplacé, une fois tous les articles numérotés, par le numéro de l'article « Droits d'engagement ». */
    private static final String RENVOI_DROITS = "\u0000DROITS\u0000";

    /** Montant affiché dans l'article 4 : le tarif en ligne, sinon le tarif sur place. */
    private static String tarifTableau(TarifInscription tr) {
        if (tr.isEnLigneProposee()) {
            return tr.getMontantEnLigne() == 0 ? "Gratuit" : Formats.montant(tr.getMontantEnLigne());
        }
        return tr.getMontantSurPlace() == 0 ? "Gratuit" : Formats.montant(tr.getMontantSurPlace()) + " (sur place)";
    }

    /** Remplace le repère de renvoi par le numéro réel de l'article « Droits d'engagement ». */
    private void resoudreRenvois() {
        String numeroDroits = articles.stream().filter(a -> a.titre().equals("Droits d'engagement"))
                .map(a -> String.valueOf(a.numero())).findFirst().orElse("");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            List<Bloc> blocs = new ArrayList<>();
            boolean change = false;
            for (Bloc b : a.blocs()) {
                if (b instanceof Grille g && g.note() != null && g.note().contains(RENVOI_DROITS)) {
                    blocs.add(new Grille(g.entetes(), g.lignes(), g.note().replace(RENVOI_DROITS, numeroDroits)));
                    change = true;
                } else {
                    blocs.add(b);
                }
            }
            if (change) {
                articles.set(i, new Article(a.numero(), a.titre(), List.copyOf(blocs)));
            }
        }
    }

    /** Qui peut jouer (hors points, donnés dans leur propre colonne) : « Tous », « Dames », catégories. */
    private static String joueursAdmis(ConfigurationTableau c) {
        String sexe = c.getSexeRequis() == Sexe.FEMME ? "Dames"
                : c.getSexeRequis() == Sexe.HOMME ? "Messieurs" : "Tous";
        String cats = LibellesTableau.categories(c);
        return cats == null ? sexe : sexe + ", " + cats;
    }

    /**
     * Texte de l'article « Nombre de tableaux par joueur » pour des règles données
     * (par exemple en cours de saisie, pas encore enregistrées) : un paragraphe par
     * élément de la liste ; liste vide si aucune règle n'est définie.
     */
    public synchronized List<String> apercuNombreDeTableaux(Tournoi tournoi, QuotasTableaux regles) {
        this.t = Objects.requireNonNull(tournoi, "tournoi");
        this.o = tournoi.getOptionsReglement();
        articles.clear();
        aCompleter.clear();
        numero = 0;
        nombreDeTableaux(regles);
        List<String> res = new ArrayList<>();
        if (!articles.isEmpty()) {
            for (Bloc b : articles.get(0).blocs()) {
                if (b instanceof Liste l) {
                    l.elements().forEach(e -> res.add("• " + e));
                } else {
                    res.add(b.texteBrut());
                }
            }
        }
        return res;
    }

    private void nombreDeTableaux(QuotasTableaux q) {
        if (q == null || !q.aUneRegle()) {
            return;
        }
        // Tableau exigé pour le surplus des joueuses : seulement s'il existe encore.
        String bonus = q.getTableauBonusJoueuses();
        Tableau feminin = bonus == null ? null
                : t.getTableaux().stream().filter(x -> bonus.equals(x.getNom())).findFirst().orElse(null);
        if (bonus != null && feminin == null) {
            aCompleter.add("Vérifier les tableaux par jour : le tableau « " + bonus + " », exigé pour le tableau "
                    + "supplémentaire des joueuses, n'existe plus");
        }
        String b = feminin == null ? null : bonus;
        // Obligation de jouer le tableau des joueuses : renvoi « * » après son nom
        // dans la liste, et note en bas de l'article.
        boolean note = q.isTableauBonusObligatoire() && feminin != null;
        java.util.function.UnaryOperator<String> etoile = ph -> note && b != null
                ? ph.replace("« " + b + " »", "« " + b + " »*") : ph;
        List<Bloc> blocs = new ArrayList<>();
        LocalDate jourFeminin = feminin == null || feminin.getHoraires() == null ? null : feminin.getHoraires().getJour();
        List<String> simple = q.aLimite() ? quotasSimples(q, b, note, jourFeminin) : null;
        boolean obligationEcrite = false;
        if (simple != null) {
            // Cas courant : même limite pour tous ; tableau dames imposé ou en option pour les joueuses.
            blocs.add(p(String.join(" ", simple)));
            obligationEcrite = note;
        } else if (q.aLimite() && !q.varieSelonLeJour()) {
            blocs.add(p(etoile.apply(phraseQuota(q.getTousLesJours(), b)) + "."));
        } else if (q.aLimite()) {
            List<String> jours = new ArrayList<>();
            q.getParJour().forEach((jour, quota) -> {
                String ph = etoile.apply(phraseQuota(quota, b));
                jours.add(Formats.capitale(Formats.jourLong(jour).replaceFirst(" \\d{4}$", "")) + " : "
                        + Character.toLowerCase(ph.charAt(0)) + ph.substring(1) + ".");
            });
            blocs.add(p("Le nombre de tableaux auxquels on peut s'inscrire varie selon le jour :"));
            blocs.add(new Liste(jours));
        }
        if (q.isTableauxConsecutifsInterdits()) {
            blocs.add(p("Un joueur ou une joueuse ne peut pas s'inscrire à deux tableaux qui se suivent dans "
                    + "la journée."));
        }
        if (note && !obligationEcrite) {
            String quand = feminin.getHoraires() == null || feminin.getHoraires().getJour() == null ? "le jour où il est joué"
                    : "le " + Formats.jourLong(feminin.getHoraires().getJour()).replaceFirst(" \\d{4}$", "");
            boolean renvoi = blocs.stream().anyMatch(x -> x.texteBrut().contains("»*"));
            blocs.add(p((renvoi ? "* " : "") + Formats.capitale(quand) + ", toute joueuse inscrite à un tableau "
                    + "doit aussi participer au tableau « " + bonus + " »."));
        }
        article("Nombre de tableaux par joueur", blocs);
    }

    /**
     * Rédaction simple quand les joueurs et les joueuses ont la même limite chaque jour,
     * les joueuses pouvant avoir des tableaux en plus certains jours :
     * « Les joueurs et les joueuses peuvent s'inscrire à 2 tableaux maximum par jour.
     * Le samedi 17 avril, les joueuses peuvent s'inscrire à un troisième tableau au choix,
     * à condition d'avoir sélectionné le tableau « Toutes séries dames ». »
     *
     * @return les phrases, ou {@code null} si les limites ne s'y prêtent pas
     */
    private static List<String> quotasSimples(QuotasTableaux q, String bonus, boolean obligatoire,
                                              LocalDate jourFeminin) {
        java.util.Map<LocalDate, QuotasTableaux.Quota> jours = new java.util.LinkedHashMap<>();
        if (q.varieSelonLeJour()) {
            jours.putAll(q.getParJour());
        } else {
            jours.put(null, q.getTousLesJours());
        }
        Integer h = null;
        for (QuotasTableaux.Quota quota : jours.values()) {
            if (quota.joueurs() == null || quota.joueuses() == null || quota.joueuses() < quota.joueurs()
                    || (h != null && !h.equals(quota.joueurs()))) {
                return null;
            }
            h = quota.joueurs();
        }
        // L'obligation doit pouvoir s'écrire sur le jour du tableau dames : sinon, rédaction
        // détaillée (avec la note) pour ne pas la perdre.
        if (h == null || (obligatoire && (jourFeminin == null
                || (q.varieSelonLeJour() && !jours.containsKey(jourFeminin))))) {
            return null;
        }
        List<String> res = new ArrayList<>();
        res.add("Chaque joueur et chaque joueuse peut s'inscrire à " + tableaux(h) + " maximum par jour.");
        for (java.util.Map.Entry<LocalDate, QuotasTableaux.Quota> e : jours.entrySet()) {
            int plus = e.getValue().joueuses() - h;
            LocalDate jour = e.getKey() != null ? e.getKey() : jourFeminin;
            String quand = jour == null ? "" : "Le " + Formats.jourLong(jour).replaceFirst(" \\d{4}$", "") + ", ";
            boolean jourDuTableauDames = obligatoire && jour != null && jour.equals(jourFeminin);
            if (jourDuTableauDames) {
                // Tableau dames imposé : il s'ajoute aux tableaux au choix (ou compte parmi eux).
                res.add(quand + "toute joueuse qui s'inscrit doit aussi participer au tableau « " + bonus + " »"
                        + (plus == 1 ? " : ce tableau imposé s'ajoute à ses " + tableaux(h) + " au choix, soit "
                        + tableaux(h + 1) + " au maximum."
                        : plus > 1 ? " ; elle peut s'inscrire jusqu'à " + tableaux(e.getValue().joueuses()) + " au total."
                        : ", qui compte parmi ses " + tableaux(h) + "."));
                continue;
            }
            if (plus <= 0) {
                continue;
            }
            String qui = bonus != null ? "les joueuses inscrites au tableau « " + bonus + " »" : "les joueuses";
            String quoi = plus == 1 ? "à un " + ordinal(h + 1) + " tableau de leur choix"
                    : "jusqu'à " + tableaux(e.getValue().joueuses()) + " au total";
            res.add(quand.isEmpty() ? Formats.capitale(qui) + " peuvent s'inscrire " + quoi + "."
                    : quand + qui + " peuvent s'inscrire " + quoi + ".");
        }
        return res;
    }

    private static String ordinal(int n) {
        return switch (n) {
            case 2 -> "deuxième";
            case 3 -> "troisième";
            case 4 -> "quatrième";
            case 5 -> "cinquième";
            default -> n + "e";
        };
    }

    /** « Chaque joueur peut s'inscrire à 2 tableaux par jour, chaque joueuse à 3 tableaux ». */
    private static String phraseQuota(QuotasTableaux.Quota q, String bonus) {
        Integer h = q.joueurs();
        Integer f = q.joueuses();
        if (bonus != null && h != null && f != null && f > h) {
            return "Chaque joueur peut s'inscrire à " + tableaux(h) + " par jour ; chaque joueuse à " + tableaux(f)
                    + ", à condition que l'un d'eux soit le tableau « " + bonus + " »";
        }
        if (h == null && f == null) {
            return "Pas de limite de tableaux";
        }
        if (h != null && h.equals(f)) {
            return "Chaque joueur et chaque joueuse peut s'inscrire à " + tableaux(h) + " par jour";
        }
        String joueurs = h == null ? "Les joueurs peuvent s'inscrire sans limite de tableaux"
                : "Chaque joueur peut s'inscrire à " + tableaux(h) + " par jour";
        String joueuses = f == null ? "les joueuses sans limite" : "chaque joueuse à " + tableaux(f);
        return joueurs + ", " + joueuses;
    }

    private static String tableaux(int n) {
        return n + (n > 1 ? " tableaux" : " tableau");
    }

    private void deroulement() {
        List<Bloc> blocs = new ArrayList<>();
        Map<String, FormatTournoi> formats = new LinkedHashMap<>();
        for (Tableau tab : t.getTableaux()) {
            FormatTournoi f = tab.getConfiguration().getFormat();
            if (f != null) {
                formats.putIfAbsent(f.identifiant(), f);
            }
        }
        if (formats.isEmpty()) {
            blocs.add(p(t.getTableaux().isEmpty() ? trou("format de compétition des tableaux")
                    : "Le format de chaque tableau est " + trou("format de compétition") + "."));
        } else {
            for (FormatTournoi f : formats.values()) {
                String qui = formats.size() == 1 ? "Chaque tableau commence" : "Les tableaux au format « " + f.nom() + " » commencent";
                StringBuilder sb = new StringBuilder(qui).append(" par des poules de ").append(f.taillePouleSouhaitee())
                        .append(" joueurs");
                if (f.formatPhaseFinale() == FormatPhaseFinale.ELIMINATION_DIRECTE) {
                    sb.append(" ; ").append(f.qualifiesParPoule() > 1 ? "les " + f.qualifiesParPoule() + " premiers"
                            : "le premier").append(" de chaque poule ").append(f.qualifiesParPoule() > 1 ? "sont qualifiés" : "est qualifié")
                            .append(" pour un tableau final à élimination directe");
                }
                sb.append(". Toutes les parties se jouent au meilleur des ").append(f.nombreManchesGagnantes() * 2 - 1)
                        .append(" manches de 11 points");
                if (f.nombreManchesGagnantesFinale() != f.nombreManchesGagnantes()) {
                    sb.append(", la finale au meilleur des ").append(f.nombreManchesGagnantesFinale() * 2 - 1).append(" manches");
                }
                sb.append('.');
                if (f.avecPetiteFinale()) {
                    sb.append(" Une petite finale désigne la troisième place.");
                }
                blocs.add(p(sb.toString()));
            }
        }
        List<AffectationArbitre> arbitres = t.getArbitres();
        if (arbitres.isEmpty()) {
            blocs.add(p("Les parties sont arbitrées par les joueurs : chaque perdant arbitre la partie suivante sur sa "
                    + "table, sauf décision contraire du juge-arbitre."));
        } else {
            blocs.add(p((arbitres.size() > 1 ? arbitres.size() + " arbitres officiels officient" : "Un arbitre officiel officie")
                    + " pendant le tournoi ; les autres parties sont arbitrées par les perdants, sauf décision contraire "
                    + "du juge-arbitre."));
        }
        blocs.add(p("Un joueur absent à l'appel de sa partie peut être déclaré forfait par le juge-arbitre."));
        article("Déroulement", blocs);
    }

    private void inscriptions() {
        InfosReglement ir = t.getInfosReglement();
        LocalDateTime ouv = ir == null ? null : ir.getOuvertureInscriptions();
        LocalDateTime clo = ir == null ? null : ir.getClotureInscriptions();
        boolean enLigne = t.getTableaux().stream().anyMatch(x -> tarif(x).isEnLigneProposee());
        boolean surPlace = t.getTableaux().stream().anyMatch(x -> tarif(x).isSurPlaceProposee());
        boolean attente = t.getTableaux().stream().anyMatch(x -> x.getConfiguration().getTailleMaxListeAttente() > 0);

        Paiement pay = t.getPaiement();
        String lienInscription = t.getSiteWeb() != null && !t.getSiteWeb().isBlank() ? t.getSiteWeb() : pay.getLien();
        StringBuilder sb = new StringBuilder("Les inscriptions ");
        if (enLigne || t.getTableaux().isEmpty()) {
            sb.append("se font en ligne sur ")
                    .append(lienInscription != null ? lienInscription : trou("adresse du site d'inscription")).append(", ");
        } else {
            sb.append("se font sur place uniquement, ");
        }
        sb.append("du ").append(ouv == null ? trou("date d'ouverture des inscriptions") : Formats.dateHeure(ouv))
                .append(" au ").append(clo == null ? trou("date de clôture des inscriptions") : Formats.dateHeure(clo))
                .append(". Elles sont retenues par ordre d'arrivée, dans la limite des places de chaque tableau.");
        if (attente) {
            sb.append(" Au-delà, une liste d'attente est ouverte : une place libérée est proposée au premier joueur de la liste.");
        }
        if (enLigne && surPlace) {
            sb.append(" Des inscriptions sur place restent possibles jusqu'à la fin du pointage, s'il reste des places.");
        }
        if (enLigne) {
            boolean memeLien = pay.estPret() && pay.getLien().equals(lienInscription);
            sb.append(" Le paiement se fait en ligne")
                    .append(memeLien ? " via " + pay.getMode().getLibelle() + ", au moment de l'inscription"
                            : pay.estPret() ? " via " + pay.getMode().getLibelle() + " : " + pay.getLien()
                            : " : " + trou("lien de paiement (section Complémentaire)"))
                    .append(". Une inscription n'est validée qu'à réception du paiement.");
        }
        article("Inscriptions", p(sb.toString()));
    }

    private void droitsEngagement() {
        List<Bloc> blocs = new ArrayList<>();
        List<Tableau> liste = tableauxTries();
        Map<String, List<String>> parTarif = new LinkedHashMap<>();
        for (Tableau tab : liste) {
            parTarif.computeIfAbsent(phraseTarif(tarif(tab)), k -> new ArrayList<>()).add(tab.getNom());
        }
        if (liste.isEmpty()) {
            blocs.add(p(trou("droits d'engagement par tableau")));
        } else if (parTarif.size() == 1) {
            blocs.add(p("L'inscription coûte " + parTarif.keySet().iterator().next() + " par tableau."));
        } else if (parTarif.size() == 2) {
            // Un tarif courant et une exception : une phrase suffit (le détail est dans l'article Tableaux).
            List<Map.Entry<String, List<String>>> groupes = new ArrayList<>(parTarif.entrySet());
            groupes.sort((x, y) -> Integer.compare(y.getValue().size(), x.getValue().size()));
            Map.Entry<String, List<String>> courant = groupes.get(0);
            Map.Entry<String, List<String>> autre = groupes.get(1);
            String noms = joindre(autre.getValue());
            blocs.add(p("L'inscription coûte " + courant.getKey() + " par tableau ; pour "
                    + (autre.getValue().size() > 1 ? "les tableaux " : "le tableau ") + noms + ", elle coûte "
                    + (autre.getKey().matches("[0-9 ,]+ €") ? autre.getKey() + ", en ligne comme sur place"
                    : autre.getKey()) + "."));
        } else {
            List<List<String>> lignes = new ArrayList<>();
            for (Tableau tab : liste) {
                TarifInscription tr = tarif(tab);
                lignes.add(List.of(tab.getNom(),
                        tr.isEnLigneProposee() ? Formats.montant(tr.getMontantEnLigne()) : "—",
                        tr.isSurPlaceProposee() ? Formats.montant(tr.getMontantSurPlace()) : "—"));
            }
            blocs.add(p("Les droits d'engagement dépendent du tableau :"));
            blocs.add(new Grille(List.of("Tableau", "En ligne", "Sur place"), lignes, null));
        }
        blocs.add(p(forfaits()));
        article("Droits d'engagement", blocs);
    }

    /**
     * Remboursement selon le moment du forfait (pendant les inscriptions, après la
     * clôture, jour J). Une seule phrase quand la règle est la même aux trois moments.
     */
    private String forfaits() {
        PolitiqueForfaits pf = t.getPolitiqueForfaits();
        InfosReglement ir = t.getInfosReglement();
        LocalDateTime clo = ir == null ? null : ir.getClotureInscriptions();
        PolitiqueForfaits.Regle r1 = pf.pour(PolitiqueForfaits.Periode.PENDANT_INSCRIPTIONS);
        PolitiqueForfaits.Regle r2 = pf.pour(PolitiqueForfaits.Periode.APRES_CLOTURE);
        PolitiqueForfaits.Regle r3 = pf.pour(PolitiqueForfaits.Periode.JOUR_J);
        String frais = pf.isFraisEnLigneDeduits() ? " Les frais de paiement en ligne ne sont pas remboursés." : "";

        if (r1 != null && r1.equals(r2) && r1.equals(r3)) {
            String moments = "pendant la période d'inscription, après la clôture des inscriptions ou le jour du tournoi";
            if (r1.remboursement() == PolitiqueForfaits.Remboursement.AUCUN) {
                return "Que le forfait intervienne " + moments + ", aucun remboursement n'est effectué : toute "
                        + "inscription est définitive et n'ouvre droit à aucun remboursement.";
            }
            return "Que le forfait intervienne " + moments + ", il " + effet(r1) + "." + frais;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Un forfait signalé avant la clôture des inscriptions")
                .append(clo != null ? " (" + Formats.dateHeure(clo) + ")" : "")
                .append(' ').append(effetForfait(r1, PolitiqueForfaits.Periode.PENDANT_INSCRIPTIONS)).append(". ");
        sb.append("Un forfait signalé après la clôture et avant le tournoi ")
                .append(effetForfait(r2, PolitiqueForfaits.Periode.APRES_CLOTURE)).append(". ");
        sb.append("Un forfait le jour du tournoi, ou une absence au pointage, ")
                .append(effetForfait(r3, PolitiqueForfaits.Periode.JOUR_J)).append('.');
        return sb.append(frais).toString();
    }

    private String effetForfait(PolitiqueForfaits.Regle r, PolitiqueForfaits.Periode periode) {
        return r == null ? "donne lieu à " + trou("remboursement en cas de forfait, "
                + periode.getLibelle().toLowerCase(Locale.FRENCH)) : effet(r);
    }

    /** « n'est pas remboursé », « est remboursé intégralement », « est remboursé à hauteur de 50 % »… */
    private static String effet(PolitiqueForfaits.Regle r) {
        String base = r.remboursement() == PolitiqueForfaits.Remboursement.AUCUN ? "n'est pas remboursé"
                : r.remboursement() == PolitiqueForfaits.Remboursement.COMPLET ? "est remboursé intégralement"
                : "est remboursé à hauteur de " + r.pourcentage() + " %";
        return r.justificatif() ? base + ", sur présentation d'un justificatif (certificat médical…)" : base;
    }

    private static String phraseTarif(TarifInscription tr) {
        if (tr.isEnLigneProposee() && tr.isSurPlaceProposee()) {
            if (tr.getMontantEnLigne() == tr.getMontantSurPlace()) {
                return tr.getMontantEnLigne() == 0 ? "0 € (gratuit)" : Formats.montant(tr.getMontantEnLigne());
            }
            return Formats.montant(tr.getMontantEnLigne()) + " en ligne et " + Formats.montant(tr.getMontantSurPlace()) + " sur place";
        }
        return tr.isEnLigneProposee() ? Formats.montant(tr.getMontantEnLigne()) + " (en ligne uniquement)"
                : Formats.montant(tr.getMontantSurPlace()) + " (sur place uniquement)";
    }

    private void dotations() {
        List<Bloc> blocs = new ArrayList<>();
        // Grille dans l'ordre des classements (du plus faible au plus fort, toutes séries ensuite).
        List<Tableau> dotes = t.getTableaux().stream()
                .filter(x -> x.getConfiguration().getRecompenses() != null && !x.getConfiguration().getRecompenses().estVide())
                .sorted(java.util.Comparator.comparingLong(com.pingmanager.core.OrdonnancementTableaux::niveau)
                        .thenComparing(Tableau::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (dotes.isEmpty()) {
            blocs.add(p(trou("récompenses de chaque tableau (argent, lots, réductions)")));
            article("Dotations", blocs);
            return;
        }
        double total = dotationTotale();
        List<Tableau> prop = tableauxProportionnels();
        StringBuilder intro = new StringBuilder();
        if (total > 0) {
            intro.append("Le tournoi est doté de ").append(Formats.montant(total)).append(" en espèces");
            intro.append(aDesLots() ? ", ainsi que de lots" : "").append(" (IX.108).");
        } else if (!prop.isEmpty()) {
            intro.append("Les tableaux sont dotés d'une part des inscriptions").append(aDesLots() ? " et de lots" : "")
                    .append(" (IX.108).");
        } else {
            intro.append("Les tableaux sont dotés de lots (IX.108).");
        }
        intro.append(" Les récompenses sont réparties ainsi :");
        blocs.add(p(intro.toString()));

        // Colonnes : une par groupe de places rencontré (vainqueur, finaliste, ½ finalistes…).
        TreeMap<Integer, int[]> groupes = new TreeMap<>();
        for (Tableau tab : dotes) {
            for (GrilleRecompenses.Entree e : tab.getConfiguration().getRecompenses().getEntrees()) {
                groupes.putIfAbsent(e.getPlaceDebut() * 1000 + e.getPlaceFin(), new int[] {e.getPlaceDebut(), e.getPlaceFin()});
            }
        }
        List<String> entetes = new ArrayList<>();
        entetes.add("Tableau");
        entetes.add("Points");
        groupes.values().forEach(g -> entetes.add(libellePlaces(g[0], g[1])));
        entetes.add("Total");
        List<List<String>> lignes = new ArrayList<>();
        for (Tableau tab : dotes) {
            GrilleRecompenses gr = tab.getConfiguration().getRecompenses();
            List<String> l = new ArrayList<>();
            l.add(tab.getNom());
            l.add(LibellesTableau.points(tab.getConfiguration()));
            for (int[] g : groupes.values()) {
                String cellule = "—";
                for (GrilleRecompenses.Entree e : gr.getEntrees()) {
                    if (e.getPlaceDebut() == g[0] && e.getPlaceFin() == g[1]) {
                        cellule = LibellesTableau.recompense(e.getRecompense());
                    }
                }
                l.add(cellule);
            }
            l.add(LibellesTableau.recompenses(gr));
            lignes.add(l);
        }
        String note = null;
        ConditionsMaintien cm = t.getConditionsMaintien();
        if (cm.getReduction() == ConditionsMaintien.ModeReduction.REMPLISSAGE) {
            note = "Si un tableau compte moins de " + cm.getSeuilRemplissagePct()
                    + " % de ses places occupées au début de la compétition, sa dotation en espèces "
                    + (cm.getReductionPct() > 0 ? "est réduite de " + cm.getReductionPct() + " %." : "peut être réduite.");
        } else if (cm.getReduction() == ConditionsMaintien.ModeReduction.ABSENTS) {
            note = "La dotation en espèces d'un tableau est réduite de " + cm.getReductionPct() + " % par tranche de "
                    + cm.getTrancheAbsents() + (cm.getTrancheAbsents() > 1 ? " inscrits absents" : " inscrit absent")
                    + " au pointage, dans la limite de sa totalité.";
        }

        // Deux cas distincts : la base de calcul des pourcentages (note sous la grille) et la
        // réduction de dotation (paragraphe à part).
        String explication = null;
        if (!prop.isEmpty()) {
            List<String> noms = prop.stream().map(x -> "« " + x.getNom() + " »").toList();
            explication = (prop.size() > 1 ? "Pour les tableaux " + joindre(noms) : "Pour le tableau " + noms.get(0))
                    + ", les pourcentages portent sur le total des droits d'engagement perçus sur ce tableau"
                    + (prop.size() > 1 ? " (chacun le sien)" : "") + " : la dotation suit le nombre d'inscrits.";
        }
        blocs.add(new Grille(entetes, lignes, explication));
        if (note != null) {
            blocs.add(p(note));
        }
        blocs.add(p("Les récompenses sont remises à l'issue de chaque tableau ; pour un joueur mineur, la dotation "
                + "en espèces est remise à son représentant légal."));
        article("Dotations", blocs);
    }

    private static String libellePlaces(int debut, int fin) {
        if (debut == 1 && fin == 1) {
            return "Vainqueur";
        }
        if (debut == 2 && fin == 2) {
            return "Finaliste";
        }
        if (debut == 3 && fin == 4) {
            return "½ finalistes (chacun)";
        }
        if (debut == 5 && fin == 8) {
            return "¼ finalistes (chacun)";
        }
        return debut == fin ? debut + "e place" : "Places " + debut + " à " + fin + " (chacun)";
    }

    private void pointage() {
        boolean attente = t.getTableaux().stream().anyMatch(x -> x.getConfiguration().getTailleMaxListeAttente() > 0);
        String renvoi = numeroArticleTableaux > 0 ? " (article " + numeroArticleTableaux + ")" : "";
        article("Pointage", p("Chaque joueur pointe à la table du juge-arbitre au plus tard à l'heure de fin du "
                + "pointage de chacun de ses tableaux" + renvoi + ". Un joueur qui n'a pas pointé à cette heure perd "
                + "sa place" + (attente ? " : le juge-arbitre appelle alors le premier joueur présent de la liste "
                + "d'attente, qui la prend s'il pointe aussitôt." : ".")));
    }

    private void jugeArbitrage() {
        List<AffectationJugeArbitre> jas = t.getJugesArbitres();
        AffectationJugeArbitre principal = t.getJugeArbitrePrincipal().orElse(jas.isEmpty() ? null : jas.get(0));
        StringBuilder sb = new StringBuilder("Le tournoi est placé sous l'autorité de ");
        if (principal == null) {
            sb.append(trou("juge-arbitre désigné (nom et grade, IX.104)"));
        } else {
            sb.append(principal.getPrenom()).append(' ').append(principal.getNom())
                    .append(", juge-arbitre de grade ").append(principal.getGrade().getLibelle());
            List<String> adjoints = new ArrayList<>();
            for (AffectationJugeArbitre a : jas) {
                if (a != principal && a.getRole() == RoleJugeArbitre.ADJOINT) {
                    adjoints.add(a.getPrenom() + " " + a.getNom() + " (" + a.getGrade().getLibelle() + ")");
                }
            }
            if (!adjoints.isEmpty()) {
                sb.append(", assisté de ").append(String.join(", ", adjoints));
            }
        }
        sb.append(". Le juge-arbitre est seul habilité à régler les litiges, prévus ou non par le présent règlement, "
                + "et ses décisions sont sans appel (IX.109). Il ne peut pas participer au tournoi comme joueur. "
                + "Il peut modifier l'ordre ou l'horaire des parties pour la bonne marche de la compétition.");
        article("Juge-arbitrage", p(sb.toString()));
    }

    private void materiel() {
        StringBuilder sb = new StringBuilder();
        int nb = t.getNombreDeTables();
        if (nb <= 0) {
            sb.append("Le tournoi se déroule sur ").append(trou("nombre de tables")).append(" tables");
        } else {
            sb.append("Le tournoi se déroule sur ").append(nb).append(nb > 1 ? " tables" : " table");
            List<String> lots = new ArrayList<>();
            for (LotTables l : t.getLotsTables()) {
                StringBuilder d = new StringBuilder();
                if (t.getLotsTables().size() > 1) {
                    d.append(l.getNombre()).append(' ');
                }
                d.append(l.getMarque() != null ? l.getMarque() : "");
                if (l.getCouleur() != null) {
                    d.append(d.length() > 0 && !d.toString().endsWith(" ") ? " " : "").append(l.getCouleur().toLowerCase(Locale.FRENCH));
                }
                if (!d.toString().isBlank()) {
                    lots.add(d.toString().trim());
                }
            }
            if (!lots.isEmpty()) {
                sb.append(" (").append(String.join(", ", lots)).append(')');
            }
        }
        sb.append(", conformes aux normes et de marques agréées par l'ITTF ou la FFTT (3.2.1.1).");
        if (t.getTablesEchauffement() > 0) {
            sb.append(" Une salle d'échauffement de ").append(t.getTablesEchauffement())
                    .append(t.getTablesEchauffement() > 1 ? " tables" : " table")
                    .append(" est à la disposition des joueurs.");
        }

        InfosReglement ir = t.getInfosReglement();
        PolitiqueBalles pb = ir == null ? null : ir.getPolitiqueBalles();
        String couleur = ir == null || ir.getCouleurBalles() == null ? ""
                : (ir.getCouleurBalles() == CouleurBalles.BLANCHE ? " blanches" : " orange");
        String marque = ir == null || ir.getMarqueBalles() == null ? "" : " de marque " + ir.getMarqueBalles();
        if (pb == PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR) {
            sb.append(" Les balles, en plastique").append(couleur).append(marque).append(", agréées, sont fournies par le club.");
        } else if (pb == PolitiqueBalles.APPORTEES_PAR_JOUEURS) {
            sb.append(" Les joueurs fournissent leurs balles : balles en plastique").append(couleur)
                    .append(", agréées").append(marque.isEmpty() ? "" : ", de préférence" + marque).append('.');
            List<String> refusees = ir.getBallesRefusees();
            if (!refusees.isEmpty()) {
                sb.append(" Sont refusées : ").append(String.join(", ", refusees).toLowerCase(Locale.FRENCH)).append('.');
            }
        } else {
            sb.append(" Balles : ").append(trou("provenance des balles (club ou joueurs) et couleur")).append('.');
        }
        article("Matériel", p(sb.toString()));
    }

    private void tenue() {
        article("Tenue et comportement", p("Une tenue sportive conforme aux règlements de la FFTT est exigée dans "
                + "l'aire de jeu. Seuls les joueurs appelés, les arbitres et un conseiller par joueur pendant les "
                + "interruptions autorisées y ont accès. Tout manquement au comportement sportif est sanctionné "
                + "selon les règles du jeu (3.5)."));
    }

    private void resultats() {
        article("Résultats", p("Les résultats de tous les tableaux sont transmis à la FFTT dans les huit jours "
                + "suivant le tournoi, sous le format informatique communiqué lors de l'homologation (IX.107)."));
    }

    private void responsabilite() {
        article("Responsabilité", p("Le club décline toute responsabilité en cas d'accident, de perte ou de vol "
                + "dans la salle et ses abords."));
    }

    /** Articles facultatifs choisis dans la section Complémentaire, dans l'ordre d'ajout. */
    private void articlesOptionnels() {
        for (ArticleOptionnel a : o.getArticles()) {
            String titre = a.type().getLibelle();
            switch (a.type().name()) {
                case "DROIT_IMAGE" -> article(titre, p("En s'inscrivant, chaque participant accepte d'être photographié "
                        + "ou filmé pendant le tournoi et autorise le club à diffuser ces images sur ses supports (site, "
                        + "réseaux sociaux, presse), sauf opposition écrite remise au pointage. Les données d'inscription "
                        + "servent uniquement à l'organisation du tournoi et à la transmission des résultats à la FFTT."));
                case "BUVETTE" -> article(titre, p("Une buvette est ouverte dans la salle pendant toute la durée "
                        + "du tournoi" + (a.param(ArticleOptionnel.HORAIRES) != null ? ", " + a.param(ArticleOptionnel.HORAIRES)
                        : "") + ". Boissons chaudes et fraîches, sandwichs et petite restauration y sont proposés : "
                        + "joueurs, accompagnateurs et spectateurs peuvent se restaurer sur place tout au long de la "
                        + "journée, sans quitter la compétition."));
                case "OUVERTURE_SALLE" -> article(titre, p("La salle ouvre à "
                        + (a.param(ArticleOptionnel.HEURE) != null ? a.param(ArticleOptionnel.HEURE)
                        : trou("heure d'ouverture de la salle")) + " chaque jour du tournoi."));
                case "TIRAGE_AU_SORT" -> article(titre, p("Le tirage au sort public des poules de chaque tableau a lieu "
                        + (a.param(ArticleOptionnel.MINUTES) != null ? a.param(ArticleOptionnel.MINUTES) + " minutes après"
                        : "à") + " la fin de son pointage, à la table du juge-arbitre."));
                case "SUR_CLASSEMENT" -> {
                    String cat = a.param(ArticleOptionnel.CATEGORIE);
                    article(titre, p("Les joueurs à partir de la catégorie "
                            + (cat != null ? cat.toLowerCase(Locale.FRENCH) : trou("catégorie minimale"))
                            + " peuvent participer aux tableaux ouverts à toutes les catégories d'âge."));
                }
                case "FORFAIT_NON_EXCUSE" -> {
                    // Sans objet en promotionnel : les résultats ne comptent pas pour le classement (IX.102).
                    if (t.getNiveau() != NiveauTournoi.PROMOTIONNEL) {
                        article(titre, p("Un joueur qui ne se présente pas à sa première partie perd les points qu'il "
                                + "aurait perdus s'il l'avait jouée et perdue ; la commission sportive compétente peut "
                                + "lever cette perte pour des circonstances particulières et justifiées (article IV.202.2 "
                                + "des règlements administratifs)."));
                    }
                }
                case "INFOS_PRATIQUES" -> {
                    List<String> dispo = new ArrayList<>();
                    if (a.coche(ArticleOptionnel.PARKING)) {
                        dispo.add("un parking");
                    }
                    if (a.coche(ArticleOptionnel.VESTIAIRES)) {
                        dispo.add("des vestiaires et douches");
                    }
                    if (a.coche(ArticleOptionnel.PMR)) {
                        dispo.add("un accès pour les personnes à mobilité réduite");
                    }
                    StringBuilder sb = new StringBuilder();
                    if (!dispo.isEmpty()) {
                        String liste = joindre(dispo);
                        sb.append(liste.startsWith("un") ? "La salle dispose d'" : "La salle dispose de ")
                                .append(liste).append('.');
                    }
                    if (a.param(ArticleOptionnel.TEXTE) != null) {
                        sb.append(sb.length() > 0 ? " " : "").append(a.param(ArticleOptionnel.TEXTE));
                    }
                    if (sb.length() > 0) {
                        article(titre, p(sb.toString()));
                    }
                }
                default -> {
                    String texte = a.param(ArticleOptionnel.TEXTE);
                    if (texte != null) {
                        List<Bloc> blocs = new ArrayList<>();
                        for (String para : texte.split("\\R\\s*\\R")) {
                            if (!para.isBlank()) {
                                blocs.add(p(para.trim().replaceAll("\\s*\\R\\s*", " ")));
                            }
                        }
                        article(a.param(ArticleOptionnel.TITRE) != null ? a.param(ArticleOptionnel.TITRE)
                                : "Disposition particulière", blocs);
                    }
                }
            }
        }
    }

    private static String joindre(List<String> l) {
        if (l.size() == 1) {
            return l.get(0);
        }
        return String.join(", ", l.subList(0, l.size() - 1)) + " et " + l.get(l.size() - 1);
    }

    private void acceptation() {
        article("Acceptation", p("L'inscription au tournoi vaut acceptation sans réserve du présent règlement. Le "
                + "comité d'organisation, avec l'accord du juge-arbitre, peut le compléter par toute mesure nécessaire "
                + "au bon déroulement du tournoi."));
    }

    private void contact() {
        String nom = ((t.getCorrespondantPrenom() == null ? "" : t.getCorrespondantPrenom()) + " "
                + (t.getCorrespondantNom() == null ? "" : t.getCorrespondantNom())).trim();
        List<String> parts = new ArrayList<>();
        parts.add(nom.isEmpty() ? trou("correspondant du tournoi") : nom);
        if (t.getCorrespondantEmail() != null) {
            parts.add(t.getCorrespondantEmail());
        }
        if (t.getCorrespondantTelephone() != null) {
            parts.add(t.getCorrespondantTelephone());
        }
        article("Contact", p("Pour tout renseignement : " + String.join(", ", parts) + "."));
    }

    // ==================================================================
    // Check-list avant dépôt
    // ==================================================================

    private List<String> checkList() {
        List<String> l = new ArrayList<>(aCompleter);
        for (String m : cycle.elementsManquantsPourHomologation(t)) {
            String item = "Dossier d'homologation : " + m;
            if (!l.contains(item)) {
                l.add(item);
            }
        }
        if (l.isEmpty()) {
            return List.of();
        }
        EchelonHomologation e = t.getEchelonHomologation();
        LocalDate debut = t.getDateDebut();
        if (e == EchelonHomologation.FFTT && debut != null) {
            l.add("Déposer la demande via SPID au moins deux mois avant le tournoi (IX.104), soit au plus tard le "
                    + Formats.jourLong(debut.minusMonths(2)) + ", avec le règlement et les droits d'homologation");
        } else {
            l.add("Déposer la demande d'homologation auprès de la ligue, selon ses délais (IX.105), avec le règlement");
        }
        l.add("Vérifier qu'aucune épreuve incompatible ne tombe aux mêmes dates : tours du critérium fédéral, "
                + "journées du championnat de France par équipes, compétition nationale proche (IX.106)");
        return List.copyOf(l);
    }

    // ==================================================================
    // Outils
    // ==================================================================

    /** Insère un trou dans le texte et l'ajoute à la check-list. */
    private String trou(String quoi) {
        aCompleter.add("Compléter : " + quoi);
        return "[à compléter : " + quoi + "]";
    }

    private int article(String titre, Bloc... blocs) {
        return article(titre, List.of(blocs));
    }

    private int article(String titre, List<Bloc> blocs) {
        numero++;
        articles.add(new Article(numero, titre, List.copyOf(blocs)));
        return numero;
    }

    private static Paragraphe p(String texte) {
        return new Paragraphe(texte);
    }

    private List<Tableau> tableauxTries() {
        List<Tableau> l = new ArrayList<>(t.getTableaux());
        l.sort(Comparator.comparing((Tableau x) -> x.getHoraires() == null || x.getHoraires().getDebutMatchs() == null
                        ? LocalDateTime.MAX : x.getHoraires().getDebutMatchs())
                .thenComparing(Tableau::getNom));
        return l;
    }

    private static TarifInscription tarif(Tableau tab) {
        TarifInscription tr = tab.getConfiguration().getTarif();
        return tr == null ? TarifInscription.gratuit() : tr;
    }

    /** Tableaux dont la dotation comprend une part des inscriptions. */
    private List<Tableau> tableauxProportionnels() {
        return t.getTableaux().stream()
                .filter(x -> LibellesTableau.partInscriptions(x.getConfiguration().getRecompenses()) > 0)
                .sorted(java.util.Comparator.comparingLong(com.pingmanager.core.OrdonnancementTableaux::niveau)
                        .thenComparing(Tableau::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private double dotationTotale() {
        double total = 0;
        for (Tableau tab : t.getTableaux()) {
            total += LibellesTableau.totalArgent(tab.getConfiguration().getRecompenses());
        }
        return total;
    }

    private boolean aDesLots() {
        for (Tableau tab : t.getTableaux()) {
            GrilleRecompenses g = tab.getConfiguration().getRecompenses();
            if (g != null && g.getEntrees().stream().anyMatch(e -> e.getRecompense().getType() == TypeRecompense.AUTRE)) {
                return true;
            }
        }
        return false;
    }
}
