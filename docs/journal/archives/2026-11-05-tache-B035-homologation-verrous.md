# B035 : homologation, verrous et refus — 2026-11-05

- Branche : `tache/B035-homologation-verrous`
- Agent : developpeur (cœur et persistance), developpeur-interface (écrans), designer-interface (charte, captures), verificateur-modele, correcteur, relecteur-code, documentaliste
- Pull Request : à créer (étape `integrateur`)
- Statut : prête pour l'étape `integrateur` (cœur, écrans, charte et tests faits ; validation à l'écran par Quentin à faire)

## Pour Quentin

Dès que l'homologation est demandée, les informations du dossier ne peuvent plus être modifiées : nom, catégorie, club, adresse, dates, correspondant, juges-arbitres, tableaux (ajout, retrait, nom, points, public, jour et heure). Le logiciel refuse et dit pourquoi (« Retirez la demande pour le modifier »). Les champs grisés affichent un cadenas et un bandeau avec le motif.

Le reste (numéro d'homologation, places, tarifs, récompenses, pointage, tables, balles…) reste modifiable.

Une demande refusée par la ligue ou la FFTT se saisit avec son motif : le tournoi revient au brouillon, et la date et le motif restent visibles. Acceptée, elle fait passer le tournoi à « homologué ».

Après homologation, la vue du tournoi change : le parcours en 4 étapes est relié, et trois cartes « bientôt » (Salle, Stratégie d'ordonnancement, Remplissage et finances) sont en haut. Le règlement est en bas, en lecture.

L'import de joueurs ne crée plus de tableaux sur un tournoi verrouillé.

## À tester à l'écran

- Compte de démo : « Tournoi National B de Brunoy 2027 » (à recréer si la démo n'a pas été relancée depuis ; la version de la démo n'a pas changé).
- Chemin : Gestion des tournois → Brunoy 2027 → vue de préparation (Général, Tableaux, Juges-arbitres). Chemin exact à confirmer à l'écran.
- Ce qu'il faut regarder (thème clair et sombre) :
  - Brouillon : après un refus, la date et le motif apparaissent en haut de la vue.
  - Demande d'homologation : les champs verrouillés sont grisés, lisibles (liste déroulante comprise), avec le cadenas et le bandeau du motif. Le bouton « Homologation refusée » demande un motif avant de valider.
  - Tableaux et juges-arbitres : on ne peut pas ajouter, retirer ni renommer. Le numéro d'homologation, les tarifs et les places restent modifiables.
  - Homologué : la vue du tournoi montre la deuxième version (parcours relié, trois cartes « bientôt », règlement en bas).
- Captures de référence (1440 px) : `docs/captures/B035/apres/` (brouillon refusé, demande, saisie du refus, général verrouillé, homologué haut / milieu / bas). Harnais : `outils-dev/run.sh <dossier> b035 [sombre]`.
- Non vérifié : rendu sous Windows, mise à l'échelle différente de 100 %.

## Questions pour Quentin

### 2026-11-05 — Juge-arbitre après la demande d'homologation
- Contexte : le juge-arbitre fait partie du dossier (IX.104). Après la demande, on ne peut plus ajouter, retirer ni changer le nom, le grade ou le rôle d'un JA. Sa licence, son email et son téléphone restent modifiables (à Brunoy, les licences sont encore « à renseigner »). Un JA malade ne peut donc être remplacé qu'en retirant la demande.
- Article FFTT : IX.104 (grade exigé) ; le règlement ne dit rien du changement de JA après homologation.
- Options : A) tout verrouiller sauf licence et contact (choix actuel) ; B) laisser changer le JA si le grade reste conforme.
- Choix provisoire de l'agent : A, le plus prudent.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Tableaux après la demande d'homologation
- Contexte : ajouter ou retirer un tableau, ou changer son nom, ses points, son public ou son horaire, est refusé dès la demande. Format, places, tarif, récompenses et heures de pointage restent libres.
- Article FFTT : aucun (choix de l'application, la grille 🔴 sert de référence).
- Options : A) verrouillage complet de ces champs (choix actuel) ; B) permettre un décalage d'horaire le même jour après homologation.
- Choix provisoire de l'agent : A.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Tournoi annulé
- Contexte : comme pour le règlement figé (B010), un tournoi annulé n'est plus verrouillé.
- Article FFTT : aucun.
- Options : A) garder les verrous après annulation ; B) déverrouiller (choix actuel, cohérent avec B010).
- Choix provisoire de l'agent : B.
- Réponse de Quentin : à compléter.

## Notes pour l'état du projet

- Cœur : `model.ChampHomologation` (champs du dossier 🔴), `model.RefusHomologation` (date, motif) ; `Tournoi.champsHomologationVerrouilles()`, `getRefusHomologation()`, `getDernierRefusHomologation()`, `remplacerJugeArbitre(licence, nouveau)`.
- `core.ServiceVerrousHomologation` : lecture (`champsVerrouilles`, `estVerrouille`, `motifVerrou`, `verifier`) et modifications gardées (`modifierNom`, `modifierCategorie`, `modifierClubOrganisateur`, `modifierNumeroAffiliation`, `modifierAdresse`, `modifierPeriode`, `modifierCorrespondant`, `ajouter/retirer/remplacerJugeArbitre`, `ajouter/retirer/remplacerTableau`) → `core.ResultatModificationTournoi` (`APPLIQUEE` / `CHAMP_VERROUILLE`, champs, message).
- `ServiceCycleTournoi.refuserHomologation(tournoi, motif)` : demande → brouillon ; `ResultatTransition.Motif.MOTIF_REFUS_ABSENT` si le motif est vide ; constructeur avec `Horloge`.
- Import : `ImportJoueurs` ne crée plus de tableaux sur un tournoi verrouillé (`planifier` et garde dans `appliquer`).
- Format de sauvegarde : champ facultatif `refusHomologation` (liste `{date, motif}`), absent des anciens fichiers. Relecture des anciens fichiers couverte par `RoundTripTournoiTest`.
- Écrans : `PresentationPreparationTournoi` (logique, 15 tests), `Ui.verrouiller` / `bandeauVerrou` / `carteBientot`, section CSS « B035 » dans `theme.css`. Les formulaires `FormulairesSection` et `FormulaireTableau`, ainsi que `EcranCompletion` et `EcranTableaux`, passent par ces verrous.
- Les setters du modèle ne lèvent pas d'exception (contrairement à B010) : les écrans passent par `ServiceVerrousHomologation`. Choix prudent pour ne pas casser les formulaires, la démo et le géocodage existants.
- Défauts restants, non bloquants :
  - `ServiceCycleTournoi.annuler(tournoi)` renvoie `void`, alors que les transitions voisines renvoient un `ResultatTransition` (asymétrie relevée par le vérificateur).
  - La classe `cartes-a-venir` (conteneur des cartes « bientôt ») n'a pas de règle CSS ; la carte `carte-bientot` est stylée.
  - Rendu Windows et mise à l'échelle non vérifiés.

## Fusion

À compléter par l'`integrateur`.

## Chiffres

- Tests : 697 → 879 (879 rapportés par le correcteur, 0 échec ; à reconfirmer par `mvn -B test` lors de la fusion)
- Version de la démo : inchangée
