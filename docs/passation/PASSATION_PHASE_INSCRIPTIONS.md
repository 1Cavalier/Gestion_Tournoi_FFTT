# PingManager — passation vers la phase « Inscriptions et publication »

État au 22 octobre 2026, fin de la phase pré-tournoi (brouillon → demande d'homologation → publication). 356 tests verts.

## Où en est le projet
- **Création d'un tournoi complète** : sections Général, Tableaux et inscriptions, Informations sportives et techniques, Complémentaire ; règlement généré (aperçu + PDF, PDFBox 2.0.29, police Inter) ; détail des champs et règles dans `docs/passation/REGLES_CREATION_TOURNOI.md`.
- **Cycle de vie** : `StatutTournoi` BROUILLON → DEMANDE_HOMOLOGATION → HOMOLOGUE → PUBLIE (+ ANNULE). Boutons « Demander l'homologation » / « Publier le tournoi » en bas de la vue d'ensemble (`EcranCompletion`), via `ServiceCycleTournoi.demanderHomologation`, `retirerDemande`, `publierDepuisDemande`.
- **Comptes de démo** (`DonneesDemo`) :
  - club Brunoy 08911132 — `soumet.quentin@gmail.com` / `Soumet123/` (admin niveau 1) ;
  - ultra admin (supervision de tous les tournois) — `admin@pingmanager.fr` / `PingAdmin123/`.
- **Tournoi de référence** : « Tournoi National B de Brunoy 2027 » (`ui.infra.TournoiDemoBrunoy`), samedi 10 et dimanche 11 avril 2027, 157 Route de Brie, 13 tableaux nommés A à M, dotation 3 390 €, juges-arbitres Samuel LOOBUYCK (JA3, principal) et William PRIOU (JA2, adjoint), licences à renseigner ; recréé au démarrage quand la version (`ContexteApplication.VERSION_DEMO`, actuellement `.demo-brunoy-v10`) change.

## Ce qui existe déjà pour les inscriptions (cœur, sans écran)
- `ServiceInscription.inscrire(tournoi, joueur, tableau)` : statut du tournoi (`PolitiqueOuverture`), éligibilité au tableau (points, rang, sexe), **quotas par jour** (`QuotasTableaux` : joueurs / joueuses, tableau exigé pour le 3e des joueuses), **tableau dames obligatoire le jour J**, **pas deux tableaux qui se suivent** (même jour), capacité + liste d'attente. Motifs : `ResultatInscriptionTournoi.Motif` (dont `QUOTA_TABLEAUX_ATTEINT`, `TABLEAU_FEMININ_OBLIGATOIRE`, `TABLEAUX_SUCCESSIFS`, `LISTE_ATTENTE_PLEINE`).
- `RegistreLicences` / `FicheLicence` (date de naissance, catégorie, points) dans la base de démo.

## À prévoir dans la phase suivante
- Écran d'inscription (joueur ou club), lié au tournoi publié ; paiement HelloAsso (lien déjà saisi, `Paiement`).
- **Catégories non admises** (`Tournoi.getCategoriesExclues()`) : à contrôler à l'inscription — `Joueur` n'a pas encore de catégorie d'âge ; la tirer de la licence (date de naissance, II.303).
- **Catégories autorisées par tableau** (`ConfigurationTableau.categorieAutorisee`) : même besoin.
- Dotation en % des inscriptions (`TypeRecompense.PART_INSCRIPTIONS`) : calcul réel une fois les inscriptions encaissées.
- Liste d'attente : appel au pointage (règlement art. Pointage).

## Conventions de code (à garder)
- **Pas de `switch` sur une énumération** (classe annexe `X$1` générée par javac, qui a cassé le build de l'utilisateur) : chaînes de `if` ou `switch` sur `.name()`.
- UI JavaFX : fabrique `Ui` (`champEtiquete`, puces de dépendance, cadres `cadre-ok` / `cadre-manquant` / `cadre-facultatif`), jetons CSS dans `theme.css` (clair / sombre), charte `CHARTE_GRAPHIQUE.md`.
- Règles FFTT citées par article (IX.10x, II.30x…) d'après les PDF du projet.
- Après livraison : `mvn clean javafx:run` chez l'utilisateur.


## Feuille d'engagement générée (29/10/2026)
- `GenerateurFeuilleEngagement.generer(Tournoi)` → `DocumentReglement` ; PDF par `RenduPdfReglement.ecrire(doc, cible, GenerateurFeuilleEngagement.FORMAT_TITRE)`.
- Écran Complémentaire : bouton « Feuille d'engagement (PDF)… » (méthode `ecrireFeuilleEngagement(Path)`), dialogue d'enregistrement commun avec le règlement.
- `RenduPdfReglement` : surcharge avec format de titre, lignes à cellule vide hautes de 24 pt (cases à remplir), pas de puce devant un élément commençant par « □ » (Inter n'a pas « ☐ »).
- Tests : `GenerateurFeuilleEngagementTest` (370 tests verts).
