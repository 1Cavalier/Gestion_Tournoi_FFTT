# B035 : homologation, verrous et refus — 2026-11-05

- Branche : `tache/B035-homologation-verrous`
- Agent : developpeur (cœur et persistance) ; l'écran suit (developpeur-interface)
- Pull Request : à créer (étape `integrateur`)
- Statut : en cours (cœur fait, écran à faire)

## Pour Quentin

Dès que l'homologation est demandée, les informations du dossier (nom,
catégorie, club, adresse, dates, correspondant, juges-arbitres, tableaux et
leurs nom, points, public, jour et heure) ne peuvent plus être modifiées : le
logiciel le refuse et dit pourquoi (« Retirez la demande pour le modifier »).
Le reste (numéro d'homologation, places, tarifs, récompenses, pointage, tables,
balles…) reste modifiable. Une demande refusée par la ligue ou la FFTT
s'enregistre avec son motif : le tournoi revient au brouillon et le motif reste
visible. Acceptée, elle fait passer le tournoi à « homologué » comme avant.

## À tester à l'écran

À compléter par l'agent de l'écran (cœur seul pour l'instant).

## Questions pour Quentin

### 2026-11-05 — Juge-arbitre après la demande d'homologation
- Contexte : le juge-arbitre fait partie du dossier (IX.104). Après la demande,
  on ne peut plus ajouter, retirer ni changer le nom, le grade ou le rôle d'un
  JA. Sa licence, son email et son téléphone restent modifiables (à Brunoy, les
  licences sont encore « à renseigner »). Un JA malade ne peut donc être
  remplacé qu'en retirant la demande.
- Article FFTT : IX.104 (grade exigé), le règlement ne dit rien du changement de JA après homologation.
- Options : A) tout verrouiller sauf licence et contact (choix actuel) ; B) laisser changer le JA si le grade reste conforme.
- Choix provisoire de l'agent : A, le plus prudent.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Tableaux après la demande d'homologation
- Contexte : ajouter ou retirer un tableau, ou changer son nom, ses points, son
  public ou son horaire, est refusé dès la demande. Format, places, tarif,
  récompenses et heures de pointage restent libres.
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

- `model.ChampHomologation` (champs du dossier 🔴), `model.RefusHomologation`
  (date, motif) ; `Tournoi.champsHomologationVerrouilles()`,
  `getRefusHomologation()`, `getDernierRefusHomologation()`,
  `remplacerJugeArbitre(licence, nouveau)`.
- `core.ServiceVerrousHomologation` (lecture : `champsVerrouilles`,
  `estVerrouille`, `motifVerrou`, `verifier` ; modifications gardées :
  `modifierNom`, `modifierCategorie`, `modifierClubOrganisateur`,
  `modifierNumeroAffiliation`, `modifierAdresse`, `modifierPeriode`,
  `modifierCorrespondant`, `ajouter/retirer/remplacerJugeArbitre`,
  `ajouter/retirer/remplacerTableau`) → `core.ResultatModificationTournoi`
  (`APPLIQUEE` / `CHAMP_VERROUILLE`, champs, message).
- `ServiceCycleTournoi.refuserHomologation(tournoi, motif)` (demande →
  brouillon ; `ResultatTransition.Motif.MOTIF_REFUS_ABSENT` si motif vide) ;
  constructeur avec `Horloge`.
- Format de sauvegarde : champ facultatif `refusHomologation` (liste
  `{date, motif}`), absent des anciens fichiers.
- Les setters du modèle ne lèvent pas d'exception (contrairement à B010) : les
  écrans doivent passer par `ServiceVerrousHomologation`. Choix prudent pour ne
  pas casser les formulaires, la démo et le géocodage existants.

## Fusion

À compléter par l'`integrateur`.

## Chiffres

- Tests : 697 → 742 (mesurés par `mvn -B test`)
- Version de la démo : inchangée
