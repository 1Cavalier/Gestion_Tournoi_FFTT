---
id: B018
titre: Prononciation des noms en alphabet phonétique
type: idee
priorite: P2
taille: M
statut: a-faire
depend-de: [B017]
fichiers: [core (registre de prononciations), persistence/DepotApplication, ui/ecran/PageCompte, ui/ecran/EcranJourJ]
branche:
pr:
---

## Ce qu'il faut faire

Pour les noms mal prononcés par la synthèse vocale :
- un **registre de prononciations** (par licence pour les joueurs, par numéro de club pour les clubs), en **alphabet phonétique international (API / IPA)**, conservé dans les données de l'application (réutilisé d'un tournoi à l'autre) ;
- dans le **profil** du joueur (`PageCompte`), une partie **« Appel »** : sa prononciation phonétique, avec un bouton « Écouter » ;
- le juge-arbitre peut aussi la saisir ou la corriger depuis le jour J ;
- l'annonce (B017) utilise la balise SSML `phoneme` (alphabet IPA) quand une prononciation existe, sinon le texte normal ;
- aide à la saisie : quelques exemples et les symboles courants du français.

## Critères de fin

- [ ] registre persisté (test aller-retour)
- [ ] SSML produit avec la prononciation (test)
- [ ] partie « Appel » dans le profil
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

La partie « Appel micro : prononciation phonétique » du profil est mise en place par B029 ; cette tâche fournit la saisie, le registre et l'écoute.

Aucune pour l'instant.

## Passations

## Résultat
