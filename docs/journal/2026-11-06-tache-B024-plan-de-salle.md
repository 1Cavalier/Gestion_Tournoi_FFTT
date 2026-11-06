# Organiser la salle : plusieurs salles, plan des tables (B024) — 2026-11-06

- Branche : `tache/B024-plan-de-salle`
- Agent : `developpeur` (étape de pipeline)
- Pull Request : (complété par l'intégrateur)
- Statut : en attente de l'intégrateur (relecture passée : `verificateur-modele` 0 bloquant, `relecteur-code` 0 bloquant ; points mineurs ci-dessous)

## Pour Quentin

Une fois le tournoi homologué, une carte « Plan de la salle » apparaît sur
l'écran du tournoi. Elle ouvre un éditeur : vous dessinez une ou plusieurs
salles (avec leur étage) sur une grille, vous y posez les tables (2 × 2 cases),
les allées et les repères (table d'arbitrage, entrée, sortie, WC, buvette…),
vous les déplacez, les numérotez, les tournez, ou partez d'un plan type
(1 à 4 rangées). Vous pouvez réserver une table ou une salle entière à un
tableau, ou dire qu'un tableau joue « de préférence » dans une salle : le
bouton « Proposer » du jour J et l'envoi manuel en tiennent compte. Le jour J,
l'onglet Salle montre alors le vrai plan (une mini-carte permet de choisir la
salle), au lieu de la grille habituelle. Un tournoi sans plan ne change pas.

## À tester à l'écran

- Compte de démo : Quentin.
- Chemin : Gestion des tournois → un tournoi homologué ou publié → carte « Plan de la salle » → « Préparer le plan ». Essayer : plan type « 2 rangées », outil « Table » puis clic sur une case, glisser une table, « Sélectionner » puis numéro / « Tourner » / « Réservée au tableau », « Ajouter une salle » (étage 1), cases « Salle réservée aux tableaux », « Enregistrer le plan ».
- Jour J : tournoi de démonstration du jour J, après avoir enregistré un plan → onglet Salle : plan réel, mini-carte des salles, tables hors plan à part.
- Ce qu'il faut regarder (thème clair et sombre) : lisibilité de la grille et des repères, largeur du plan au jour J (7 tables par rangée dépassent 1440 px : défilement horizontal de la page), glisser-déposer dans l'éditeur.
- Captures (harnais `outils-dev/run.sh <dossier> b024 [sombre]`) : `docs/captures/b024/` (carte, éditeur, deuxième salle, salle du jour J). Le glisser-déposer n'a pas pu être vérifié en session cloud.

## Questions pour Quentin

### 2026-11-06 — Numérotation des tables : entiers 1 à N
- Contexte : les matchs et l'ordonnanceur connaissent les tables par un numéro de 1 à N (N = nombre de tables du tournoi). La « numérotation libre » permet de choisir où est la table n° 7, mais pas de l'appeler « D3 » ou « 101 ». Une table du plan au-delà de N ne reçoit aucun match (alerte dans l'éditeur) ; une table de 1 à N absente du plan s'affiche à part le jour J.
- Article FFTT : aucun.
- Options : A) garder des numéros 1 à N ; B) ajouter un libellé libre affiché à côté du numéro ; C) numéros quelconques (l'ordonnanceur devrait accepter une liste de tables).
- Choix provisoire de l'agent : A (le plan ne change pas le nombre de tables du tournoi, qui reste dans « Informations sportives et techniques »).
- Réponse de Quentin :

### 2026-11-06 — Table réservée : réserve la table, pas le tableau
- Contexte : une table réservée au tableau X ne reçoit que des matchs de X ; mais X peut aussi jouer sur les autres tables libres (il commence par les siennes). De même pour une salle réservée.
- Article FFTT : aucun (le JA programme « en fonction des tables disponibles », 3.3.1.2.2 ; clause d'organisateur).
- Options : A) réserver la table seulement ; B) cantonner aussi le tableau à ses tables réservées ; C) choix par réservation.
- Choix provisoire de l'agent : A (ne retarde jamais un tableau).
- Réponse de Quentin :

### 2026-11-06 — Réservations par nom de tableau
- Contexte : une réservation désigne le tableau par son nom. Si le tableau est supprimé ou recréé sous un autre nom, la réservation devient orpheline : elle est signalée dans l'éditeur et sur la carte, et la table (ou la salle) réservée à ce tableau disparu ne reçoit plus aucun match tant qu'elle n'est pas corrigée.
- Article FFTT : aucun.
- Options : A) garder le nom et l'alerte ; B) lever automatiquement les réservations orphelines à l'enregistrement.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Simulation et prévisions sans réservations
- Contexte : `SimulateurTournoi` (B025) simule la journée sans tenir compte des réservations de tables.
- Article FFTT : aucun.
- Options : A) laisser ainsi ; B) appliquer les réservations à la simulation (prévisions plus fidèles quand une salle est réservée).
- Choix provisoire de l'agent : A (à reprendre dans une tâche dédiée si besoin).
- Réponse de Quentin :

## Notes pour l'état du projet

- Modèle : `PlanSalle` (grille, immuable ; `ajouter`, `deplacer`, `retirer`, `remplacer`, `numeroter`, `tourner`, `reserver`, `redimensionner`), `ElementPlan` (scellée : `TablePlan` 2 × 2 avec numéro, orientation et tableau réservé, `AlleePlan` 1 × 1, `ReperePlan` typé), `TypeRepere`, `OrientationTable`, `ResultatPlacementPlan` (motifs HORS_GRILLE, CHEVAUCHEMENT, NUMERO_DEJA_PRIS…), `SalleTournoi` (nom, étage, plan, tableaux réservés, tableaux préférés), `Tournoi.getSalles` / `setSalles` / `aUnPlanDeSalle` (noms et numéros uniques).
- Cœur : `PlansTypesSalle` (1 à 4 rangées, `grilleParDefaut`), `ServicePlanSalle` (salles effectives, `verifier` → `BilanPlanSalles`, `reservations`), `EditionPlanSalles` (brouillon de l'éditeur, numéros uniques toutes salles), `ResultatEditionSalles`. `ServiceSalle` applique les réservations ; nouveau motif `ResultatPropositionSalle.TABLES_LIBRES_RESERVEES`.
- Ordonnancement : `ReservationsTables` (table restreinte = contrainte dure, préférence = souple), `Ordonnanceur.setReservations` ; `proposerLancements` choisit pour chaque match la meilleure table libre (inchangé sans réservation) ; `verifierAffectation` refuse avec `ResultatAffectationTable.Motif.TABLE_RESERVEE` (+ `getTableauxAdmis`).
- Persistance : champ facultatif `salles` du tournoi (`PlanSalleMapper`), écrit seulement s'il y a un plan ; anciens fichiers relus sans plan (une salle implicite en grille) ; relecture tolérante (repère ou orientation inconnus, élément de genre inconnu, salle en double).
- Interface : `EcranPlanSalle` + `PresentationPlanSalle` ; carte « Plan de la salle » dans la deuxième version d'`EcranCompletion` (B024 ajouté à `PARTIES_LIVREES`, bloc « Pour la suite » masqué quand il est vide) ; `EcranSalle` dessine le plan réel si le tournoi en a un ; section `theme.css` « Plan de la salle (B024) » ; mode `b024` du harnais de captures.
- `REGLES_CREATION_TOURNOI.md` : ligne « Salles et plan » ajoutée sous « Tables ».
- Attendu de test modifié (validé d'avance par Quentin) : `PresentationPreparationTournoiTest.remplissage_et_finances_remplace_sa_carte_bientot_apres_acceptation` attend `[]` au lieu de `[B024]`. Aucun autre attendu changé.
- Démo : inchangée (Brunoy et jour J non touchés).
- Points non bloquants relevés par la relecture (à traiter dans une tâche dédiée si besoin) :
  - `verificateur-modele` : une table réservée à un tableau absent (supprimé ou renommé) n'est jamais utilisée par l'ordonnanceur ; seul `ServicePlanSalle.verifier` le signale (éditeur et carte). Voir la question « Réservations par nom de tableau ».
  - `verificateur-modele` : `Tournoi.setSalles(null)` veut dire « sans plan » (le tournoi redevient une salle implicite en grille). Ce comportement n'est pas encore écrit dans la Javadoc de `Tournoi`.
  - `relecteur-code` : la classe `plan-grille-jourj` (`EcranSalle.java`, ligne 427) n'a pas encore de règle dans `theme.css` ; elle ne sert qu'à repérer le plan réel au jour J.

## Fusion

Fusion de `origin/main` sans conflit ; `verifier-fusion.py` : OK.

Attendu modifié : `PresentationPreparationTournoiTest` passe de [B024] à [] (carte « bientôt » retirée). Changement validé explicitement par Quentin avant le développement : ne bloque pas la fusion.

## Chiffres

- Tests : 983 → 1042 (mesurés par `mvn -B test`, 0 échec ; 1022 à la fin du développement, +20 tests limites ajoutés par `verificateur-modele`)
- Après synchronisation avec main : 1043 tests, 0 échec
- Version de la démo : inchangée
