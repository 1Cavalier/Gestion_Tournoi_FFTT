---
id: B019
titre: Convocations par message (SMS)
type: idee
priorite: P2
taille: L
statut: en-cours
depend-de: []
fichiers: [model/Joueur ou FicheLicence (portable), core (convocations), ui/ecran (réglages et consentement), persistence]
branche: tache/B019-convocations-sms
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

Le choix « rien / SMS / appel vocal / les deux » au lancement d'un match est posé par B037 ; cette tâche branche l'option « SMS » (message seulement, jamais d'appel).

Aucune pour l'instant.

## Passations

- 2026-11-06 `developpeur` : cœur (textes GSM 7 bits en un SMS, port `EnvoiSms`, consentement, limite, journal, effacement), persistance `convocationsSms` rétrocompatible, simulation `EnvoiSmsSimule`, envoi au lancement (annonce B037 + case « envoi automatique »), écran minimal « SMS… » depuis le jour J. Tests 1046 → 1100, 0 échec.
- Reste pour `developpeur-interface` : mise en forme de `EcranConvocationsSms` (aucun CSS propre), saisie du numéro et du consentement dans le formulaire d'inscription (non faite).
- 4 questions pour Quentin dans `docs/journal/2026-11-06-tache-B019-convocations-sms.md` (double condition d'envoi, limite 300, moment de l'effacement, accents remplacés).
- 2026-11-08 `developpeur-interface` : portable facultatif + case d'accord jamais pré-cochée à l'inscription en ligne (`AssistantInscription`) et côté club (`VueInscriptionsClub`, « Par joueur »), bloc commun `BlocSaisieSms` ; enregistrement immédiat du tournoi.
- Écran « SMS… » fini (tuiles, alerte limite, journal en pastilles, bulles simulées, effacement avec confirmation explicite) ; logique dans `PresentationConvocationsSms` / `PresentationSaisieSms` testées ; CSS section B019 en fin de `theme.css` ; cœur : `refusSaisieFacultative`, `dateEffacementAutomatique`.
- Tests 1100 → 1112, 0 échec ; captures Xvfb vues (clair/sombre), validation visuelle finale à Quentin ; 1 question ajoutée (saisie club / import).
- 2026-11-08 `verificateur-modele` : 28 tests ajoutes (`ConvocationsSmsLimitesTest`, `ConvocationsSmsRetrocompatibiliteTest`) : limite exacte/zero, consentement, effacement, GSM-7/UCS-2, numeros, expediteur, jamais d'appel, anciens fichiers, aller-retour ; 1140 tests, 0 echec, aucun defaut bloquant.
- Non bloquants : le `detail` d'un echec (message d'exception du prestataire) peut contenir le numero et survit a l'effacement (`ServiceConvocationsSms.envoyerA`) ; `+33 (0)6 12...` refuse (`NumeroPortable`) ; effacement automatique declenche seulement a l'ouverture de `EcranJourJ`/`EcranConvocationsSms`.

## Résultat
