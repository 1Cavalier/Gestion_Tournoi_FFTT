---
id: B017
titre: Annonces vocales automatiques des matchs
type: jour-j
priorite: P2
taille: L
statut: en-cours
depend-de: []
fichiers: [core (annonces), ui/infra (synthèse vocale), ui/ecran/EcranSalle, ui/ecran/EcranJourJ]
branche: tache/B017-annonces-vocales
pr:
---

## Ce qu'il faut faire

Quand un match est envoyé sur une table, l'ordinateur l'annonce : « *Prénom Nom, club X, contre Prénom Nom, club Y, table N* » (avec le tableau et le tour si l'option est choisie).
- Cœur : texte d'annonce (testé), file d'annonces (pas deux en même temps), répétition possible, activation et réglages par tournoi (voix, débit, répétition, rappel au bout de N minutes si la table reste vide).
- Port `SyntheseVocale` dans le cœur ; implémentation dans `ui.infra` **sans dépendance Maven** : sous Windows, la synthèse vocale du système (SAPI) appelée par PowerShell avec du SSML ; repli silencieux (annonce affichée à l'écran) si indisponible.
- **Sécurité** : les noms viennent des données ; ne jamais les mettre dans une ligne de commande. Passer le SSML par un fichier temporaire ou l'entrée standard, échappé XML. Tests de l'échappement.
- Prononciation **par défaut** : le texte normal. La prononciation phonétique personnalisée est la tâche B018 (prévoir le point d'accroche).

## Critères de fin

- [ ] texte d'annonce et file testés
- [ ] bouton « Annoncer » et annonce automatique activable
- [ ] aucun nom dans une ligne de commande (test)
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Le choix « rien / SMS / appel vocal / les deux » au lancement d'un match est posé par B037 ; cette tâche branche l'option « appel vocal ».

Seul Windows est testable chez Quentin ; la session cloud ne peut pas écouter : le dire dans le journal.

## Passations

- developpeur-jour-j (2026-11-08) : cœur (`ServiceAnnoncesVocales`, `TexteAnnonceVocale`, `GenerateurSsml`, ports `SyntheseVocale` / `ResolveurPrononciation`), réglages persistés (`annoncesVocales`), `ui.infra.SyntheseVocaleWindows` + `DiffuseurAnnonces`, salle (menu « Voix », « Annoncer », « Joueurs à la table »).
- Tests 1263 → 1299 via `outils-dev/tests.sh`, 0 échec ; captures `run.sh <dossier> b017` regardées (en-tête passé en FlowPane, lien de rappel sur sa ligne). Son non vérifiable en cloud.
- À vérifier : relecture sécurité de `SyntheseVocaleWindows` (aucun nom en ligne de commande, test dédié) ; questions Quentin dans le journal (libellé « Appel vocal », rappel unique).

## Résultat
- verificateur-modele (2026-11-08) : +21 tests (`ServiceAnnoncesVocalesLimitesTest`, `GenerateurSsmlLimitesTest`, `SyntheseVocaleWindowsLimitesTest`, 2 dans `AnnoncesVocalesPersistanceTest`) ; SSML, texte, persistance, `SyntheseVocaleWindows` (aucune donnée de joueur, fichier supprimé dans tous les cas) conformes.
- Défaut à corriger : `ServiceAnnoncesVocales.diffuserSuivante` (l.~226) reprogramme un rappel même si `oublier(match)` a été appelé pendant la diction (test `rencontre_oubliee_pendant_la_diction_ne_programme_pas_de_rappel`, rouge volontairement) ; mineur : `SyntheseVocaleWindows.dire` met `powershellAbsent` sur toute IOException après création du fichier (écriture disque comprise).
