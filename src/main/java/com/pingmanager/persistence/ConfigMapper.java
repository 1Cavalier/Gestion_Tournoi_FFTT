package com.pingmanager.persistence;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.AireDeJeu;
import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.FormatPhaseFinale;
import com.pingmanager.model.GradeJugeArbitre;
import com.pingmanager.model.GrilleRecompenses;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.ModeLimiteClassement;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Recompense;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.TypeRecompense;
import com.pingmanager.model.format.FormatTournoi;
import com.pingmanager.model.format.RegistreFormats;
import com.pingmanager.persistence.json.Json;

import static com.pingmanager.persistence.ConversionsCommunes.ecrire;
import static com.pingmanager.persistence.ConversionsCommunes.ecrireEnum;
import static com.pingmanager.persistence.ConversionsCommunes.lireDateHeure;
import static com.pingmanager.persistence.ConversionsCommunes.lireEnum;
import static com.pingmanager.persistence.ConversionsCommunes.lireHeure;

/**
 * Traduit les objets immuables de configuration et de métadonnées entre le
 * modèle et le JSON.
 *
 * <p>Ces objets se reconstruisent par leurs fabriques ou leurs builders (jamais
 * par réflexion). Le {@link FormatTournoi} d'un tableau est un cas particulier :
 * il n'est pas recréé mais <b>re-résolu par son identifiant</b> via le
 * {@link RegistreFormats}, conformément au principe « ajouter un format = une
 * classe enregistrée dans le registre ».</p>
 */
final class ConfigMapper {

    private ConfigMapper() {
    }

    // ------------------------------------------------------------------
    // TarifInscription
    // ------------------------------------------------------------------

    static Json ecrireTarif(TarifInscription t) {
        Json o = Json.objet();
        o.set("enLigne", t.getMontantEnLigne());
        o.set("surPlace", t.getMontantSurPlace());
        o.set("enLigneProposee", t.isEnLigneProposee());
        o.set("surPlaceProposee", t.isSurPlaceProposee());
        return o;
    }

    static TarifInscription lireTarif(Json o) {
        if (o == null || o.estNul()) {
            return TarifInscription.gratuit();
        }
        return new TarifInscription(o.getDouble("enLigne", 0), o.getBoolean("enLigneProposee", true),
                o.getDouble("surPlace", 0), o.getBoolean("surPlaceProposee", true));
    }

    // ------------------------------------------------------------------
    // Recompense + GrilleRecompenses
    // ------------------------------------------------------------------

    static Json ecrireRecompense(Recompense r) {
        Json o = Json.objet();
        o.set("type", ecrireEnum(r.getType()));
        if (r.getType() == TypeRecompense.ARGENT) {
            o.set("montant", r.getMontant());
        } else if (r.getType() == TypeRecompense.AUTRE) {
            o.set("libelle", r.getLibelle());
        } else {
            o.set("pourcentage", r.getPourcentage());
        }
        return o;
    }

    static Recompense lireRecompense(Json o) {
        TypeRecompense type = lireEnum(o.get("type"), TypeRecompense.class);
        if (type == TypeRecompense.REDUCTION_POURCENTAGE) {
            return Recompense.reductionPourcentage(o.getDouble("pourcentage", 0));
        }
        if (type == TypeRecompense.PART_INSCRIPTIONS) {
            return Recompense.partInscriptions(o.getDouble("pourcentage", 0));
        }
        if (type == TypeRecompense.AUTRE) {
            return Recompense.autre(o.getString("libelle", ""));
        }
        return Recompense.argent(o.getDouble("montant", 0));
    }

    static Json ecrireGrille(GrilleRecompenses g) {
        Json arr = Json.tableau();
        for (GrilleRecompenses.Entree e : g.getEntrees()) {
            Json o = Json.objet();
            o.set("placeDebut", e.getPlaceDebut());
            o.set("placeFin", e.getPlaceFin());
            o.set("recompense", ecrireRecompense(e.getRecompense()));
            arr.add(o);
        }
        return arr;
    }

    static GrilleRecompenses lireGrille(Json arr) {
        if (arr == null || arr.estNul() || arr.taille() == 0) {
            return GrilleRecompenses.vide();
        }
        GrilleRecompenses.Builder b = GrilleRecompenses.builder();
        for (Json o : arr.elements()) {
            b.plage(o.get("placeDebut").asInt(), o.get("placeFin").asInt(),
                    lireRecompense(o.get("recompense")));
        }
        return b.build();
    }

    // ------------------------------------------------------------------
    // ConfigurationTableau
    // ------------------------------------------------------------------

    static Json ecrireConfiguration(ConfigurationTableau c) {
        Json o = Json.objet();
        // Le format est identifié par son identifiant de registre.
        o.set("formatId", c.getFormat().identifiant());
        o.set("taillePouleSouhaitee", c.getTaillePouleSouhaitee());
        o.set("qualifiesParPoule", c.getQualifiesParPoule());
        o.set("formatPhaseFinale", ecrireEnum(c.getFormatPhaseFinale()));
        o.set("nombreManchesGagnantes", c.getNombreManchesGagnantes());
        // Petite finale par tableau ; absente des fichiers plus anciens (relus sans petite finale).
        o.set("petiteFinale", c.avecPetiteFinale());
        o.set("placesMax", c.getPlacesMax());
        o.set("tailleMaxListeAttente", c.getTailleMaxListeAttente());
        o.set("tarif", ecrireTarif(c.getTarif()));
        o.set("recompenses", ecrireGrille(c.getRecompenses()));
        o.set("modeLimite", ecrireEnum(c.getModeLimite()));
        o.set("pointsMin", c.getPointsMin());
        o.set("pointsMax", c.getPointsMax());
        o.set("exclusionRangNational", c.getExclusionRangNational());
        if (c.getSexeRequis() != null) {
            o.set("sexeRequis", ecrireEnum(c.getSexeRequis()));
        }
        Json cats = Json.tableau();
        for (CategorieAge ca : c.getCategoriesAutorisees()) {
            cats.add(Json.of(ca.name()));
        }
        o.set("categoriesAutorisees", cats);
        return o;
    }

    static ConfigurationTableau lireConfiguration(Json o) {
        String formatId = o.getString("formatId", null);
        FormatTournoi format = RegistreFormats.parIdentifiant(formatId)
                .orElseThrow(() -> new com.pingmanager.persistence.json.JsonException(
                        "Format inconnu du registre : " + formatId));
        ConfigurationTableau.Builder b = ConfigurationTableau.depuisFormat(format)
                .taillePouleSouhaitee(o.getInt("taillePouleSouhaitee", 4))
                .qualifiesParPoule(o.getInt("qualifiesParPoule", 2))
                .formatPhaseFinale(lireEnum(o.get("formatPhaseFinale"), FormatPhaseFinale.class))
                .nombreManchesGagnantes(o.getInt("nombreManchesGagnantes", 3))
                .petiteFinale(o.getBoolean("petiteFinale", format.avecPetiteFinale()))
                .placesMax(o.getInt("placesMax", 0))
                .tailleMaxListeAttente(o.getInt("tailleMaxListeAttente", 4))
                .tarif(lireTarif(o.get("tarif")))
                .recompenses(lireGrille(o.get("recompenses")));

        // Mode de limite de classement.
        ModeLimiteClassement mode = lireEnum(o.get("modeLimite"), ModeLimiteClassement.class);
        int pMin = o.getInt("pointsMin", 0);
        int pMax = o.getInt("pointsMax", 0);
        if (mode == ModeLimiteClassement.TRANCHE) {
            b.tranche(pMin, pMax);
        } else if (mode == ModeLimiteClassement.POINT_MAX) {
            b.pointMax(pMax);
        } else if (mode == ModeLimiteClassement.TOUTES_SERIES) {
            b.toutesSeries();
        }
        int exclRang = o.getInt("exclusionRangNational", 0);
        if (exclRang > 0) {
            b.exclusionRangNational(exclRang);
        }
        if (o.has("sexeRequis")) {
            b.sexeRequis(lireEnum(o.get("sexeRequis"), Sexe.class));
        }
        if (o.has("categoriesAutorisees")) {
            for (Json jc : o.get("categoriesAutorisees").elements()) {
                b.autoriserCategorie(CategorieAge.valueOf(jc.asString()));
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------
    // HorairesTableau
    // ------------------------------------------------------------------

    static Json ecrireHoraires(HorairesTableau h) {
        Json o = Json.objet();
        o.set("debutPointage", ecrire(h.getDebutPointage()));
        o.set("finPointage", ecrire(h.getFinPointage()));
        o.set("debutMatchs", ecrire(h.getDebutMatchs()));
        o.set("finTheorique", ecrire(h.getFinTheorique()));
        return o;
    }

    static HorairesTableau lireHoraires(Json o) {
        if (o == null || o.estNul()) {
            return new HorairesTableau();
        }
        return new HorairesTableau(
                lireDateHeure(o.get("debutPointage")),
                lireDateHeure(o.get("finPointage")),
                lireDateHeure(o.get("debutMatchs")),
                lireDateHeure(o.get("finTheorique")));
    }

    // ------------------------------------------------------------------
    // AdresseTournoi
    // ------------------------------------------------------------------

    static Json ecrireAdresse(AdresseTournoi a) {
        if (a == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("voie", a.getVoie());
        o.set("residenceBatiment", a.getResidenceBatiment());
        o.set("lieuDit", a.getLieuDit());
        o.set("codePostal", a.getCodePostal());
        o.set("ville", a.getVille());
        if (a.getCoordonnees() != null) {
            o.set("latitude", a.getCoordonnees().latitude());
            o.set("longitude", a.getCoordonnees().longitude());
        }
        return o;
    }

    static AdresseTournoi lireAdresse(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        // On recharge sans réexiger les champs obligatoires : un tournoi encore en
        // brouillon peut avoir été sauvegardé avec une adresse partielle. La
        // complétude est revérifiée par le service de cycle de vie, pas au chargement.
        AdresseTournoi a = AdresseTournoi.partielle(
                o.getString("voie", null),
                o.getString("residenceBatiment", null),
                o.getString("lieuDit", null),
                o.getString("codePostal", null),
                o.getString("ville", null));
        if (o.has("latitude") && o.has("longitude")) {
            a = a.avecCoordonnees(new com.pingmanager.model.Coordonnees(
                    o.getDouble("latitude", 0), o.getDouble("longitude", 0)));
        }
        return a;
    }

    // ------------------------------------------------------------------
    // DocumentReference
    // ------------------------------------------------------------------

    static Json ecrireDocument(DocumentReference d) {
        if (d == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("libelle", d.getLibelle());
        o.set("emplacement", d.getEmplacement());
        return o;
    }

    static DocumentReference lireDocument(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        return new DocumentReference(o.getString("libelle", ""), o.getString("emplacement", ""));
    }

    // ------------------------------------------------------------------
    // AireDeJeu
    // ------------------------------------------------------------------

    static Json ecrireAire(AireDeJeu a) {
        if (a == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        if (a.getLongueurCourtM() != null) {
            o.set("longueur", a.getLongueurCourtM());
        }
        if (a.getLargeurCourtM() != null) {
            o.set("largeur", a.getLargeurCourtM());
        }
        if (a.getEclairageLux() != null) {
            o.set("eclairageLux", a.getEclairageLux());
        }
        return o;
    }

    static AireDeJeu lireAire(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        Double lo = o.has("longueur") ? o.get("longueur").asDouble() : null;
        Double la = o.has("largeur") ? o.get("largeur").asDouble() : null;
        Integer ec = o.has("eclairageLux") ? o.get("eclairageLux").asInt() : null;
        return new AireDeJeu(lo, la, ec);
    }

    // ------------------------------------------------------------------
    // AffectationJugeArbitre
    // ------------------------------------------------------------------

    static Json ecrireJugeArbitre(AffectationJugeArbitre a) {
        Json o = Json.objet();
        o.set("nom", a.getNom());
        o.set("prenom", a.getPrenom());
        o.set("email", a.getEmail());
        o.set("licence", a.getNumeroLicence());
        o.set("grade", ecrireEnum(a.getGrade()));
        o.set("role", ecrireEnum(a.getRole()));
        if (a.getTelephone() != null) {
            o.set("telephone", a.getTelephone());
        }
        return o;
    }

    static AffectationJugeArbitre lireJugeArbitre(Json o) {
        // Grade relu avec tolérance : anciens noms internes (JA_REGIONAL…) acceptés.
        String g = o.getString("grade", null);
        GradeJugeArbitre grade = GradeJugeArbitre.depuisCode(g).orElseGet(() -> {
            try {
                return GradeJugeArbitre.valueOf(g);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Grade de juge-arbitre inconnu : " + g);
            }
        });
        return new AffectationJugeArbitre(
                o.getString("nom", ""),
                o.getString("prenom", ""),
                o.getString("email", null),
                o.getString("licence", ""),
                grade,
                o.getString("telephone", null),
                lireEnum(o.get("role"), com.pingmanager.model.RoleJugeArbitre.class));
    }

    // ------------------------------------------------------------------
    // InfosReglement (IX.111)
    // ------------------------------------------------------------------

    private static java.util.List<String> lireTexte(Json tab) {
        java.util.List<String> l = new java.util.ArrayList<>();
        if (tab != null && !tab.estNul()) {
            for (Json x : tab.elements()) {
                l.add(x.asString());
            }
        }
        return l;
    }

    static Json ecrireArbitre(com.pingmanager.model.AffectationArbitre a) {
        Json o = Json.objet();
        o.set("licence", a.getNumeroLicence());
        o.set("nom", a.getNom());
        o.set("prenom", a.getPrenom());
        o.set("grade", a.getGrade());
        return o;
    }

    static com.pingmanager.model.AffectationArbitre lireArbitre(Json o) {
        return new com.pingmanager.model.AffectationArbitre(o.getString("licence", ""),
                o.getString("nom", ""), o.getString("prenom", ""), o.getString("grade", ""));
    }

    static Json ecrireLotTables(com.pingmanager.model.LotTables l) {
        Json o = Json.objet();
        o.set("nombre", l.getNombre());
        o.set("marque", l.getMarque());
        o.set("couleur", l.getCouleur());
        return o;
    }

    static com.pingmanager.model.LotTables lireLotTables(Json o) {
        return new com.pingmanager.model.LotTables(o.getInt("nombre", 1),
                o.getString("marque", null), o.getString("couleur", null));
    }

    static Json ecrireInfosReglement(InfosReglement r) {
        if (r == null) {
            return Json.nul();
        }
        Json o = Json.objet();
        o.set("responsableNom", r.getResponsableNom());
        o.set("responsableEmail", r.getResponsableEmail());
        o.set("responsableTelephone", r.getResponsableTelephone());
        o.set("salleNom", r.getSalleNom());
        o.set("salleAdresse", r.getSalleAdresse());
        o.set("salleCodePostal", r.getSalleCodePostal());
        o.set("salleVille", r.getSalleVille());
        o.set("nombreDeTables", r.getNombreDeTables());
        o.set("marqueBalles", r.getMarqueBalles());
        o.set("politiqueBalles", ecrireEnum(r.getPolitiqueBalles()));
        o.set("couleurBalles", ecrireEnum(r.getCouleurBalles()));
        Json refusees = Json.tableau();
        r.getBallesRefusees().forEach(x -> refusees.add(Json.of(x)));
        o.set("ballesRefusees", refusees);
        o.set("aireDeJeu", ecrireAire(r.getAireDeJeu()));
        o.set("ouvertureInscriptions", ecrire(r.getOuvertureInscriptions()));
        o.set("clotureInscriptions", ecrire(r.getClotureInscriptions()));
        o.set("cloturePointage", ecrire(r.getCloturePointage()));
        o.set("debutMatchs", ecrire(r.getDebutMatchs()));
        o.set("finPrevue", ecrire(r.getFinPrevue()));
        o.set("horairePrevuFinales", ecrire(r.getHorairePrevuFinales()));
        o.set("tiragePublicDateHeure", ecrire(r.getTiragePublicDateHeure()));
        o.set("tiragePublicLieu", r.getTiragePublicLieu());
        return o;
    }

    static InfosReglement lireInfosReglement(Json o) {
        if (o == null || o.estNul()) {
            return null;
        }
        InfosReglement.Builder b = InfosReglement.builder()
                .responsableNom(o.getString("responsableNom", null))
                .responsableEmail(o.getString("responsableEmail", null))
                .responsableTelephone(o.getString("responsableTelephone", null))
                .salleNom(o.getString("salleNom", null))
                .salleAdresse(o.getString("salleAdresse", null))
                .salleCodePostal(o.getString("salleCodePostal", null))
                .salleVille(o.getString("salleVille", null))
                .nombreDeTables(o.getInt("nombreDeTables", 0))
                .marqueBalles(o.getString("marqueBalles", null))
                .politiqueBalles(lireEnum(o.get("politiqueBalles"), PolitiqueBalles.class))
                .couleurBalles(lireEnum(o.get("couleurBalles"), com.pingmanager.model.CouleurBalles.class))
                .ballesRefusees(lireTexte(o.get("ballesRefusees")))
                .aireDeJeu(lireAire(o.get("aireDeJeu")))
                .ouvertureInscriptions(lireDateHeure(o.get("ouvertureInscriptions")))
                .clotureInscriptions(lireDateHeure(o.get("clotureInscriptions")))
                .cloturePointage(lireDateHeure(o.get("cloturePointage")))
                .debutMatchs(lireDateHeure(o.get("debutMatchs")))
                .finPrevue(lireHeure(o.get("finPrevue")))
                .horairePrevuFinales(lireHeure(o.get("horairePrevuFinales")))
                .tiragePublicDateHeure(lireDateHeure(o.get("tiragePublicDateHeure")))
                .tiragePublicLieu(o.getString("tiragePublicLieu", null));
        return b.build();
    }
}
