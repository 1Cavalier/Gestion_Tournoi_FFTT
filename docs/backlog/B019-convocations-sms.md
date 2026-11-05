---
id: B019
titre: Convocations par message (SMS)
type: idee
priorite: P2
taille: L
statut: a-faire
depend-de: []
fichiers: [model/Joueur ou FicheLicence (portable), core (convocations), ui/ecran (réglages et consentement), persistence]
branche:
pr:
---

## Ce qu'il faut faire

Le club peut prévenir par **message écrit (SMS, jamais d'appel)** :
- « Vous êtes attendu(e) table X contre Prénom Nom (club) — Tableau T, poule B » ; « … — Tableau T, 1/8 de finale » ; « … match de classement (places 17 à 32) » ;
- convocation pour **arbitrer** : « Vous arbitrez la partie table X : A contre B » ;
- envoi automatique à l'envoi d'un match sur une table (option) ou manuel ;
- numéro de portable **facultatif**, saisi à l'inscription ou par le club, avec **consentement explicite** (RGPD) et effacement à la fin du tournoi ;
- port `EnvoiSms` dans le cœur + implémentation **simulée** (journal des messages à l'écran) ; le vrai prestataire viendra avec B020 ;
- textes testés (longueur d'un SMS, caractères), limite d'envois, journal des envois.

## Critères de fin

- [ ] textes et règles de déclenchement testés
- [ ] consentement et effacement du numéro
- [ ] fonctionne de bout en bout avec l'implémentation simulée
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

## Résultat
