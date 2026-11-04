package com.pingmanager.ui;

import com.pingmanager.core.CalendrierTournoi;
import com.pingmanager.core.CatalogueTournois;
import com.pingmanager.core.DemandeVerification;
import com.pingmanager.core.EligibiliteTournoi;
import com.pingmanager.core.Geocodeur;
import com.pingmanager.core.ServiceInscription;
import com.pingmanager.core.ServiceInscriptionEnLigne;
import com.pingmanager.core.Territoires;
import com.pingmanager.model.Coordonnees;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.RegistreLicences;
import com.pingmanager.core.ResultatLiaisonClub;
import com.pingmanager.core.ServiceAuthentification;
import com.pingmanager.core.ServiceLiaisonClub;
import com.pingmanager.core.ServiceLiaisonLicence;
import com.pingmanager.core.Horloge;
import com.pingmanager.core.RegistreDisciplinaire;
import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.FicheLicence;
import com.pingmanager.model.NiveauAcces;
import com.pingmanager.model.Tournoi;
import com.pingmanager.persistence.DepotApplication;
import com.pingmanager.persistence.DepotTournoi;
import com.pingmanager.persistence.ProjetTournoi;
import com.pingmanager.persistence.ServiceFichierTournoi;
import com.pingmanager.ui.infra.DonneesDemo;
import com.pingmanager.ui.infra.TournoisDemo;
import com.pingmanager.ui.infra.EmailConsole;
import com.pingmanager.ui.infra.GenerateurCodeAleatoire;
import com.pingmanager.ui.infra.HachageMotDePassePBKDF2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contexte applicatif : monte toutes les briques (dépôt, ports, services) et
 * tient la <b>session courante</b> (le compte connecté). C'est le point d'entrée
 * unique de l'interface vers le métier — les écrans ne parlent qu'à lui, jamais
 * directement au dépôt ou aux services.
 *
 * <p>Volontairement sans aucune dépendance à JavaFX : c'est le contrôleur de
 * l'application, pas un écran. Il pourrait piloter une autre interface (web) sans
 * changer une ligne.</p>
 *
 * <p>Les données FFTT (clubs, licences) sont ici une <b>base de démonstration</b>
 * en mémoire ({@link DonneesDemo}) : on ne fait que l'interroger. Les comptes,
 * accès et récents, eux, sont <b>persistés</b> dans le dépôt applicatif après
 * chaque modification.</p>
 */
public final class ContexteApplication {

    private final Path dossierDonnees;
    private final Path fichierDepot;
    private final ServiceFichierTournoi service;
    private final DepotApplication depot;

    // Base FFTT (interrogée seulement) — remplacée un jour par la vraie source.
    private final RegistreClubs clubsFFTT;
    private final RegistreLicences licencesFFTT;

    // Ports concrets.
    private final EmailConsole email;

    // Services métier.
    private final ServiceAuthentification authentification;
    private final ServiceLiaisonLicence liaisonLicence;
    private final ServiceLiaisonClub liaisonClub;

    // Onglet Tournois : accès IX.102 et inscription en ligne.
    private final EligibiliteTournoi eligibilite;
    private final ServiceInscriptionEnLigne inscriptionEnLigne;

    // Session.
    private CompteUtilisateur compteConnecte;

    public ContexteApplication() {
        this(ServiceFichierTournoi.dossierDonneesParDefaut(), DonneesDemo.clubs(), DonneesDemo.licences());
    }

    /** Constructeur ouvert (tests, données personnalisées). Prend le dossier de données. */
    public ContexteApplication(Path dossierDonnees, RegistreClubs clubsFFTT, RegistreLicences licencesFFTT) {
        this.dossierDonnees = dossierDonnees;
        this.fichierDepot = dossierDonnees.resolve(DepotApplication.NOM_FICHIER);
        this.clubsFFTT = clubsFFTT;
        this.licencesFFTT = licencesFFTT;

        // Le service de fichiers tournoi porte le dépôt applicatif : une seule
        // instance en mémoire, un seul fichier sur disque (clubs, comptes, accès,
        // tournois récents). L'écouteur est un no-op : l'UI se rafraîchit elle-même.
        this.service = new ServiceFichierTournoi(dossierDonnees, Duration.ofMinutes(15),
                new ServiceFichierTournoi.Ecouteur() { });
        this.depot = service.demarrer();

        this.email = new EmailConsole();
        GenerateurCodeAleatoire code = new GenerateurCodeAleatoire();
        HachageMotDePassePBKDF2 hachage = new HachageMotDePassePBKDF2();
        Horloge horloge = Horloge.systeme();

        this.authentification = new ServiceAuthentification(hachage);
        this.liaisonLicence = new ServiceLiaisonLicence(licencesFFTT);
        this.liaisonClub = new ServiceLiaisonClub(clubsFFTT, code, email, hachage, horloge);
        this.eligibilite = new EligibiliteTournoi(clubsFFTT);
        this.inscriptionEnLigne = new ServiceInscriptionEnLigne(ServiceInscription.sansContrainteSuccession(), eligibilite);

        amorcerProfilDemo();
    }

    /**
     * Pré-charge le profil personnel une seule fois : si le compte n'existe pas
     * encore dans le dépôt, il est créé, sa licence et son club sont rattachés,
     * puis le tout est persisté. Aux démarrages suivants, le compte étant déjà
     * présent, cette méthode ne fait rien.
     */
    private void amorcerProfilDemo() {
        boolean modifie = false;

        // 1) Administrateur par défaut (référent) du club de Brunoy, défini par la base : Serge BOULIER.
        if (!depot.aAcces(DonneesDemo.CLUB_BRUNOY, DonneesDemo.EMAIL_REFERENT_BRUNOY)) {
            depot.enregistrerAcces(new AccesClub(
                    DonneesDemo.CLUB_BRUNOY, DonneesDemo.EMAIL_REFERENT_BRUNOY,
                    DonneesDemo.REFERENT_BRUNOY_NOM, DonneesDemo.REFERENT_BRUNOY_PRENOM,
                    NiveauAcces.ADMIN_PRINCIPAL));
            modifie = true;
        }

        // 2) Profil personnel, créé une seule fois (mot de passe haché à la création).
        CompteUtilisateur quentin = depot.trouverCompteUtilisateur(DonneesDemo.EMAIL_QUENTIN).orElse(null);
        if (quentin == null) {
            Optional<CompteUtilisateur> compte = authentification.creerCompte(
                    DonneesDemo.EMAIL_QUENTIN, "SOUMET", "Quentin", "Soumet123/",
                    depot.emailUtilisateurDejaUtilise());
            if (compte.isPresent()) {
                quentin = compte.get();
                liaisonLicence.lier(quentin, DonneesDemo.LICENCE_QUENTIN);
                depot.enregistrerCompteUtilisateur(quentin);
                // Quentin est administrateur (niveau 1) de Brunoy — le référent est Serge.
                depot.enregistrerAcces(new AccesClub(
                        DonneesDemo.CLUB_BRUNOY, quentin.getEmail(), "SOUMET", "Quentin",
                        NiveauAcces.ADMIN_NIVEAU_1));
                modifie = true;
            }
        } else {
            // Correction d'anciennes données : si Quentin était principal de Brunoy,
            // le ramener à niveau 1 (le référent est désormais Serge).
            AccesClub a = depot.accesDe(quentin.getEmail()).stream()
                    .filter(x -> x.getNumeroClub().equals(DonneesDemo.CLUB_BRUNOY))
                    .findFirst().orElse(null);
            if (a != null && a.estPrincipal()) {
                depot.enregistrerAcces(new AccesClub(
                        DonneesDemo.CLUB_BRUNOY, quentin.getEmail(), "SOUMET", "Quentin",
                        NiveauAcces.ADMIN_NIVEAU_1));
                modifie = true;
            }
        }

        // 3) Compte « ultra admin » (supervision), créé une seule fois, sans club.
        if (depot.trouverCompteUtilisateur(DonneesDemo.EMAIL_ULTRA_ADMIN).isEmpty()) {
            Optional<CompteUtilisateur> admin = authentification.creerCompte(
                    DonneesDemo.EMAIL_ULTRA_ADMIN, "SUPERVISION", "PingManager", DonneesDemo.MDP_ULTRA_ADMIN,
                    depot.emailUtilisateurDejaUtilise());
            if (admin.isPresent()) {
                depot.enregistrerCompteUtilisateur(admin.get());
                modifie = true;
            }
        }

        if (modifie) {
            sauvegarder();
        }
        nettoyerTournoisBrunoy();
        rafraichirTournoiDemo();
        amorcerTournoiDemo();
        amorcerAutresTournoisDemo();
    }

    /**
     * Version des données du National B de Brunoy. Quand elle change (nouvelles
     * décisions du club), le tournoi est recréé une fois au démarrage suivant.
     * v3 (21/10/2026, 23 h) : dotations (Non numérotés 200/100/50, ¼ de finalistes à 10 €),
     * buvette, textes du règlement. v4 : tableau dames en part des inscriptions, dotation
     * « peut être réduite » sous 50 % de remplissage. v5 : poussins non admis. v6 : tableaux nommés par lettre (A à K).
     * v7 (26/10/2026) : règlement validé par le club (10-11 avril, 157 Route de Brie, 3 390 €, pas de JA).
     * v8 : une lettre par tableau (A à M), Non numérotés hors n° 1 à 999.
     * v9 (29/10/2026) : 10 € en ligne pour les tableaux ≤ 1900, ≤ 2000 et Non numérotés.
     * v10 : juges-arbitres désignés.
     * v11 (02/11/2026) : Brunoy publié (n° d'homologation fictif) et rempli par des joueurs
     * fictifs ; cinq autres tournois publiés pour la carte de l'onglet Tournois.
     * v12 (02/11/2026, midi) : tableau D de Créteil complet avec liste d'attente (essai de
     * l'inscription en attente).
     */
    private static final String VERSION_DEMO = ".demo-brunoy-v12";

    private void rafraichirTournoiDemo() {
        Path marqueur = dossierDonnees.resolve("tournois").resolve(VERSION_DEMO);
        if (java.nio.file.Files.exists(marqueur)) {
            return;
        }
        try {
            java.nio.file.Files.createDirectories(marqueur.getParent());
            java.nio.file.Files.deleteIfExists(fichierTournoiDemo()); // recréé juste après par amorcerTournoiDemo
            for (String nom : NOMS_AUTRES_DEMOS) {
                java.nio.file.Files.deleteIfExists(fichierAutreDemo(nom));
            }
            java.nio.file.Files.writeString(marqueur, "Tournoi de démo recréé le " + java.time.LocalDate.now() + "\n");
        } catch (java.io.IOException e) {
            System.err.println("Mise à jour du tournoi de Brunoy non faite : " + e.getMessage());
        }
    }

    /** Marqueur du nettoyage décidé à la réunion du 21 octobre 2026 (fait une seule fois). */
    private static final String MARQUEUR_NETTOYAGE = ".nettoyage-brunoy-2026-10-21";

    /**
     * Une seule fois : sur le compte de Brunoy, ne garde que le tournoi complet
     * (National B 2027, recréé avec les décisions de la réunion). Les autres tournois
     * du club ne sont pas supprimés : leurs fichiers partent dans {@code tournois/archives}
     * et ils sortent de la liste.
     */
    private void nettoyerTournoisBrunoy() {
        Path dossier = dossierDonnees.resolve("tournois");
        Path marqueur = dossier.resolve(MARQUEUR_NETTOYAGE);
        if (java.nio.file.Files.exists(marqueur)) {
            return;
        }
        try {
            java.nio.file.Files.createDirectories(dossier);
            Path demo = fichierTournoiDemo().toAbsolutePath();
            Path archives = dossier.resolve("archives");
            for (Path f : fichiersTournois()) {
                if (f.toAbsolutePath().equals(demo)) {
                    continue;
                }
                Tournoi t;
                try {
                    t = DepotTournoi.charger(f).getTournoi();
                } catch (RuntimeException illisible) {
                    continue;
                }
                if (!estDeBrunoy(t)) {
                    continue;
                }
                depot.oublierRecent(f);
                if (f.toAbsolutePath().startsWith(dossier.toAbsolutePath())) {
                    java.nio.file.Files.createDirectories(archives);
                    java.nio.file.Files.move(f, archives.resolve(f.getFileName()),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            // Le tournoi complet est recréé avec les nouvelles informations.
            java.nio.file.Files.deleteIfExists(fichierTournoiDemo());
            sauvegarder();
            java.nio.file.Files.writeString(marqueur, "Nettoyage fait le " + java.time.LocalDate.now() + "\n");
        } catch (java.io.IOException e) {
            System.err.println("Nettoyage des tournois de Brunoy non fait : " + e.getMessage());
        }
    }

    private static boolean estDeBrunoy(Tournoi t) {
        return DonneesDemo.CLUB_BRUNOY.equals(t.getNumeroAffiliationClub())
                || "Brunoy".equalsIgnoreCase(String.valueOf(t.getClubOrganisateur()));
    }

    /**
     * Tous les fichiers de tournoi connus : ceux du dossier {@code tournois} (hors
     * archives) et ceux de la liste des récents, sans doublon.
     */
    public List<Path> fichiersTournois() {
        java.util.LinkedHashSet<Path> res = new java.util.LinkedHashSet<>();
        service.recents().forEach(p -> res.add(p.getFichier().toAbsolutePath()));
        Path dossier = dossierDonnees.resolve("tournois");
        if (java.nio.file.Files.isDirectory(dossier)) {
            try (java.util.stream.Stream<Path> s = java.nio.file.Files.list(dossier)) {
                s.filter(p -> p.getFileName().toString().endsWith(DepotTournoi.EXTENSION))
                        .sorted()
                        .forEach(p -> res.add(p.toAbsolutePath()));
            } catch (java.io.IOException ignore) {
                // dossier illisible : on garde les récents
            }
        }
        res.removeIf(p -> !java.nio.file.Files.isRegularFile(p));
        return new ArrayList<>(res);
    }

    /** @return vrai si le compte connecté est un compte « ultra admin » (supervision). */
    public boolean estUltraAdmin() {
        return compteConnecte != null && DonneesDemo.ULTRA_ADMINS.contains(compteConnecte.getEmail());
    }

    /**
     * Le tournoi appartient-il à l'un des clubs du compte connecté ? (par n°
     * d'affiliation, ou par nom de club pour les anciens fichiers sans numéro).
     */
    public boolean estTournoiDuCompte(Tournoi t) {
        for (AccesClub a : accesCourants()) {
            if (a.getNumeroClub().equals(t.getNumeroAffiliationClub())) {
                return true;
            }
            if (t.getNumeroAffiliationClub() == null) {
                String nom = clubParNumero(a.getNumeroClub()).map(Club::getNom).orElse(null);
                if (nom != null && nom.equalsIgnoreCase(String.valueOf(t.getClubOrganisateur()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Fichier du brouillon de démo « Tournoi National B de Brunoy 2027 ». */
    public Path fichierTournoiDemo() {
        return dossierDonnees.resolve("tournois").resolve("Tournoi_National_B_Brunoy_2027-demo" + DepotTournoi.EXTENSION);
    }

    /**
     * Crée une seule fois le brouillon de démo du National B de Brunoy
     * ({@link TournoiDemoBrunoy}), toutes sections remplies. S'il existe déjà
     * (éventuellement modifié par l'utilisateur), il n'est pas touché ; pour
     * repartir de la version d'origine, supprimer son fichier.
     */
    private void amorcerTournoiDemo() {
        Path cible = fichierTournoiDemo();
        boolean dejaListe = service.recents().stream()
                .anyMatch(p -> p.getFichier().equals(cible.toAbsolutePath()));
        if (java.nio.file.Files.exists(cible) && dejaListe) {
            return;
        }
        try {
            java.nio.file.Files.createDirectories(cible.getParent());
            Tournoi t = java.nio.file.Files.exists(cible) ? DepotTournoi.charger(cible).getTournoi()
                    : TournoisDemo.brunoyPublieEtRempli(DonneesDemo.JOUEURS_FICTIFS);
            service.nouveau(t, new RegistreDisciplinaire());
            service.enregistrerSous(t, new RegistreDisciplinaire(), cible);
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("Brouillon de démo non créé : " + e.getMessage());
        }
    }

    /** Noms de fichier des autres tournois publiés de démo (carte de l'onglet Tournois). */
    private static final List<String> NOMS_AUTRES_DEMOS = List.of("Demo_Regional_Creteil_2026",
            "Demo_Departemental_Loiret_2027", "Demo_National_A_Lyon_2027", "Demo_Regional_Lille_2027",
            "Demo_Promotionnel_Rennes_2026");

    private Path fichierAutreDemo(String nom) {
        return dossierDonnees.resolve("tournois").resolve(nom + "-demo" + DepotTournoi.EXTENSION);
    }

    /** Crée une seule fois les autres tournois publiés de démo (fichiers absents seulement). */
    private void amorcerAutresTournoisDemo() {
        if (NOMS_AUTRES_DEMOS.stream().allMatch(n -> Files.exists(fichierAutreDemo(n)))) {
            return;
        }
        try {
            Map<String, Tournoi> demos = TournoisDemo.autres(DonneesDemo.JOUEURS_FICTIFS, clubsFFTT,
                    licencesFFTT.trouverParLicence(DonneesDemo.LICENCE_QUENTIN).orElse(null));
            for (Map.Entry<String, Tournoi> e : demos.entrySet()) {
                Path f = fichierAutreDemo(e.getKey());
                if (!Files.exists(f)) {
                    Files.createDirectories(f.getParent());
                    DepotTournoi.sauvegarder(e.getValue(), f);
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            System.err.println("Tournois de démo non créés : " + e.getMessage());
        }
    }

    // ==================================================================
    // Authentification / session
    // ==================================================================

    /**
     * Crée un compte puis connecte immédiatement la personne.
     *
     * @return le compte créé, ou {@link Optional#empty()} si l'email est déjà pris
     */
    public Optional<CompteUtilisateur> creerCompteEtConnecter(String email, String nom,
                                                              String prenom, String motDePasse) {
        Optional<CompteUtilisateur> compte = authentification.creerCompte(
                email, nom, prenom, motDePasse, depot.emailUtilisateurDejaUtilise());
        compte.ifPresent(c -> {
            depot.enregistrerCompteUtilisateur(c);
            sauvegarder();
            this.compteConnecte = c;
        });
        return compte;
    }

    /**
     * Connecte une personne existante.
     *
     * @return le compte si identifiants valides, sinon {@link Optional#empty()}
     */
    public Optional<CompteUtilisateur> connecter(String email, String motDePasse) {
        Optional<CompteUtilisateur> compte = authentification.connecter(
                email, motDePasse, depot::trouverCompteUtilisateur);
        compte.ifPresent(c -> this.compteConnecte = c);
        return compte;
    }

    public void deconnecter() {
        this.compteConnecte = null;
    }

    public boolean estConnecte() {
        return compteConnecte != null;
    }

    public CompteUtilisateur compteConnecte() {
        return compteConnecte;
    }

    // ==================================================================
    // Démarche 1 — liaison licence
    // ==================================================================

    /**
     * Recherche la licence et, si elle existe, la lie au compte connecté.
     *
     * @return la fiche de licence si trouvée, sinon {@link Optional#empty()}
     */
    public Optional<FicheLicence> lierLicence(String numeroLicence) {
        exigerConnecte();
        Optional<FicheLicence> fiche = liaisonLicence.lier(compteConnecte, numeroLicence);
        if (fiche.isPresent()) {
            depot.enregistrerCompteUtilisateur(compteConnecte);
            sauvegarder();
        }
        return fiche;
    }

    /** @return la fiche de la licence liée au compte (pour l'affichage), si connue. */
    public Optional<FicheLicence> ficheLicenceLiee() {
        if (compteConnecte == null || !compteConnecte.aLicenceLiee()) {
            return Optional.empty();
        }
        return licencesFFTT.trouverParLicence(compteConnecte.getLicenceLiee());
    }

    // ==================================================================
    // Démarche 2 — liaison club
    // ==================================================================

    /**
     * Lance la démarche de liaison à un club : envoie le code à l'email officiel.
     *
     * @return la demande de vérification à conserver, ou {@link Optional#empty()}
     *         si le club est introuvable
     */
    public Optional<DemandeVerification> demanderLiaisonClub(String numeroClub) {
        exigerConnecte();
        return liaisonClub.demander(compteConnecte, numeroClub);
    }

    /**
     * Confirme le code de liaison à un club ; en cas de succès, enregistre l'accès.
     */
    public ResultatLiaisonClub confirmerLiaisonClub(String numeroClub,
                                                    DemandeVerification demande, String code) {
        exigerConnecte();
        ResultatLiaisonClub resultat = liaisonClub.confirmer(compteConnecte, numeroClub, demande, code);
        if (resultat.estReussi()) {
            depot.enregistrerAcces(resultat.getAcces());
            sauvegarder();
        }
        return resultat;
    }

    /**
     * Dissocie le compte connecté d'un club (retire l'habilitation).
     *
     * <p>Cas particulier : l'<b>administrateur par défaut</b> (le référent, niveau
     * principal, défini par la base fédérale) ne peut pas se dissocier — son
     * statut ne vient pas d'un rattachement volontaire. La méthode refuse alors
     * l'opération.</p>
     *
     * @return {@code true} si la dissociation a eu lieu, {@code false} si elle est
     *         refusée (accès inexistant ou référent)
     */
    public boolean dissocierClub(String numeroClub) {
        exigerConnecte();
        AccesClub acces = depot.accesDe(compteConnecte.getEmail()).stream()
                .filter(a -> a.getNumeroClub().equals(numeroClub))
                .findFirst().orElse(null);
        if (acces == null || acces.estPrincipal()) {
            return false;
        }
        depot.retirerAcces(numeroClub, compteConnecte.getEmail());
        sauvegarder();
        return true;
    }

    /** @return les habilitations (accès club) du compte connecté. */
    public List<AccesClub> accesCourants() {
        if (compteConnecte == null) {
            return List.of();
        }
        return depot.accesDe(compteConnecte.getEmail());
    }

    /** @return toutes les personnes habilitées sur un club (l'équipe de gestion). */
    public List<AccesClub> membresDuClub(String numeroClub) {
        List<AccesClub> res = new ArrayList<>();
        if (numeroClub == null) {
            return res;
        }
        for (AccesClub a : depot.tousLesAcces()) {
            if (numeroClub.equals(a.getNumeroClub())) {
                res.add(a);
            }
        }
        return res;
    }

    /** @return true si le compte connecté administre au moins un club. */
    public boolean estOrganisateur() {
        return !accesCourants().isEmpty();
    }

    public Optional<Club> clubParNumero(String numeroClub) {
        return clubsFFTT.trouverParNumero(numeroClub);
    }

    /** @return les licenciés correspondant à une saisie (nom, prénom ou numéro). */
    public List<FicheLicence> rechercherLicences(String texte, int max) {
        return licencesFFTT.rechercher(texte, max);
    }

    /** @return les clubs (base FFTT) que le compte connecté administre. */
    public List<Club> clubsGeres() {
        List<Club> res = new ArrayList<>();
        for (AccesClub a : accesCourants()) {
            clubParNumero(a.getNumeroClub()).ifPresent(res::add);
        }
        return res;
    }

    /**
     * Correspondant proposé par défaut pour un tournoi du club : l'administrateur
     * principal s'il a un compte, sinon le responsable déclaré dans la base FFTT
     * (avec l'email officiel du club), sinon le compte connecté.
     *
     * @return {nom, prénom, email} — jamais {@code null}, éléments éventuellement vides
     */
    public String[] correspondantParDefaut(String numeroClub) {
        for (AccesClub a : membresDuClub(numeroClub)) {
            if (a.estPrincipal()) {
                return new String[] {a.getNom(), a.getPrenom(), a.getEmail()};
            }
        }
        Optional<Club> club = clubParNumero(numeroClub);
        if (club.isPresent() && club.get().getResponsableNom() != null) {
            Club c = club.get();
            return new String[] {c.getResponsableNom(), c.getResponsablePrenom(), c.getEmailOfficiel()};
        }
        CompteUtilisateur moi = compteConnecte;
        return moi == null ? new String[] {null, null, null}
                : new String[] {moi.getNom(), moi.getPrenom(), moi.getEmail()};
    }

    // ==================================================================
    // Tournois (gestion)
    // ==================================================================

    /** @return la liste des tournois récents (du plus récent au plus ancien). */
    public List<ProjetTournoi> tournoisRecents() {
        return service.recents();
    }

    /** Charge le tournoi d'un fichier (pour lire son statut et ses détails). */
    public Tournoi chargerTournoi(Path fichier) {
        return DepotTournoi.charger(fichier).getTournoi();
    }

    /**
     * Crée un nouveau tournoi au stade brouillon et l'enregistre immédiatement
     * (fichier dédié + ajout aux récents). Le reste du dossier se complète ensuite.
     *
     * @return le chemin du brouillon créé
     */
    public Path creerBrouillon(Tournoi tournoi) {
        service.nouveau(tournoi, new RegistreDisciplinaire());
        Path cible = cibleBrouillon(tournoi);
        service.enregistrerSous(tournoi, new RegistreDisciplinaire(), cible);
        return cible;
    }

    /** Enregistre les modifications d'un tournoi déjà sur disque. */
    public void enregistrerTournoi(Tournoi tournoi, Path fichier) {
        service.enregistrerSous(tournoi, new RegistreDisciplinaire(), fichier);
    }

    private Path cibleBrouillon(Tournoi tournoi) {
        String base = (tournoi.getNom() == null || tournoi.getNom().isBlank())
                ? "tournoi"
                : tournoi.getNom().trim().replaceAll("[^a-zA-Z0-9-_ ]", "").replace(' ', '_');
        return dossierDonnees.resolve("tournois")
                .resolve(base + "-" + System.currentTimeMillis() + DepotTournoi.EXTENSION);
    }

    // ==================================================================
    // Onglet Tournois : catalogue, inscription en ligne, « Mes tournois »
    // ==================================================================

    /** Un tournoi et son fichier. */
    public record TournoiFichier(Path fichier, Tournoi tournoi) {
    }

    /** @return tous les tournois lisibles (hors archives), avec leur fichier. */
    public List<TournoiFichier> tousLesTournois() {
        List<TournoiFichier> res = new ArrayList<>();
        for (Path f : fichiersTournois()) {
            try {
                res.add(new TournoiFichier(f, chargerTournoi(f)));
            } catch (RuntimeException illisible) {
                // fichier corrompu ou d'un autre format : ignoré
            }
        }
        return res;
    }

    /**
     * Catalogue de l'onglet Tournois : tournois publiés à venir ou en cours, décrits pour
     * la licence liée (accès IX.102, distance au club, tableaux à ses points…).
     * L'identifiant de chaque entrée est le chemin absolu du fichier.
     */
    public List<CatalogueTournois.Entree> catalogue(List<TournoiFichier> tournois) {
        Map<String, Tournoi> m = new java.util.LinkedHashMap<>();
        for (TournoiFichier tf : tournois) {
            m.put(tf.fichier().toString(), tf.tournoi());
        }
        return CatalogueTournois.construire(m, ficheLicenceLiee().orElse(null), positionDuJoueur(),
                eligibilite, Geocodeur.centroideDuDepartement(), java.time.LocalDate.now());
    }

    /**
     * Position de référence du joueur pour la distance : préfecture du département de
     * son club (approximation, sans appel réseau).
     */
    public Coordonnees positionDuJoueur() {
        Optional<FicheLicence> f = ficheLicenceLiee();
        if (f.isEmpty()) {
            return null;
        }
        return eligibilite.departementDuJoueur(f.get()).map(Territoires.Departement::prefecture).orElse(null);
    }

    public EligibiliteTournoi eligibilite() {
        return eligibilite;
    }

    public Optional<FicheLicence> ficheLicence(String numero) {
        return licencesFFTT.trouverParLicence(numero);
    }

    /** Essai à blanc d'une sélection de tableaux pour la licence liée (validation en direct). */
    public ServiceInscriptionEnLigne.Bilan simulerInscription(Tournoi t, java.util.Collection<Tableau> selection) {
        return inscriptionEnLigne.simuler(t, t == null ? null : ficheLicenceLiee().orElse(null), selection,
                java.time.LocalDateTime.now());
    }

    /** Motif empêchant toute inscription de la licence liée à ce tournoi, ou {@code null}. */
    public String refusInscription(Tournoi t) {
        return inscriptionEnLigne.refusGlobal(t, ficheLicenceLiee().orElse(null), java.time.LocalDateTime.now());
    }

    /**
     * Inscrit la licence liée aux tableaux choisis (tout ou rien) et enregistre le
     * tournoi. Le paiement reste « à régler » (phase de paiement ultérieure).
     */
    public ServiceInscriptionEnLigne.Bilan inscrireEnLigne(Path fichier, Tournoi t, java.util.Collection<Tableau> selection) {
        ServiceInscriptionEnLigne.Bilan b = inscriptionEnLigne.inscrire(t, ficheLicenceLiee().orElse(null), selection,
                java.time.LocalDateTime.now());
        if (b.estValide()) {
            DepotTournoi.sauvegarder(t, new RegistreDisciplinaire(), fichier);
        }
        return b;
    }

    /** Tournois où la licence liée est inscrite ou en liste d'attente (tous statuts sauf brouillon). */
    public List<TournoiFichier> mesTournoisJoueur(List<TournoiFichier> tous) {
        List<TournoiFichier> res = new ArrayList<>();
        Optional<FicheLicence> f = ficheLicenceLiee();
        if (f.isEmpty()) {
            return res;
        }
        for (TournoiFichier tf : tous) {
            if (com.pingmanager.core.RemplissageTableaux.participant(tf.tournoi(), f.get().getNumeroLicence()).isPresent()) {
                res.add(tf);
            }
        }
        return res;
    }

    /** Tournois organisés par les clubs du compte (tous statuts). */
    public List<TournoiFichier> mesTournoisClub(List<TournoiFichier> tous) {
        List<TournoiFichier> res = new ArrayList<>();
        for (TournoiFichier tf : tous) {
            if (estTournoiDuCompte(tf.tournoi())) {
                res.add(tf);
            }
        }
        return res;
    }

    // ==================================================================
    // Accès aux briques (pour l'UI)
    // ==================================================================

    /** @return le dernier email « envoyé » (démo), pour l'afficher à l'écran. */
    public EmailConsole email() {
        return email;
    }

    public String memoDemo() {
        return DonneesDemo.memo();
    }

    /** Ferme proprement l'application : persiste le dépôt et arrête l'autosave. */
    public void fermer() {
        service.close();
    }

    // ==================================================================
    // Interne
    // ==================================================================

    /** Persiste le dépôt applicatif (écriture atomique). */
    public void sauvegarder() {
        try {
            Files.createDirectories(fichierDepot.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier de données " + fichierDepot.getParent(), e);
        }
        depot.sauvegarder(fichierDepot);
    }

    public Path fichierDepot() {
        return fichierDepot;
    }

    private void exigerConnecte() {
        if (compteConnecte == null) {
            throw new IllegalStateException("Aucun compte connecté.");
        }
    }
}
