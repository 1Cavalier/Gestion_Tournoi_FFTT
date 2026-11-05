package com.pingmanager.persistence;

import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.Carton;
import com.pingmanager.model.CouleurCarton;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.EchelonHomologation;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.Poule;
import com.pingmanager.model.RegleTableauxParJour;
import com.pingmanager.model.StatutTableau;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeTournoi;
import com.pingmanager.persistence.json.Json;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pingmanager.persistence.ConversionsCommunes.ecrire;
import static com.pingmanager.persistence.ConversionsCommunes.ecrireEnum;
import static com.pingmanager.persistence.ConversionsCommunes.lireDate;
import static com.pingmanager.persistence.ConversionsCommunes.lireDateHeure;
import static com.pingmanager.persistence.ConversionsCommunes.lireEnum;

/**
 * Assemble le document complet : un {@link Tournoi} (avec tous ses tableaux et
 * leur déroulé) et le {@link RegistreDisciplinaire} associé.
 *
 * <p>Structure du document JSON produit :</p>
 * <pre>
 * {
 *   "version": 1,
 *   "joueurs": [ ... annuaire dédupliqué ... ],
 *   "tournoi": { ... métadonnées + tableaux ... },
 *   "discipline": { "cartons": [...], "exclus": [...] }
 * }
 * </pre>
 *
 * <p>L'annuaire des joueurs est écrit en premier et référencé par licence
 * partout ailleurs, ce qui garantit une instance unique par joueur à la
 * relecture. Les liens de progression entre matchs sont résolus en seconde
 * passe (voir {@link ContexteSerialisation}).</p>
 */
public final class TournoiMapper {

    /** Version du format de document, pour faire évoluer sans casser l'existant. */
    public static final int VERSION = 1;

    private TournoiMapper() {
    }

    // ==================================================================
    // Écriture
    // ==================================================================

    /**
     * Sérialise un tournoi et son registre disciplinaire en un arbre JSON.
     *
     * @param tournoi    le tournoi (obligatoire)
     * @param discipline le registre disciplinaire, ou {@code null} s'il n'y en a pas
     */
    /** Écrit uniquement le sous-objet « tournoi » dans le contexte donné. */
    static Json ecrirePartieTournoi(Tournoi tournoi, ContexteSerialisation ctx) {
        return ecrireTournoi(tournoi, ctx);
    }

    /** Écrit uniquement le sous-objet « discipline » (ou null) dans le contexte donné. */
    static Json ecrirePartieDiscipline(RegistreDisciplinaire discipline, ContexteSerialisation ctx) {
        return ecrireDiscipline(discipline, ctx);
    }

    /**
     * Reconstruit le tournoi depuis le sous-objet « tournoi », en utilisant le
     * contexte fourni (annuaire déjà chargé). Ne résout pas les liaisons de
     * matchs : l'appelant le fait après avoir lu toutes les parties.
     */
    static Tournoi lirePartieTournoi(Json jTournoi, ContexteSerialisation ctx) {
        return lireTournoi(jTournoi, ctx);
    }

    /** Reconstruit la discipline depuis le sous-objet « discipline », contexte fourni. */
    static RegistreDisciplinaire lirePartieDiscipline(Json jDiscipline, ContexteSerialisation ctx) {
        return lireDiscipline(jDiscipline, ctx);
    }

    public static Json ecrireDocument(Tournoi tournoi, RegistreDisciplinaire discipline) {
        ContexteSerialisation ctx = new ContexteSerialisation();
        Json doc = ecrireCorps(tournoi, discipline, ctx);
        return doc;
    }

    /**
     * Écrit le corps du document (annuaire + tournoi + discipline) dans un objet
     * JSON, en utilisant le contexte fourni. Permet à la persistance de session
     * de partager le même annuaire de joueurs et les mêmes identifiants de matchs
     * entre le tournoi et l'ordonnancement.
     */
    static Json ecrireCorps(Tournoi tournoi, RegistreDisciplinaire discipline,
                            ContexteSerialisation ctx) {
        // On sérialise d'abord le corps (ce qui peuple l'annuaire des joueurs),
        // puis on place l'annuaire en tête du document.
        Json jTournoi = ecrireTournoi(tournoi, ctx);
        Json jDiscipline = ecrireDiscipline(discipline, ctx);

        Json doc = Json.objet();
        doc.set("version", VERSION);
        doc.set("joueurs", ctx.ecrireAnnuaire());
        doc.set("tournoi", jTournoi);
        if (jDiscipline != null) {
            doc.set("discipline", jDiscipline);
        }
        return doc;
    }

    private static Json ecrireTournoi(Tournoi t, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("nom", t.getNom());
        o.set("clubOrganisateur", t.getClubOrganisateur());

        Json jours = Json.tableau();
        for (LocalDate j : t.getJours()) {
            jours.add(ecrire(j));
        }
        o.set("jours", jours);

        o.set("numeroHomologation", t.getNumeroHomologation());
        o.set("nombreDeTables", t.getNombreDeTables());
        o.set("niveau", ecrireEnum(t.getNiveau()));
        o.set("statut", ecrireEnum(t.getStatut()));
        o.set("regleTableauxParJour", ecrireRegle(t.getRegleTableauxParJour()));

        // Champs de la demande d'homologation (SPID).
        o.set("typeTournoi", ecrireEnum(t.getTypeTournoi()));
        o.set("dateDebut", ecrire(t.getDateDebut()));
        o.set("dateFin", ecrire(t.getDateFin()));
        o.set("adresse", ConfigMapper.ecrireAdresse(t.getAdresse()));
        o.set("correspondantNom", t.getCorrespondantNom());
        o.set("correspondantPrenom", t.getCorrespondantPrenom());
        o.set("correspondantEmail", t.getCorrespondantEmail());
        o.set("correspondantTelephone", t.getCorrespondantTelephone());
        o.set("correspondantAutresInfos", t.getCorrespondantAutresInfos());
        o.set("siteWeb", t.getSiteWeb());
        o.set("numeroAffiliationClub", t.getNumeroAffiliationClub());
        o.set("echelonHomologation", ecrireEnum(t.getEchelonHomologation()));
        o.set("accepteLicencePromotionnelle", t.accepteLicencePromotionnelle());
        o.set("accepteJoueursEtrangers", t.accepteJoueursEtrangers());
        Json exclues = Json.tableau();
        t.getCategoriesExclues().forEach(c -> exclues.add(Json.of(c.name())));
        o.set("categoriesExclues", exclues);
        o.set("dotationGlobale", t.getDotationGlobale());
        o.set("reglementDocument", ConfigMapper.ecrireDocument(t.getReglementDocument()));
        o.set("feuilleEngagement", ConfigMapper.ecrireDocument(t.getFeuilleEngagement()));
        o.set("imagePromotionnelle", ConfigMapper.ecrireDocument(t.getImagePromotionnelle()));
        o.set("optionsReglement", ecrireOptions(t.getOptionsReglement()));
        // Copies figées à la publication (facultatives : absentes avant publication).
        if (t.getReglementFige() != null) {
            o.set("reglementFige", DocumentReglementMapper.ecrire(t.getReglementFige()));
        }
        if (t.getFeuilleEngagementFigee() != null) {
            o.set("feuilleEngagementFigee", DocumentReglementMapper.ecrire(t.getFeuilleEngagementFigee()));
        }
        o.set("phasePoints", ecrireEnum(t.getPhasePoints()));
        o.set("modePreparation", ecrireEnum(t.getModePreparation()));
        o.set("quotasTableaux", ecrireQuotas(t.getQuotasTableaux()));
        o.set("conditionsMaintien", ecrireMaintien(t.getConditionsMaintien()));
        o.set("politiqueForfaits", ecrireForfaits(t.getPolitiqueForfaits()));
        o.set("paiement", ecrirePaiement(t.getPaiement()));
        // Refus d'homologation (facultatif : absent tant qu'il n'y en a pas eu).
        if (!t.getRefusHomologation().isEmpty()) {
            Json refus = Json.tableau();
            for (com.pingmanager.model.RefusHomologation r : t.getRefusHomologation()) {
                Json jr = Json.objet();
                jr.set("date", r.date().toString());
                jr.set("motif", r.motif());
                refus.add(jr);
            }
            o.set("refusHomologation", refus);
        }
        o.set("infosReglement", ConfigMapper.ecrireInfosReglement(t.getInfosReglement()));

        // Juges-arbitres.
        Json jas = Json.tableau();
        for (AffectationJugeArbitre a : t.getJugesArbitres()) {
            jas.add(ConfigMapper.ecrireJugeArbitre(a));
        }
        o.set("jugesArbitres", jas);
        Json arb = Json.tableau();
        t.getArbitres().forEach(a -> arb.add(ConfigMapper.ecrireArbitre(a)));
        o.set("arbitres", arb);
        Json lots = Json.tableau();
        t.getLotsTables().forEach(l -> lots.add(ConfigMapper.ecrireLotTables(l)));
        o.set("lotsTables", lots);
        o.set("tablesEchauffement", t.getTablesEchauffement());

        // Tableaux.
        Json tableaux = Json.tableau();
        for (Tableau tab : t.getTableaux()) {
            tableaux.add(ecrireTableau(tab, ctx));
        }
        o.set("tableaux", tableaux);
        return o;
    }

    static Json ecrireOptions(com.pingmanager.model.OptionsReglement r) {
        Json o = Json.objet();
        o.set("mode", ecrireEnum(r.getMode()));
        Json arts = Json.tableau();
        for (com.pingmanager.model.ArticleOptionnel a : r.getArticles()) {
            Json ja = Json.objet();
            ja.set("type", ecrireEnum(a.type()));
            Json params = Json.objet();
            a.parametres().forEach(params::set);
            ja.set("parametres", params);
            arts.add(ja);
        }
        o.set("articles", arts);
        return o;
    }

    static com.pingmanager.model.OptionsReglement lireOptions(Json o) {
        if (o == null || o.estNul()) {
            return com.pingmanager.model.OptionsReglement.parDefaut();
        }
        java.util.List<com.pingmanager.model.ArticleOptionnel> arts = new java.util.ArrayList<>();
        if (o.has("articles")) {
            for (Json ja : o.get("articles").elements()) {
                java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
                Json p = ja.get("parametres");
                if (!p.estNul()) {
                    p.entrees().forEach((k, v) -> params.put(k, v.asString()));
                }
                arts.add(new com.pingmanager.model.ArticleOptionnel(
                        lireEnum(ja.get("type"), com.pingmanager.model.ArticleOptionnel.Type.class), params));
            }
        } else if (o.getBoolean("droitImage", true)) {
            // Ancien format (avant les articles facultatifs) : clause droit à l'image cochée.
            arts.add(com.pingmanager.model.ArticleOptionnel.de(com.pingmanager.model.ArticleOptionnel.Type.DROIT_IMAGE));
        }
        return new com.pingmanager.model.OptionsReglement(
                lireEnum(o.get("mode"), com.pingmanager.model.OptionsReglement.Mode.class), arts);
    }

    // --- Quotas, maintien, forfaits, paiement -------------------------------

    static Json ecrireQuota(com.pingmanager.model.QuotasTableaux.Quota q) {
        Json o = Json.objet();
        if (q.joueurs() != null) {
            o.set("joueurs", q.joueurs());
        }
        if (q.joueuses() != null) {
            o.set("joueuses", q.joueuses());
        }
        return o;
    }

    static com.pingmanager.model.QuotasTableaux.Quota lireQuota(Json o) {
        return new com.pingmanager.model.QuotasTableaux.Quota(
                o.has("joueurs") ? o.getInt("joueurs", 1) : null,
                o.has("joueuses") ? o.getInt("joueuses", 1) : null);
    }

    static Json ecrireQuotas(com.pingmanager.model.QuotasTableaux q) {
        Json o = Json.objet();
        o.set("tousLesJours", ecrireQuota(q.getTousLesJours()));
        Json jours = Json.objet();
        q.getParJour().forEach((j, v) -> jours.set(j.toString(), ecrireQuota(v)));
        o.set("parJour", jours);
        o.set("tableauBonusJoueuses", q.getTableauBonusJoueuses());
        o.set("tableauBonusObligatoire", q.isTableauBonusObligatoire());
        o.set("tableauxConsecutifsInterdits", q.isTableauxConsecutifsInterdits());
        return o;
    }

    static com.pingmanager.model.QuotasTableaux lireQuotas(Json o) {
        if (o == null || o.estNul()) {
            return com.pingmanager.model.QuotasTableaux.aucun();
        }
        java.util.Map<java.time.LocalDate, com.pingmanager.model.QuotasTableaux.Quota> jours = new java.util.TreeMap<>();
        Json pj = o.get("parJour");
        if (!pj.estNul()) {
            pj.entrees().forEach((k, v) -> jours.put(java.time.LocalDate.parse(k), lireQuota(v)));
        }
        String bonus = o.getString("tableauBonusJoueuses", null);
        com.pingmanager.model.QuotasTableaux q;
        if (!jours.isEmpty()) {
            q = com.pingmanager.model.QuotasTableaux.parJour(jours);
        } else {
            com.pingmanager.model.QuotasTableaux.Quota t = lireQuota(o.get("tousLesJours"));
            q = com.pingmanager.model.QuotasTableaux.identiques(t.joueurs(), t.joueuses());
        }
        return q.avecTableauBonusJoueuses(bonus)
                .avecTableauBonusObligatoire(o.getBoolean("tableauBonusObligatoire", false))
                .avecTableauxConsecutifsInterdits(o.getBoolean("tableauxConsecutifsInterdits", false));
    }

    static Json ecrireMaintien(com.pingmanager.model.ConditionsMaintien c) {
        Json o = Json.objet();
        o.set("minimumInscrits", c.getMinimumInscrits());
        o.set("dateDecision", c.getDateDecision() == null ? null : c.getDateDecision().toString());
        o.set("reduction", ecrireEnum(c.getReduction()));
        o.set("seuilRemplissagePct", c.getSeuilRemplissagePct());
        o.set("trancheAbsents", c.getTrancheAbsents());
        o.set("reductionPct", c.getReductionPct());
        return o;
    }

    static com.pingmanager.model.ConditionsMaintien lireMaintien(Json o) {
        if (o == null || o.estNul()) {
            return com.pingmanager.model.ConditionsMaintien.aucune();
        }
        String d = o.getString("dateDecision", null);
        return new com.pingmanager.model.ConditionsMaintien(o.getInt("minimumInscrits", 0),
                d == null ? null : java.time.LocalDate.parse(d),
                lireEnum(o.get("reduction"), com.pingmanager.model.ConditionsMaintien.ModeReduction.class),
                o.getInt("seuilRemplissagePct", 0), o.getInt("trancheAbsents", 0), o.getInt("reductionPct", 0));
    }

    static Json ecrireForfaits(com.pingmanager.model.PolitiqueForfaits p) {
        Json o = Json.objet();
        Json regles = Json.objet();
        p.getRegles().forEach((per, r) -> {
            Json jr = Json.objet();
            jr.set("remboursement", ecrireEnum(r.remboursement()));
            jr.set("pourcentage", r.pourcentage());
            jr.set("justificatif", r.justificatif());
            regles.set(per.name(), jr);
        });
        o.set("regles", regles);
        o.set("fraisEnLigneDeduits", p.isFraisEnLigneDeduits());
        return o;
    }

    static com.pingmanager.model.PolitiqueForfaits lireForfaits(Json o) {
        if (o == null || o.estNul()) {
            return com.pingmanager.model.PolitiqueForfaits.nonDefinie();
        }
        java.util.Map<com.pingmanager.model.PolitiqueForfaits.Periode, com.pingmanager.model.PolitiqueForfaits.Regle> m =
                new java.util.EnumMap<>(com.pingmanager.model.PolitiqueForfaits.Periode.class);
        Json r = o.get("regles");
        if (!r.estNul()) {
            r.entrees().forEach((k, v) -> m.put(com.pingmanager.model.PolitiqueForfaits.Periode.valueOf(k),
                    new com.pingmanager.model.PolitiqueForfaits.Regle(
                            lireEnum(v.get("remboursement"), com.pingmanager.model.PolitiqueForfaits.Remboursement.class),
                            v.getInt("pourcentage", 0), v.getBoolean("justificatif", false))));
        }
        return new com.pingmanager.model.PolitiqueForfaits(m, o.getBoolean("fraisEnLigneDeduits", false));
    }

    static Json ecrirePaiement(com.pingmanager.model.Paiement p) {
        Json o = Json.objet();
        o.set("mode", ecrireEnum(p.getMode()));
        o.set("lien", p.getLien());
        return o;
    }

    static com.pingmanager.model.Paiement lirePaiement(Json o) {
        if (o == null || o.estNul()) {
            return com.pingmanager.model.Paiement.aucun();
        }
        return new com.pingmanager.model.Paiement(lireEnum(o.get("mode"), com.pingmanager.model.Paiement.Mode.class),
                o.getString("lien", null));
    }

    private static Json ecrireRegle(RegleTableauxParJour r) {
        Json o = Json.objet();
        o.set("max", r.getMaxTableauxParJour());
        o.set("exceptionFeminine", ecrireEnum(r.getExceptionFeminine()));
        if (r.getTableauSpecial() != null) {
            o.set("tableauSpecialNom", r.getTableauSpecial().getNom());
        }
        return o;
    }

    private static Json ecrireTableau(Tableau tab, ContexteSerialisation ctx) {
        Json o = Json.objet();
        o.set("nom", tab.getNom());
        o.set("type", tab.getType());
        o.set("configuration", ConfigMapper.ecrireConfiguration(tab.getConfiguration()));
        o.set("statut", ecrireEnum(tab.getStatut()));
        o.set("pointageCloture", tab.pointageEstCloture());
        if (tab.getGrainePoules() != null) {
            // Graine du tirage des poules (IX.109) ; absente des fichiers plus anciens.
            o.set("grainePoules", String.valueOf(tab.getGrainePoules()));
        }
        if (tab.getGraineTableauFinal() != null) {
            // Graine du tirage du tableau final (IX.109) ; absente des fichiers plus anciens.
            o.set("graineTableauFinal", String.valueOf(tab.getGraineTableauFinal()));
        }
        o.set("horaires", ConfigMapper.ecrireHoraires(tab.getHoraires()));

        Json inscrits = Json.tableau();
        for (Joueur j : tab.getInscrits()) {
            inscrits.add(ctx.refJoueur(j));
        }
        o.set("inscrits", inscrits);

        Json file = Json.tableau();
        for (Joueur j : tab.getFileAttente()) {
            file.add(ctx.refJoueur(j));
        }
        o.set("fileAttente", file);

        // États pointé / payé / remboursé, par joueur (faits par tableau).
        Json pointes = Json.tableau();
        Json payes = Json.tableau();
        for (Joueur j : tab.getInscrits()) {
            if (tab.aPointe(j)) {
                pointes.add(ctx.refJoueur(j));
            }
            if (tab.aPaye(j)) {
                payes.add(ctx.refJoueur(j));
            }
        }
        o.set("pointes", pointes);
        o.set("payes", payes);

        // Les remboursés peuvent ne plus être inscrits : on les repère via l'annuaire
        // en balayant inscrits + file ; pour les autres (désistés), la trace est déjà
        // portée par la discipline/le déroulé. Ici on écrit les remboursés connus.
        Json rembourses = Json.tableau();
        for (Joueur j : tab.getInscrits()) {
            if (tab.aEteRembourse(j)) {
                rembourses.add(ctx.refJoueur(j));
            }
        }
        o.set("rembourses", rembourses);

        // Poules.
        Json poules = Json.tableau();
        for (Poule p : tab.getPoules()) {
            poules.add(DerouleMapper.ecrirePoule(p, ctx));
        }
        o.set("poules", poules);

        // Phase finale (tableau à élimination), si montée.
        if (tab.aPhaseFinale()) {
            o.set("phaseFinale", DerouleMapper.ecrirePhaseFinale(tab.getPhaseFinale(), ctx));
        }
        return o;
    }

    private static Json ecrireDiscipline(RegistreDisciplinaire d, ContexteSerialisation ctx) {
        if (d == null) {
            return null;
        }
        Json o = Json.objet();
        Json cartons = Json.tableau();
        for (Carton c : d.tousLesCartons()) {
            Json jc = Json.objet();
            jc.set("joueur", ctx.refJoueur(c.getJoueur()));
            jc.set("couleur", ecrireEnum(c.getCouleur()));
            jc.set("pointsPenalite", c.getPointsPenalite());
            jc.set("horsMatch", c.estHorsMatch());
            jc.set("motif", c.getMotif());
            jc.set("instant", ecrire(c.getInstant()));
            cartons.add(jc);
        }
        o.set("cartons", cartons);
        return o;
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    /** Résultat d'un chargement : le tournoi et son registre disciplinaire. */
    public static final class DocumentCharge {
        private final Tournoi tournoi;
        private final RegistreDisciplinaire discipline;

        DocumentCharge(Tournoi tournoi, RegistreDisciplinaire discipline) {
            this.tournoi = tournoi;
            this.discipline = discipline;
        }

        public Tournoi getTournoi() {
            return tournoi;
        }

        /** @return le registre disciplinaire (jamais {@code null} : vide si absent du fichier). */
        public RegistreDisciplinaire getDiscipline() {
            return discipline;
        }
    }

    /** Reconstruit un tournoi complet + sa discipline depuis un arbre JSON. */
    public static DocumentCharge lire(Json doc) {
        int version = doc.getInt("version", 1);
        if (version > VERSION) {
            throw new com.pingmanager.persistence.json.JsonException(
                    "Version de fichier " + version + " plus récente que celle supportée ("
                            + VERSION + "). Mettez à jour PingManager.");
        }
        ContexteSerialisation ctx = new ContexteSerialisation();
        ctx.lireAnnuaire(doc.get("joueurs"));

        Tournoi tournoi = lireTournoi(doc.get("tournoi"), ctx);

        // Seconde passe : rétablir le graphe de matchs (sourceA/sourceB).
        ctx.resoudreLiaisons();

        RegistreDisciplinaire discipline = lireDiscipline(doc.get("discipline"), ctx);

        return new DocumentCharge(tournoi, discipline);
    }

    private static Tournoi lireTournoi(Json o, ContexteSerialisation ctx) {
        List<LocalDate> jours = new ArrayList<>();
        for (Json j : o.get("jours").elements()) {
            jours.add(lireDate(j));
        }
        Tournoi t = new Tournoi(o.get("nom").asString(), o.get("clubOrganisateur").asString(), jours);

        t.setNumeroHomologation(o.getString("numeroHomologation", ""));
        t.setNombreDeTables(o.getInt("nombreDeTables", 0));
        t.setNiveau(lireEnum(o.get("niveau"), NiveauTournoi.class));

        // Champs SPID.
        t.setTypeTournoi(lireEnum(o.get("typeTournoi"), TypeTournoi.class));
        LocalDate dd = lireDate(o.get("dateDebut"));
        LocalDate df = lireDate(o.get("dateFin"));
        if (dd != null || df != null) {
            t.setPeriode(dd, df);
        }
        t.setAdresse(ConfigMapper.lireAdresse(o.get("adresse")));
        t.setCorrespondant(
                o.getString("correspondantNom", null),
                o.getString("correspondantPrenom", null),
                o.getString("correspondantEmail", null),
                o.getString("correspondantTelephone", null),
                o.getString("correspondantAutresInfos", null));
        t.setSiteWeb(o.getString("siteWeb", null));
        t.setNumeroAffiliationClub(o.getString("numeroAffiliationClub", null));
        if (o.has("echelonHomologation") && !o.get("echelonHomologation").estNul()) {
            t.setEchelonHomologation(lireEnum(o.get("echelonHomologation"), EchelonHomologation.class));
        }
        t.setAccepteLicencePromotionnelle(o.getBoolean("accepteLicencePromotionnelle", true));
        t.setAccepteJoueursEtrangers(o.getBoolean("accepteJoueursEtrangers", true));
        if (o.has("categoriesExclues")) {
            java.util.List<com.pingmanager.model.CategorieAge> exclues = new java.util.ArrayList<>();
            for (Json c : o.get("categoriesExclues").elements()) {
                try {
                    exclues.add(com.pingmanager.model.CategorieAge.valueOf(c.asString()));
                } catch (IllegalArgumentException inconnue) {
                    // catégorie d'une autre version : ignorée
                }
            }
            t.setCategoriesExclues(exclues);
        }
        t.setDotationGlobale(o.getDouble("dotationGlobale", 0));
        t.setReglementDocument(ConfigMapper.lireDocument(o.get("reglementDocument")));
        t.setFeuilleEngagement(ConfigMapper.lireDocument(o.get("feuilleEngagement")));
        t.setImagePromotionnelle(ConfigMapper.lireDocument(o.get("imagePromotionnelle")));
        t.setOptionsReglement(lireOptions(o.get("optionsReglement")));
        t.setReglementFige(DocumentReglementMapper.lire(o.get("reglementFige")));
        t.setFeuilleEngagementFigee(DocumentReglementMapper.lire(o.get("feuilleEngagementFigee")));
        t.setPhasePoints(lireEnum(o.get("phasePoints"), com.pingmanager.model.PhasePoints.class));
        com.pingmanager.model.ModePreparation mode = lireEnum(o.get("modePreparation"),
                com.pingmanager.model.ModePreparation.class);
        t.setModePreparation(mode == null ? com.pingmanager.model.ModePreparation.COMPLETE : mode);
        t.setQuotasTableaux(lireQuotas(o.get("quotasTableaux")));
        t.setConditionsMaintien(lireMaintien(o.get("conditionsMaintien")));
        t.setPolitiqueForfaits(lireForfaits(o.get("politiqueForfaits")));
        t.setPaiement(lirePaiement(o.get("paiement")));
        lireRefusHomologation(t, o.get("refusHomologation"));
        t.setInfosReglement(ConfigMapper.lireInfosReglement(o.get("infosReglement")));

        // Juges-arbitres.
        if (o.has("arbitres")) {
            for (Json a : o.get("arbitres").elements()) {
                t.ajouterArbitre(ConfigMapper.lireArbitre(a));
            }
        }
        t.setTablesEchauffement(Math.max(0, o.getInt("tablesEchauffement", 0)));
        if (o.has("lotsTables")) {
            java.util.List<com.pingmanager.model.LotTables> lots = new java.util.ArrayList<>();
            for (Json l : o.get("lotsTables").elements()) {
                lots.add(ConfigMapper.lireLotTables(l));
            }
            int nb = t.getNombreDeTables();
            t.setLotsTables(lots);
            if (lots.isEmpty()) {
                t.setNombreDeTables(nb);
            }
        }
        if (o.has("jugesArbitres")) {
            for (Json ja : o.get("jugesArbitres").elements()) {
                t.ajouterJugeArbitre(ConfigMapper.lireJugeArbitre(ja));
            }
        }

        // Tableaux (ajoutés avant de résoudre la règle par jour, qui peut en référencer un).
        Map<String, Tableau> parNom = new java.util.LinkedHashMap<>();
        if (o.has("tableaux")) {
            for (Json jtab : o.get("tableaux").elements()) {
                Tableau tab = lireTableau(jtab, ctx);
                t.ajouterTableau(tab);
                parNom.put(tab.getNom(), tab);
            }
        }

        // Règle de tableaux par jour (peut référencer un tableau spécial par son nom).
        t.setRegleTableauxParJour(lireRegle(o.get("regleTableauxParJour"), parNom));

        // Statut : appliqué en dernier via transitions successives depuis BROUILLON.
        restaurerStatut(t, lireEnum(o.get("statut"), StatutTournoi.class));
        figerSiPublieSansCopie(t);
        return t;
    }

    /**
     * Tournoi publié enregistré avant les copies figées : le règlement et la
     * feuille d'engagement sont générés à la relecture et gardés (ils seront
     * écrits au prochain enregistrement). Un échec de génération n'empêche pas
     * la relecture : le service du règlement réessaiera à la première demande.
     */
    private static void figerSiPublieSansCopie(Tournoi t) {
        if (t.getStatut() != StatutTournoi.PUBLIE
                || (t.getReglementFige() != null && t.getFeuilleEngagementFigee() != null)) {
            return;
        }
        try {
            new com.pingmanager.core.reglement.ServiceReglementTournoi().figer(t);
        } catch (RuntimeException generationImpossible) {
            // Données incomplètes d'un ancien fichier : copie créée plus tard.
        }
    }

    /**
     * Refus d'homologation : absents des anciens fichiers ; une entrée illisible
     * (date ou motif manquant) est ignorée sans bloquer la relecture.
     */
    private static void lireRefusHomologation(Tournoi t, Json refus) {
        if (refus == null || refus.estNul()) {
            return;
        }
        for (Json jr : refus.elements()) {
            try {
                t.enregistrerRefusHomologation(new com.pingmanager.model.RefusHomologation(
                        java.time.Instant.parse(jr.getString("date", "")),
                        jr.getString("motif", "")));
            } catch (RuntimeException illisible) {
                // entrée incomplète ou corrompue : ignorée
            }
        }
    }

    private static void restaurerStatut(Tournoi t, StatutTournoi cible) {
        if (cible == null || cible == t.getStatut()) {
            return;
        }
        // Le graphe d'états est linéaire (BROUILLON → HOMOLOGUE → PUBLIE) avec
        // ANNULE atteignable ; on suit le chemin en s'appuyant sur les transitions
        // légales portées par l'enum.
        if (cible == StatutTournoi.ANNULE) {
            t.changerStatut(StatutTournoi.ANNULE);
            return;
        }
        if (cible == StatutTournoi.DEMANDE_HOMOLOGATION) {
            t.changerStatut(StatutTournoi.DEMANDE_HOMOLOGATION);
            return;
        }
        if (cible == StatutTournoi.HOMOLOGUE || cible == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.HOMOLOGUE);
        }
        if (cible == StatutTournoi.PUBLIE) {
            t.changerStatut(StatutTournoi.PUBLIE);
        }
    }

    private static RegleTableauxParJour lireRegle(Json o, Map<String, Tableau> parNom) {
        if (o == null || o.estNul()) {
            return RegleTableauxParJour.illimitee();
        }
        int max = o.getInt("max", 0);
        RegleTableauxParJour.ExceptionFeminine exc =
                lireEnum(o.get("exceptionFeminine"), RegleTableauxParJour.ExceptionFeminine.class);
        if (max <= 0) {
            return RegleTableauxParJour.illimitee();
        }
        if (exc == null) {
            return RegleTableauxParJour.quota(max);
        }
        return switch (exc.name()) {
            case "AUCUNE" -> RegleTableauxParJour.quota(max);
            case "TABLEAU_SUPPLEMENTAIRE_SANS_CONDITION" ->
                    RegleTableauxParJour.quotaAvecBonusFemininSansCondition(max);
            case "TABLEAU_SUPPLEMENTAIRE_SI_TABLEAU_SPECIAL" -> {
                Tableau special = parNom.get(o.getString("tableauSpecialNom", null));
                yield RegleTableauxParJour.quotaAvecBonusFemininSiTableauSpecial(max, special);
            }
            default -> throw new IllegalStateException("Exception féminine inconnue : " + exc);
        };
    }

    private static Tableau lireTableau(Json o, ContexteSerialisation ctx) {
        Tableau tab = new Tableau(
                o.get("nom").asString(),
                ConfigMapper.lireConfiguration(o.get("configuration")));

        Tableau.EtatRestaure e = new Tableau.EtatRestaure();
        e.type = o.getString("type", null);
        e.statut = lireEnum(o.get("statut"), StatutTableau.class);
        e.pointageCloture = o.getBoolean("pointageCloture", false);
        e.grainePoules = lireGraine(o.getString("grainePoules", null));
        e.graineTableauFinal = lireGraine(o.getString("graineTableauFinal", null));
        e.horaires = ConfigMapper.lireHoraires(o.get("horaires"));

        for (Json ref : o.get("inscrits").elements()) {
            e.inscrits.add(ctx.joueur(ref));
        }
        if (o.has("fileAttente")) {
            for (Json ref : o.get("fileAttente").elements()) {
                e.fileAttente.add(ctx.joueur(ref));
            }
        }
        if (o.has("pointes")) {
            for (Json ref : o.get("pointes").elements()) {
                e.pointes.add(ctx.joueur(ref));
            }
        }
        if (o.has("payes")) {
            for (Json ref : o.get("payes").elements()) {
                e.payes.add(ctx.joueur(ref));
            }
        }
        if (o.has("rembourses")) {
            for (Json ref : o.get("rembourses").elements()) {
                e.rembourses.add(ctx.joueur(ref));
            }
        }
        if (o.has("poules")) {
            for (Json jp : o.get("poules").elements()) {
                e.poules.add(DerouleMapper.lirePoule(jp, ctx));
            }
        }
        if (o.has("phaseFinale")) {
            e.phaseFinale = DerouleMapper.lirePhaseFinale(o.get("phaseFinale"), ctx);
        }

        tab.restaurerEtat(e);
        return tab;
    }

    /** Graine écrite en texte (un long ne tient pas toujours dans un nombre JSON exact). */
    private static Long lireGraine(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(texte.trim());
        } catch (NumberFormatException illisible) {
            return null;
        }
    }

    private static RegistreDisciplinaire lireDiscipline(Json o, ContexteSerialisation ctx) {
        RegistreDisciplinaire d = new RegistreDisciplinaire();
        if (o == null || o.estNul()) {
            return d;
        }
        if (o.has("cartons")) {
            for (Json jc : o.get("cartons").elements()) {
                Carton c = new Carton(
                        ctx.joueur(jc.get("joueur")),
                        lireEnum(jc.get("couleur"), CouleurCarton.class),
                        jc.getInt("pointsPenalite", 0),
                        jc.getBoolean("horsMatch", false),
                        jc.getString("motif", ""),
                        lireDateHeure(jc.get("instant")));
                d.enregistrer(c);
            }
        }
        return d;
    }
}
