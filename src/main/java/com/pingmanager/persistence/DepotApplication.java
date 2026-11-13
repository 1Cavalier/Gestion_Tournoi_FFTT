package com.pingmanager.persistence;

import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.RegistrePrononciations;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.PrononciationClub;
import com.pingmanager.model.PrononciationJoueur;
import com.pingmanager.persistence.json.Json;
import com.pingmanager.persistence.json.JsonException;
import com.pingmanager.persistence.json.JsonWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Stockage des données d'<b>application</b> qui vivent entre les tournois :
 * référentiel des clubs, comptes organisateurs et accès (qui peut administrer
 * quel club).
 *
 * <p>Contrairement au document de tournoi, ces données ne dépendent pas d'un
 * tournoi précis : elles constituent le socle réutilisé d'un tournoi à l'autre.
 * Ce dépôt les tient en mémoire, indexées par identité, et sait se sauvegarder
 * et se recharger dans un fichier JSON local (écriture atomique, aucune
 * dépendance externe), au même titre que le reste de la persistance.</p>
 *
 * <p>Il joue trois rôles pour le reste du logiciel :</p>
 * <ul>
 *   <li>il <b>implémente {@link RegistreClubs}</b>, donc se branche directement
 *       sur {@code ServiceComptesOrganisateurs} à la place de la version mémoire ;</li>
 *   <li>il fournit le prédicat {@link #emailDejaUtilise()} attendu par le service
 *       à l'inscription (unicité de l'email de compte) ;</li>
 *   <li>il enregistre les {@link CompteOrganisateur} et {@link AccesClub} produits
 *       par le service, pour qu'ils survivent à la fermeture du logiciel.</li>
 * </ul>
 *
 * <p>Le dépôt vit dans la couche de persistance et dépend de {@code core} et
 * {@code model} ; jamais l'inverse (règle d'or).</p>
 */
public final class DepotApplication implements RegistreClubs {

    /** Version du format de fichier applicatif. */
    public static final int VERSION = 1;

    /** Nom de fichier conseillé pour le dépôt applicatif. */
    public static final String NOM_FICHIER = "pingmanager-application.json";

    private final Map<String, Club> clubsParNumero = new LinkedHashMap<>();
    private final Map<String, CompteOrganisateur> comptesParEmail = new LinkedHashMap<>();
    private final Map<String, CompteUtilisateur> comptesUtilisateursParEmail = new LinkedHashMap<>();
    private final Map<String, AccesClub> accesParCle = new LinkedHashMap<>();
    private final Map<String, AgendaCompte> agendasParEmail = new LinkedHashMap<>();

    /** B018 : prononciations phonétiques des joueurs et des clubs (appels au micro). */
    private final RegistrePrononciations prononciations = new RegistrePrononciations();

    /** Tournois récemment ouverts, du plus récent au plus ancien. */
    private final List<ProjetTournoi> recents = new ArrayList<>();

    /** Nombre maximum d'entrées conservées dans la liste des récents. */
    public static final int MAX_RECENTS = 12;

    // ==================================================================
    // Clubs (référentiel) — implémente RegistreClubs
    // ==================================================================

    /** Ajoute ou remplace un club (indexé par numéro FFTT). */
    public DepotApplication ajouterClub(Club club) {
        Objects.requireNonNull(club, "club");
        clubsParNumero.put(cleClub(club.getNumero()), club);
        return this;
    }

    @Override
    public Optional<Club> trouverParNumero(String numeroClub) {
        if (numeroClub == null || numeroClub.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clubsParNumero.get(cleClub(numeroClub)));
    }

    public int nombreDeClubs() {
        return clubsParNumero.size();
    }

    // ==================================================================
    // Comptes organisateurs
    // ==================================================================

    /**
     * Enregistre (ou remplace) un compte, indexé par email. Typiquement appelé
     * après {@code ServiceComptesOrganisateurs.inscrire(...)} pour persister le
     * compte créé, puis de nouveau après confirmation pour refléter l'email
     * vérifié.
     */
    public DepotApplication enregistrerCompte(CompteOrganisateur compte) {
        Objects.requireNonNull(compte, "compte");
        comptesParEmail.put(cleEmail(compte.getEmail()), compte);
        return this;
    }

    public Optional<CompteOrganisateur> trouverCompte(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(comptesParEmail.get(cleEmail(email)));
    }

    /**
     * @return un prédicat « cet email est déjà pris par un compte », à passer
     *         directement à {@code ServiceComptesOrganisateurs.inscrire(...)}.
     */
    public Predicate<String> emailDejaUtilise() {
        return email -> email != null && comptesParEmail.containsKey(cleEmail(email));
    }

    public List<CompteOrganisateur> tousLesComptes() {
        return new ArrayList<>(comptesParEmail.values());
    }

    // ==================================================================
    // Comptes utilisateurs (socle : membres et organisateurs)
    // ==================================================================

    /**
     * Enregistre (ou remplace) un compte utilisateur, indexé par email.
     * Appelé après la création d'un compte, puis de nouveau après chaque
     * évolution (liaison d'une licence, changement de mot de passe).
     */
    public DepotApplication enregistrerCompteUtilisateur(CompteUtilisateur compte) {
        Objects.requireNonNull(compte, "compte");
        comptesUtilisateursParEmail.put(cleEmail(compte.getEmail()), compte);
        return this;
    }

    public Optional<CompteUtilisateur> trouverCompteUtilisateur(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(comptesUtilisateursParEmail.get(cleEmail(email)));
    }

    /**
     * @return un prédicat « cet email est déjà pris par un compte utilisateur »,
     *         à passer à {@code ServiceAuthentification.creerCompte(...)}.
     */
    /**
     * Point 14 : le compte qui porte cette licence (une licence pour un seul compte).
     * Un ancien fichier où deux comptes la portent renvoie le premier enregistré.
     */
    public Optional<CompteUtilisateur> compteDeLaLicence(String numeroLicence) {
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return Optional.empty();
        }
        String n = numeroLicence.trim();
        for (CompteUtilisateur c : comptesUtilisateursParEmail.values()) {
            if (n.equals(c.getLicenceLiee())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public Predicate<String> emailUtilisateurDejaUtilise() {
        return email -> email != null && comptesUtilisateursParEmail.containsKey(cleEmail(email));
    }

    // ==================================================================
    // Accès (habilitations)
    // ==================================================================

    /** Enregistre (ou remplace) un accès, identifié par le couple (club, email). */
    public DepotApplication enregistrerAcces(AccesClub acces) {
        Objects.requireNonNull(acces, "acces");
        accesParCle.put(cleAcces(acces.getNumeroClub(), acces.getEmail()), acces);
        return this;
    }

    /** Retire l'accès d'une personne à un club, s'il existe. */
    public DepotApplication retirerAcces(String numeroClub, String email) {
        if (numeroClub != null && email != null) {
            accesParCle.remove(cleAcces(numeroClub, email));
        }
        return this;
    }

    /** @return true si cette personne est habilitée à administrer ce club. */
    public boolean aAcces(String numeroClub, String email) {
        if (numeroClub == null || email == null) {
            return false;
        }
        return accesParCle.containsKey(cleAcces(numeroClub, email));
    }

    /** @return les accès (habilitations) détenus par une personne. */
    public List<AccesClub> accesDe(String email) {
        List<AccesClub> resultat = new ArrayList<>();
        if (email == null) {
            return resultat;
        }
        String cle = cleEmail(email);
        for (AccesClub a : accesParCle.values()) {
            if (cleEmail(a.getEmail()).equals(cle)) {
                resultat.add(a);
            }
        }
        return resultat;
    }

    public List<AccesClub> tousLesAcces() {
        return new ArrayList<>(accesParCle.values());
    }

    // ==================================================================
    // Agendas des comptes
    // ==================================================================

    /**
     * Agenda du compte de cet email ; créé vide (et retenu) au premier appel. Les
     * modifications faites sur l'agenda renvoyé sont sauvegardées avec le dépôt.
     */
    public AgendaCompte agendaDe(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email obligatoire");
        }
        return agendasParEmail.computeIfAbsent(cleEmail(email), AgendaCompte::new);
    }

    /** Enregistre (ou remplace) l'agenda d'un compte. */
    public DepotApplication enregistrerAgenda(AgendaCompte agenda) {
        Objects.requireNonNull(agenda, "agenda");
        agendasParEmail.put(cleEmail(agenda.getEmail()), agenda);
        return this;
    }

    public List<AgendaCompte> tousLesAgendas() {
        return new ArrayList<>(agendasParEmail.values());
    }

    // ==================================================================
    // Prononciations (B018)
    // ==================================================================

    /**
     * Registre des prononciations phonétiques, réutilisé d'un tournoi à l'autre.
     * Les modifications sont sauvegardées avec le dépôt.
     */
    public RegistrePrononciations prononciations() {
        return prononciations;
    }

    // ==================================================================
    // Tournois récents
    // ==================================================================

    /**
     * Place (ou remonte) un tournoi en tête de la liste des récents. Dédupliqué
     * par fichier : ré-ouvrir un tournoi déjà présent le remonte simplement en
     * tête au lieu de créer un doublon. La liste est plafonnée à
     * {@link #MAX_RECENTS}.
     */
    public DepotApplication noterRecent(ProjetTournoi projet) {
        Objects.requireNonNull(projet, "projet");
        recents.removeIf(p -> p.getFichier().equals(projet.getFichier()));
        recents.add(0, projet);
        while (recents.size() > MAX_RECENTS) {
            recents.remove(recents.size() - 1);
        }
        return this;
    }

    /** Retire un tournoi de la liste des récents (fichier introuvable, oubli volontaire). */
    public DepotApplication oublierRecent(Path fichier) {
        if (fichier != null) {
            Path abs = fichier.toAbsolutePath();
            recents.removeIf(p -> p.getFichier().equals(abs));
        }
        return this;
    }

    /** @return la liste des récents, du plus récent au plus ancien (copie). */
    public List<ProjetTournoi> tousLesRecents() {
        return new ArrayList<>(recents);
    }

    // ==================================================================
    // Sauvegarde / chargement
    // ==================================================================

    /** Sauvegarde le dépôt dans un fichier (écriture atomique). */
    public void sauvegarder(Path cible) {
        Json doc = ecrire();
        String texte = JsonWriter.ecrire(doc, true);
        DepotTournoi.ecrireAtomique(cible, texte);
    }

    /**
     * Charge un dépôt applicatif depuis un fichier. Si le fichier n'existe pas,
     * renvoie un dépôt vide (premier démarrage) plutôt qu'une erreur.
     */
    public static DepotApplication charger(Path source) {
        if (!Files.exists(source) && !Files.exists(EcritureSure.secours(source))) {
            return new DepotApplication();
        }
        // Point 21 : fichier illisible ou tronqué → copie de secours « .bak », signalée.
        return lire(EcritureSure.lireJson(source).document());
    }

    Json ecrire() {
        Json doc = Json.objet();
        doc.set("version", VERSION);
        doc.set("kind", "application");

        Json clubs = Json.tableau();
        for (Club c : clubsParNumero.values()) {
            clubs.add(ApplicationMapper.ecrireClub(c));
        }
        doc.set("clubs", clubs);

        Json comptes = Json.tableau();
        for (CompteOrganisateur c : comptesParEmail.values()) {
            comptes.add(ApplicationMapper.ecrireCompte(c));
        }
        doc.set("comptes", comptes);

        Json comptesUtil = Json.tableau();
        for (CompteUtilisateur c : comptesUtilisateursParEmail.values()) {
            comptesUtil.add(ApplicationMapper.ecrireCompteUtilisateur(c));
        }
        doc.set("comptesUtilisateurs", comptesUtil);

        Json acces = Json.tableau();
        for (AccesClub a : accesParCle.values()) {
            acces.add(ApplicationMapper.ecrireAcces(a));
        }
        doc.set("acces", acces);

        Json agendas = Json.tableau();
        for (AgendaCompte a : agendasParEmail.values()) {
            if (!a.estVide()) {
                agendas.add(ApplicationMapper.ecrireAgenda(a));
            }
        }
        doc.set("agendas", agendas);

        Json pron = Json.objet();
        Json pj = Json.tableau();
        for (PrononciationJoueur p : prononciations.tousLesJoueurs()) {
            pj.add(Json.objet().set("licence", p.licence()).set("prenom", p.prenom()).set("nom", p.nom()));
        }
        pron.set("joueurs", pj);
        Json pc = Json.tableau();
        for (PrononciationClub p : prononciations.tousLesClubs()) {
            pc.add(Json.objet().set("numero", p.numero()).set("nom", p.nom()).set("ipa", p.ipa()));
        }
        pron.set("clubs", pc);
        doc.set("prononciations", pron);

        Json rec = Json.tableau();
        for (ProjetTournoi p : recents) {
            Json o = Json.objet();
            o.set("fichier", p.getFichier().toString());
            o.set("nom", p.getNom());
            o.set("derniereOuverture", p.getDerniereOuverture().toString());
            rec.add(o);
        }
        doc.set("recents", rec);
        return doc;
    }

    static DepotApplication lire(Json doc) {
        int version = doc.getInt("version", 1);
        if (version > VERSION) {
            throw new com.pingmanager.persistence.json.JsonException(
                    "Version de fichier applicatif " + version + " plus récente que celle supportée ("
                            + VERSION + ").");
        }
        DepotApplication depot = new DepotApplication();
        if (doc.has("clubs")) {
            for (Json o : doc.get("clubs").elements()) {
                depot.ajouterClub(ApplicationMapper.lireClub(o));
            }
        }
        if (doc.has("comptes")) {
            for (Json o : doc.get("comptes").elements()) {
                depot.enregistrerCompte(ApplicationMapper.lireCompte(o));
            }
        }
        if (doc.has("comptesUtilisateurs")) {
            for (Json o : doc.get("comptesUtilisateurs").elements()) {
                depot.enregistrerCompteUtilisateur(ApplicationMapper.lireCompteUtilisateur(o));
            }
        }
        if (doc.has("acces")) {
            for (Json o : doc.get("acces").elements()) {
                depot.enregistrerAcces(ApplicationMapper.lireAcces(o));
            }
        }
        if (doc.has("agendas")) { // absent des fichiers antérieurs à l'agenda
            for (Json o : doc.get("agendas").elements()) {
                depot.enregistrerAgenda(ApplicationMapper.lireAgenda(o));
            }
        }
        if (doc.has("prononciations")) { // absent des fichiers antérieurs à B018
            lirePrononciations(doc.get("prononciations"), depot.prononciations);
        }
        if (doc.has("recents")) {
            for (Json o : doc.get("recents").elements()) {
                depot.recents.add(new ProjetTournoi(
                        java.nio.file.Path.of(o.get("fichier").asString()),
                        o.getString("nom", null),
                        java.time.LocalDateTime.parse(o.get("derniereOuverture").asString())));
            }
        }
        return depot;
    }

    /**
     * Relit les prononciations en repassant par la validation : une entrée
     * invalide (fichier modifié à la main) est ignorée plutôt que de risquer un
     * SSML cassé ou d'empêcher le démarrage.
     */
    private static void lirePrononciations(Json o, RegistrePrononciations registre) {
        if (!o.estObjet()) {
            return; // section abîmée : ignorée, le reste du fichier est relu
        }
        for (Json j : entreesObjets(o.get("joueurs"))) {
            try {
                registre.definirJoueur(j.getString("licence", null), j.getString("prenom", ""),
                        j.getString("nom", ""));
            } catch (JsonException e) {
                // champ de mauvais type : entrée ignorée
            }
        }
        for (Json c : entreesObjets(o.get("clubs"))) {
            try {
                registre.definirClub(c.getString("numero", null), c.getString("nom", ""), c.getString("ipa", ""));
            } catch (JsonException e) {
                // champ de mauvais type : entrée ignorée
            }
        }
    }

    /** Éléments objets d'un tableau ; tout le reste (autre type, élément non objet) est ignoré. */
    private static List<Json> entreesObjets(Json tableau) {
        List<Json> objets = new ArrayList<>();
        if (tableau.estTableau()) {
            for (Json e : tableau.elements()) {
                if (e.estObjet()) {
                    objets.add(e);
                }
            }
        }
        return objets;
    }

    // ==================================================================
    // Clés d'index (normalisées comme les entités : numéro trim, email minuscule)
    // ==================================================================

    private static String cleClub(String numero) {
        return numero.trim();
    }

    private static String cleEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String cleAcces(String numeroClub, String email) {
        return cleClub(numeroClub) + "\u0000" + cleEmail(email);
    }
}
