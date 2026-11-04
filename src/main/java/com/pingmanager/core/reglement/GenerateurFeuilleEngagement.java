package com.pingmanager.core.reglement;

import com.pingmanager.core.reglement.DocumentReglement.Article;
import com.pingmanager.core.reglement.DocumentReglement.Bloc;
import com.pingmanager.core.reglement.DocumentReglement.Grille;
import com.pingmanager.core.reglement.DocumentReglement.Liste;
import com.pingmanager.core.reglement.DocumentReglement.Paragraphe;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.LotTables;
import com.pingmanager.model.Paiement;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.PolitiqueForfaits;
import com.pingmanager.model.RoleJugeArbitre;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Feuille d'engagement d'un tournoi : la fiche papier qu'un joueur remplit quand il
 * ne peut pas s'inscrire en ligne. Elle reprend le modèle du club en six parties :
 * <ol>
 *   <li>Le tournoi (club, dates, salle, tables, juge-arbitre, clôture, correspondant) ;</li>
 *   <li>Comment s'inscrire (lien de paiement en ligne) ;</li>
 *   <li>Le joueur (cases vides à remplir) ;</li>
 *   <li>Choix des tableaux (un tableau par ligne, avec case à cocher et tarifs) ;</li>
 *   <li>Paiement ;</li>
 *   <li>Rappels, autorisation parentale et engagement.</li>
 * </ol>
 * Tout ce qui est connu est rempli depuis le tournoi ; le reste apparaît en pointillés.
 * Rendu PDF par {@link RenduPdfReglement} avec des titres « 1. Le tournoi ».
 */
public final class GenerateurFeuilleEngagement {

    /** Format des titres de section de la fiche. */
    public static final String FORMAT_TITRE = "%d. %s";

    private static final String POINTILLES = "……………………";
    private static final String CASE = "□";

    private Tournoi t;
    private final List<Article> sections = new ArrayList<>();

    public synchronized DocumentReglement generer(Tournoi tournoi) {
        this.t = Objects.requireNonNull(tournoi, "tournoi");
        sections.clear();
        leTournoi();
        commentSInscrire();
        leJoueur();
        choixDesTableaux();
        paiement();
        rappelsEtEngagement();
        String nom = t.getNom() == null || t.getNom().isBlank() ? "Tournoi" : t.getNom();
        String periode = Formats.periode(t.getDateDebut(), t.getDateFin());
        String sous = (periode == null ? "Dates à préciser" : Formats.capitale(periode.replaceFirst("^(le|les|du) ", "")))
                + (t.getAdresse() != null && t.getAdresse().getVille() != null ? ", " + t.getAdresse().getVille() : "");
        return new DocumentReglement("Feuille d'engagement — " + nom, sous, List.copyOf(sections), List.of());
    }

    // ------------------------------------------------------------------

    private void leTournoi() {
        InfosReglement ir = t.getInfosReglement();
        List<List<String>> l = new ArrayList<>();
        String club = t.getClubOrganisateur() == null ? POINTILLES : t.getClubOrganisateur();
        l.add(List.of("Club organisateur et n° de club",
                club + (t.getNumeroAffiliationClub() != null ? " (n° " + t.getNumeroAffiliationClub() + ")" : "")));
        l.add(List.of("N° d'homologation", vide(t.getNumeroHomologation()) ? "(attribué par la FFTT)"
                : t.getNumeroHomologation()));
        String periode = Formats.periode(t.getDateDebut(), t.getDateFin());
        l.add(List.of("Dates", periode == null ? POINTILLES : Formats.capitale(periode.replaceFirst("^(le|les|du) ", ""))));
        l.add(List.of("Salle et adresse", salle(ir)));
        l.add(List.of("Nombre de tables", tables()));
        l.add(List.of("Juge-arbitrage (nom, grade)", jugeArbitre()));
        l.add(List.of("Clôture des engagements",
                ir == null || ir.getClotureInscriptions() == null ? POINTILLES : Formats.dateHeure(ir.getClotureInscriptions())));
        l.add(List.of("Tirage au sort public", tirage(ir)));
        l.add(List.of("Correspondant (nom, email, téléphone)", correspondant()));
        String nom = t.getNom() == null ? POINTILLES : t.getNom();
        section("Le tournoi", p("Nom du tournoi : " + nom),
                new Grille(List.of("Rubrique", "Information"), l, null));
    }

    private String salle(InfosReglement ir) {
        AdresseTournoi a = t.getAdresse();
        StringBuilder sb = new StringBuilder();
        if (ir != null && !vide(ir.getSalleNom())) {
            sb.append(ir.getSalleNom()).append(", ");
        }
        if (a != null && a.getVoie() != null) {
            sb.append(a.getVoie()).append(", ").append(a.getCodePostal() == null ? "" : a.getCodePostal() + " ")
                    .append(a.getVille() == null ? "" : a.getVille());
        }
        return sb.length() == 0 ? POINTILLES : sb.toString().trim();
    }

    private String tables() {
        int n = t.getNombreDeTables();
        if (n <= 0) {
            return POINTILLES;
        }
        List<String> lots = new ArrayList<>();
        for (LotTables l : t.getLotsTables()) {
            String d = ((l.getMarque() == null ? "" : l.getMarque()) + " "
                    + (l.getCouleur() == null ? "" : l.getCouleur().toLowerCase(Locale.FRENCH))).trim();
            if (!d.isEmpty()) {
                lots.add(t.getLotsTables().size() > 1 ? l.getNombre() + " " + d : d);
            }
        }
        String res = n + (n > 1 ? " tables" : " table") + (lots.isEmpty() ? "" : " (" + String.join(", ", lots) + ")");
        if (t.getTablesEchauffement() > 0) {
            res += " + salle d'échauffement de " + t.getTablesEchauffement() + " tables";
        }
        return res;
    }

    private String jugeArbitre() {
        AffectationJugeArbitre ja = t.getJugesArbitres().stream()
                .filter(a -> a.getRole() == RoleJugeArbitre.PRINCIPAL).findFirst()
                .orElse(t.getJugesArbitres().isEmpty() ? null : t.getJugesArbitres().get(0));
        if (ja == null) {
            return "À désigner";
        }
        StringBuilder sb = new StringBuilder(ja.getPrenom() + " " + ja.getNom()
                + (ja.getGrade() == null ? "" : ", " + ja.getGrade().getLibelle()));
        for (AffectationJugeArbitre a : t.getJugesArbitres()) {
            if (a != ja && a.getRole() == RoleJugeArbitre.ADJOINT) {
                sb.append(" ; adjoint : ").append(a.getPrenom()).append(' ').append(a.getNom())
                        .append(a.getGrade() == null ? "" : ", " + a.getGrade().getLibelle());
            }
        }
        return sb.toString();
    }

    private String tirage(InfosReglement ir) {
        if (ir == null || ir.getTiragePublicDateHeure() == null) {
            return "À la table du juge-arbitre, à la fin du pointage de chaque tableau";
        }
        return Formats.dateHeure(ir.getTiragePublicDateHeure())
                + (vide(ir.getTiragePublicLieu()) ? "" : ", " + ir.getTiragePublicLieu());
    }

    private String correspondant() {
        StringBuilder sb = new StringBuilder();
        if (!vide(t.getCorrespondantPrenom()) || !vide(t.getCorrespondantNom())) {
            sb.append((nonNul(t.getCorrespondantPrenom()) + " " + nonNul(t.getCorrespondantNom())).trim());
        }
        if (!vide(t.getCorrespondantEmail())) {
            sb.append(sb.length() > 0 ? ", " : "").append(t.getCorrespondantEmail());
        }
        if (!vide(t.getCorrespondantTelephone())) {
            sb.append(sb.length() > 0 ? ", " : "").append(t.getCorrespondantTelephone());
        }
        return sb.length() == 0 ? POINTILLES : sb.toString();
    }

    private void commentSInscrire() {
        Paiement pay = t.getPaiement();
        String lien = pay.estPret() ? pay.getLien() : !vide(t.getSiteWeb()) ? t.getSiteWeb() : null;
        String plateforme = pay.estPret() ? " sur " + pay.getMode().getLibelle() : "";
        section("Comment s'inscrire",
                p("Inscription en ligne de préférence" + plateforme + " : " + (lien == null ? POINTILLES + " (lien)" : lien)),
                p("L'inscription en ligne vaut engagement et paiement : le joueur n'a alors pas besoin de cette fiche. "
                        + "La fiche sert aux joueurs qui ne peuvent pas s'inscrire en ligne. Elle est à renvoyer au "
                        + "correspondant avant la clôture des engagements."));
    }

    private void leJoueur() {
        List<List<String>> l = new ArrayList<>();
        for (String champ : List.of("Nom", "Prénom", "Sexe (H / F)", "Date de naissance", "N° de licence FFTT", "Club",
                "Points (classement)", "Catégorie d'âge", "Email", "Téléphone")) {
            l.add(List.of(champ, ""));
        }
        section("Le joueur", p("Une fiche par joueur, à remplir en majuscules."),
                new Grille(List.of("Champ", "À remplir par le joueur"), l, null));
    }

    private void choixDesTableaux() {
        List<Bloc> blocs = new ArrayList<>();
        // Limites : même texte que l'article « Nombre de tableaux par joueur » du règlement.
        List<String> limites = new GenerateurReglement().apercuNombreDeTableaux(t, t.getQuotasTableaux());
        if (!limites.isEmpty()) {
            blocs.add(p(String.join(" ", limites.stream().map(x -> x.replaceFirst("^• ", "")).toList())));
        }
        List<Tableau> liste = new ArrayList<>(t.getTableaux());
        liste.sort(Comparator.comparing((Tableau x) -> x.getHoraires() == null || x.getHoraires().getDebutMatchs() == null
                ? null : x.getHoraires().getDebutMatchs(), Comparator.nullsLast(Comparator.naturalOrder())));
        List<List<String>> lignes = new ArrayList<>();
        for (Tableau tab : liste) {
            HorairesTableau h = tab.getHoraires();
            ConfigurationTableau c = tab.getConfiguration();
            TarifInscription tr = c.getTarif() == null ? TarifInscription.gratuit() : c.getTarif();
            String quand = h == null || h.getJour() == null ? "—"
                    : Formats.capitale(Formats.jourCourt(h.getJour())) + (h.getDebutMatchs() == null ? ""
                    : ", " + Formats.heure(h.getDebutMatchs().toLocalTime()));
            lignes.add(List.of(CASE, tab.getNom(), quand, conditions(c),
                    c.getPlacesMax() > 0 ? String.valueOf(c.getPlacesMax()) : "—",
                    tr.isEnLigneProposee() ? Formats.montant(tr.getMontantEnLigne()) : "—",
                    tr.isSurPlaceProposee() ? Formats.montant(tr.getMontantSurPlace()) : "—"));
        }
        blocs.add(new Grille(List.of("Choix", "Tableau", "Jour et heure", "Conditions", "Places", "Tarif en ligne",
                "Tarif sur place"), lignes, null));
        blocs.add(p("Format : " + format(liste) + " Pointage : " + pointage(liste) + "."));
        section("Choix des tableaux", blocs);
    }

    private static String conditions(ConfigurationTableau c) {
        StringBuilder sb = new StringBuilder(LibellesTableau.points(c));
        if (c.getSexeRequis() == Sexe.FEMME) {
            sb.append(", dames");
        } else if (c.getSexeRequis() == Sexe.HOMME) {
            sb.append(", messieurs");
        }
        String cats = LibellesTableau.categories(c);
        if (cats != null) {
            sb.append(", ").append(cats);
        }
        return sb.toString();
    }

    private static String format(List<Tableau> liste) {
        if (liste.isEmpty()) {
            return POINTILLES + ".";
        }
        ConfigurationTableau c = liste.get(0).getConfiguration();
        int manches = c.getNombreManchesGagnantes() * 2 - 1;
        return "poules de " + c.getTaillePouleSouhaitee() + " joueurs, " + c.getQualifiesParPoule()
                + " qualifiés par poule, puis tableau final à élimination directe ; parties au meilleur des "
                + manches + " manches.";
    }

    private static String pointage(List<Tableau> liste) {
        for (Tableau tab : liste) {
            HorairesTableau h = tab.getHoraires();
            if (h != null && h.getFinPointage() != null && h.getDebutMatchs() != null) {
                long min = Duration.between(h.getFinPointage(), h.getDebutMatchs()).toMinutes();
                return "jusqu'à " + min + " minutes avant le début du tableau";
            }
        }
        return "…… minutes avant le début du tableau";
    }

    private void paiement() {
        Paiement pay = t.getPaiement();
        List<List<String>> l = List.of(List.of("Nombre de tableaux cochés", ""), List.of("Total à payer", "……… €"));
        section("Paiement", new Grille(List.of("Ligne", "Montant"), l, null),
                new Liste(List.of(CASE + " En ligne" + (pay.estPret() ? " sur " + pay.getMode().getLibelle() : ""),
                        CASE + " Sur place, au pointage (tarif sur place)")));
    }

    private void rappelsEtEngagement() {
        List<String> rappels = new ArrayList<>();
        rappels.add("Licence FFTT obligatoire, présentée au pointage (attestation papier ou application FFTT).");
        if (!t.getCategoriesExclues().isEmpty()) {
            List<String> cats = t.getCategoriesExclues().stream().map(CategorieAge::getLibelle)
                    .map(x -> x.toLowerCase(Locale.FRENCH)).toList();
            rappels.add("Non admis : " + String.join(", ", cats) + ".");
        }
        rappels.add("Tenue réglementaire exigée. Balles : " + balles() + ".");
        rappels.add("Points pris en compte : " + (t.getPhasePoints() == null ? POINTILLES
                : "ceux de la " + t.getPhasePoints().getLibelle()
                + (t.getDateDebut() == null ? "" : " " + Formats.saison(t.getDateDebut()))) + ".");
        rappels.add("Joueur absent à l'appel de sa partie : il peut être déclaré forfait par le juge-arbitre.");
        PolitiqueForfaits pf = t.getPolitiqueForfaits();
        if (pf.estComplete() && pf.getRegles().values().stream()
                .allMatch(r -> r.remboursement() == PolitiqueForfaits.Remboursement.AUCUN)) {
            rappels.add("Toute inscription est définitive : aucun remboursement en cas de forfait.");
        }
        section("Rappels et engagement",
                new Liste(rappels),
                p("Autorisation parentale (joueurs mineurs)"),
                p("Je soussigné(e) " + POINTILLES + ", responsable légal de " + POINTILLES
                        + ", l'autorise à participer au tournoi."),
                p("Engagement"),
                new Liste(List.of(CASE + " J'ai pris connaissance du règlement du tournoi et je l'accepte.",
                        CASE + " J'accepte la publication de mes nom, club et résultats.",
                        CASE + " J'autorise la prise et la diffusion de photos (facultatif).")),
                p("Fait à ………………, le ……/……/…… Signature :"));
    }

    private String balles() {
        InfosReglement ir = t.getInfosReglement();
        if (ir == null || ir.getPolitiqueBalles() == null) {
            return POINTILLES;
        }
        String couleur = ir.getCouleurBalles() == null ? "" : " " + ir.getCouleurBalles().getLibelle()
                .toLowerCase(Locale.FRENCH);
        if (ir.getPolitiqueBalles() == PolitiqueBalles.FOURNIES_PAR_ORGANISATEUR) {
            return "plastique" + couleur + ", fournies par le club"
                    + (vide(ir.getMarqueBalles()) ? "" : " (" + ir.getMarqueBalles() + ")");
        }
        return "plastique" + couleur + ", apportées par les joueurs";
    }

    // ------------------------------------------------------------------

    private void section(String titre, Bloc... blocs) {
        section(titre, List.of(blocs));
    }

    private void section(String titre, List<Bloc> blocs) {
        sections.add(new Article(sections.size() + 1, titre, List.copyOf(blocs)));
    }

    private static Paragraphe p(String s) {
        return new Paragraphe(s);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String nonNul(String s) {
        return s == null ? "" : s;
    }
}
