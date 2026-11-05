---
id: B032
titre: Agenda personnel, ajout automatique des tournois, export
type: idee
priorite: P1
taille: M
statut: en-cours
depend-de: []
fichiers: [model (agenda), core (agenda), persistence/DepotApplication]
branche: tache/B032-agenda (2026-11-05)
pr:
---

## Ce qu'il faut faire

Section 1 et 3 du cahier des charges. Cœur et persistance de l'**agenda du compte** : dates personnelles (titre, date, heure facultative, note), **ajout automatique** d'un tournoi à l'inscription (et retrait à la désinscription complète), **export** au format iCalendar `.ics` (fichier importable dans Google Agenda, Outlook, Apple), écrit à la main sans dépendance. Pas d'écran ici (les tâches Accueil et Mes tournois l'afficheront) sauf une méthode de présentation testée.

## Critères de fin

- [ ] ajout et retrait automatiques testés
- [ ] fichier .ics valide (testé : en-têtes, échappement, fin de ligne CRLF)
- [ ] persistance rétrocompatible
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

**developpeur (2026-11-05)**
- Fait : modèle `AgendaCompte`, `EvenementAgenda` (builder, UID stable), `OrigineEvenement` ; cœur `ServiceAgenda` (dates personnelles avec motifs `ResultatAgenda`, `synchroniserTournoi` : tournoi présent tant que la licence est inscrite ou en attente sur un tableau, retiré sinon), `ExportIcs` (CRLF, échappement 3.3.11, pliage 75 octets UTF-8, UID, DTSTAMP UTC par `Horloge`, journée entière en `VALUE=DATE`) ; `ServiceInscriptionEnLigne.inscrire(…, agenda)` et nouvelle `desinscrire(…)` (tout ou rien, refusée hors phase INSCRIPTION, repêchage par `Tableau.desinscrire`) ; persistance `DepotApplication.agendaDe` + champ JSON facultatif `agendas` ; `ContexteApplication.inscrireEnLigne` alimente l'agenda du compte connecté ; `ui.ecran.PresentationAgenda` (sans JavaFX).
- Tests : 28 nouveaux (`ServiceAgendaTest` 11, `ExportIcsTest` 7, `AgendaCompteTest` 4, `AgendaRoundTripTest` 3, `PresentationAgendaTest` 3) ; 715 au total, 0 échec.
- Pour la suite : aucun écran n'appelle encore `desinscrire` ni `ExportIcs` (tâches Accueil / Mes tournois). Le modèle `Tournoi` n'a pas d'identifiant stable : clé d'agenda = n° d'homologation, sinon nom|1er jour|club (renommer un tournoi non homologué crée une nouvelle entrée).
- Questions pour Quentin : (1) un tournoi en **liste d'attente seulement** reste-t-il dans l'agenda ? (choix prudent : oui, avec la mention « Liste d'attente ») ; (2) jusqu'à quand un joueur peut-il se **désinscrire en ligne** (choix : tant que le tableau est en phase d'inscription, sans date limite ni remboursement) ? (3) une date de tournoi peut-elle être retirée à la main de l'agenda (choix : non, elle suit l'inscription) ?

**verificateur-modele (2026-11-05)**
- Fait : 12 tests dans `AgendaVerificationTest` (désinscription partielle/complète, attente, repêchage, doublons, .ics : pliage 2/3/4 octets, échappement, DTEND exclusif, DTSTAMP UTC) + 2 dans `AgendaRoundTripLimitesTest`. Total 735, 1 échec volontaire.
- DÉFAUT BLOQUANT : `ServiceInscriptionEnLigne.desinscrire` avec un tableau en double dans la collection (`List.of(a, a)`) : attendu désinscription valide (ou refus propre), obtenu `NullPointerException: joueur` (second `tab.desinscrire(present(...))` après retrait). Test `desinscription_avec_tableau_en_double_ne_plante_pas` ; correction : dédoublonner `tableaux` (LinkedHashSet) avant la validation.

**correcteur (2026-11-05)**
- Corrigé : `desinscrire` dédoublonne `tableaux` (`LinkedHashSet`) avant validation ; cause : la 2e itération cherchait un joueur déjà retiré (`present` → null). `desinscription_avec_tableau_en_double_ne_plante_pas` vert.
- Tests : 735, 0 échec.

**relecteur-code (2026-11-05)**
- Liaisons OK : règle d'or respectée (aucun import ui/persistence/javafx dans model/core, pas de switch ni TODO), `agendas` écrit et relu (facultatif, anciens fichiers relus), `inscrireEnLigne` appelle `sauvegarder()` après alimentation de l'agenda, aucune classe CSS ni couleur.
- Remarques mineures : `desinscrire`, `ExportIcs` et `PresentationAgenda` n'ont pas encore d'appelant hors tests (prévu : Accueil / Mes tournois) ; le futur écran devra sauvegarder le tournoi ET `sauvegarder()` après `desinscrire`.
- Aucun défaut bloquant.

## Résultat
