# Ports d'échange FFTT et import / export par fichier — 2026-11-06

- Branche : `tache/B023-ports-fftt`
- Agent : `developpeur` (étape de pipeline)
- Pull Request : à compléter par l'`integrateur`
- Statut : en cours (étape développeur terminée le 8 novembre 2026)

## Pour Quentin

Le logiciel sait maintenant lire une liste de licenciés et une liste de clubs
fournies par le club (fichiers CSV dans un format PingManager provisoire), et
exporter les résultats d'un tableau terminé (parties et classement) en CSV.
L'import se trouve dans « Importer des joueurs » ; l'export apparaît sous le
podium d'un tableau final entièrement joué. Ce format sera remplacé par celui
de la FFTT dès qu'elle l'aura donné.

## À tester à l'écran

- Chemin 1 : Gestion des tournois → un tournoi → Importer des joueurs → carte
  « 1. Le fichier » → « Licenciés du club (CSV)… » / « Clubs (CSV)… ». Un
  résumé (importés, refusés, cinq premières anomalies) s'affiche dessous.
- Chemin 2 : Jour J → un tableau dont la finale est jouée → Tableau final →
  carte « Podium » → « Exporter les résultats (CSV)… » → choisir un dossier.
- Vérifier la lisibilité du résumé (texte multiligne) en thème clair et sombre.
- Interface non vérifiée visuellement (session cloud).

## Questions pour Quentin

### 2026-11-08 — Conserver les licenciés et clubs importés ?
- Contexte : l'import ne vit que le temps de la session (rien sur disque).
- Article FFTT : RECHERCHE_FFTT A7 (cache 24 h des données fédérales), J5 (certificat = donnée de santé).
- Options : A) session seulement ; B) enregistrer dans le dépôt de l'application avec date de relevé ; C) enregistrer, avec purge automatique.
- Choix provisoire de l'agent : A (prudent, réversible).
- Réponse de Quentin :

### 2026-11-08 — Classement exporté des joueurs éliminés avant les demi-finales
- Contexte : `classements.csv` classe les perdants d'un tour ex aequo au rang « parties du tour + 1 » (5e en quarts, 9e en huitièmes) ; les éliminés en poule ne sont pas classés.
- Article FFTT : chapitre IX muet sur le classement d'un tournoi au-delà du podium.
- Options : A) ainsi ; B) podium seulement ; C) départager (points, tour atteint…).
- Choix provisoire de l'agent : A, documenté dans `docs/FORMAT_ECHANGES_FFTT.md`.
- Réponse de Quentin :

### 2026-11-08 — Clubs sans courriel officiel
- Contexte : le modèle `Club` exige département, ville et courriel officiel ; une ligne de `clubs.csv` sans eux est refusée.
- Options : A) refuser (actuel) ; B) assouplir le modèle pour les clubs importés.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

- Cœur : port `ExportResultatsFederaux` (sans écriture disque), `ServiceResultatsFederaux.construire(Tournoi[, Tableau])` → `ResultatPreparationExport` (motif `MotifRefusExport`, avertissements), objets `ResultatsTournoi`, `PartieExportee`, `ClassementExporte`, `JoueurExporte`, `FichierExporte`. Méthodes par défaut `RegistreLicences.listerParClub`, `RegistreClubs.rechercher`.
- `importation` : `LecteurLicencesCsv`, `RegistreLicencesFichier`, `LecteurClubsCsv`, `RegistreClubsFichier`, `ExportResultatsCsv`, utilitaire `CsvFederal` (lecture bornée, échappement et neutralisation des formules).
- UI : `ui.infra.RegistresImportes` (superposition à la base, session), branché dans `ContexteApplication` ; boutons dans `EcranImportJoueurs` et `VuePhaseFinale`.
- Format documenté : `docs/FORMAT_ECHANGES_FFTT.md`. Persistance inchangée.

## Fusion

À compléter par l'`integrateur`.

## Chiffres

- Tests : 1056 → 1075 (`outils-dev/tests.sh`, Maven inaccessible)
- Version de la démo : inchangée
