---
id: B017
titre: Annonces vocales automatiques des matchs
type: jour-j
priorite: P2
taille: L
statut: a-faire
depend-de: []
fichiers: [core (annonces), ui/infra (synthèse vocale), ui/ecran/EcranSalle, ui/ecran/EcranJourJ]
branche:
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

## Résultat
