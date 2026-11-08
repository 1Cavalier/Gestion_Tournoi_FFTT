# Annonces vocales automatiques des matchs (B017) — 2026-11-08

- Branche : `tache/B017-annonces-vocales`
- Agent : `developpeur-jour-j` (étape de pipeline)
- Pull Request : à compléter par l'intégrateur
- Statut : en attente de relecture (étape de pipeline)

## Pour Quentin

Quand un match est envoyé sur une table, l'ordinateur peut maintenant l'annoncer
dans la salle : « Hugo Morin, club TT Brunoy, contre Léa Durand, club US Évry,
table 4. » (avec le tableau et le tour si vous le cochez). Ça se branche sur le
choix « Appel vocal » (ou « SMS et appel vocal ») déjà proposé au lancement.
Un menu « Voix » dans la salle règle tout pour le tournoi : annonce automatique,
tableau et tour, répétition (1 à 3 fois), rappel au bout de 3, 5 ou 10 minutes,
débit, voix de Windows. Chaque table a un bouton « Annoncer » ; quand un rappel
est prévu, « Joueurs à la table » l'annule. Les annonces passent l'une après
l'autre. Si la voix ne marche pas (hors Windows, PowerShell absent), l'annonce
s'affiche en grand « À lire au micro » : rien ne se perd.

**La session cloud ne peut pas écouter** : la voix elle-même (SAPI de Windows)
n'a été vérifiée que par des tests sans son. C'est à valider chez vous, sous Windows.

## À tester à l'écran

- Compte de démo : profil par défaut.
- Chemin : Gestion des tournois → « Tournoi de démonstration du jour J » → Suivre
  le tournoi (écran Jour J).
- Pas à pas (sous Windows, haut-parleurs allumés) :
  1. En haut, menu « Voix : à la demande » → cocher « Annonce automatique au
     lancement ». Le libellé devient « Voix : auto ».
  2. Menu « Annonce : … » → choisir « Appel vocal ».
  3. Cliquer « Table N » sur un match proposé : l'ordinateur dit l'annonce deux
     fois ; le message vert indique « Annonce de la table N en cours. »
  4. Cliquer « Proposer » avec plusieurs tables libres : les annonces passent
     l'une après l'autre, jamais en même temps.
  5. Menu « Voix » → Répétition « Dire 1 fois », Débit « Lent », cocher « Dire le
     tableau et le tour », choisir une voix (liste remplie après quelques
     secondes au premier affichage) ; cliquer « Annoncer » sur une table occupée
     et écouter la différence. L'écran ne doit jamais se figer pendant la voix.
  6. Menu « Voix » → « Rappel après 3 min » ; envoyer un match ; attendre
     3 minutes sans rien faire : « Rappel : … » est dit. Recommencer et cliquer
     « Joueurs à la table » sur la carte : pas de rappel. Saisir le score ou
     annuler l'envoi : pas de rappel non plus.
  7. Fermer et rouvrir l'application : les réglages du menu « Voix » sont gardés.
  8. Essayer un nom avec apostrophe ou « & » (par exemple un club « Ping & Co ») :
     l'annonce est dite normalement.
  9. Sans Windows (ou PowerShell désactivé) : le menu affiche « Voix :
     indisponible » et chaque annonce apparaît en encart « À lire au micro (…) ».
- Thème clair et sombre : en-tête de la salle (les actions passent sur deux
  lignes si la fenêtre est étroite), liens « Annoncer » / « Joueurs à la table »
  dans les cartes de table, encart « À lire au micro ».

## Questions pour Quentin

### 2026-11-08 — « Appel vocal » : annonce dans la salle ou appel téléphonique ?
- Contexte : B037 a nommé l'option « Appel vocal » (« appel automatique aux deux
  joueurs ») ; B017 la branche sur l'annonce dite par l'ordinateur dans la salle.
- Article FFTT : sans objet (fonction d'organisation).
- Options : A) garder « Appel vocal » ; B) renommer en « Annonce vocale » ou
  « Annonce au micro » pour éviter de croire à un appel sur le portable.
- Choix provisoire de l'agent : A (libellé inchangé, aide du choix mise à jour).
- Réponse de Quentin :

### 2026-11-08 — Combien de rappels, et comment savoir que la table est vide ?
- Contexte : le logiciel ne sait pas si les joueurs sont arrivés à la table.
- Article FFTT : à vérifier par le contrôleur de conformité (délai de
  présentation à la table / forfait, règlements sportifs) ; aucun automatisme de
  forfait n'est lié au rappel.
- Options : A) un seul rappel, annulé par « Joueurs à la table », le score ou
  l'annulation de l'envoi ; B) rappels répétés toutes les N minutes ; C) rappel
  qui propose aussi de déclarer le forfait.
- Choix provisoire de l'agent : A, rappel coupé par défaut.
- Réponse de Quentin :

### 2026-11-08 — Réglages par défaut
- Contexte : annonce automatique coupée, dite 2 fois, sans tableau ni tour,
  sans rappel, débit normal, voix par défaut de Windows.
- Options : A) garder ; B) annonce automatique active dès qu'« Appel vocal » est choisi.
- Choix provisoire de l'agent : A (même logique que les SMS automatiques de B019).
- Réponse de Quentin :

## Notes pour l'état du projet

- Cœur : `TexteAnnonceVocale` (texte, noms en capitales remis en casse),
  `AnnonceVocale` / `SegmentAnnonce` (prénom, nom, club séparés),
  `GenerateurSsml` (échappement XML, `<phoneme alphabet="ipa">`),
  `ResolveurPrononciation` (point d'accroche B018, `TEXTE_NORMAL` par défaut),
  port `SyntheseVocale`, `ServiceAnnoncesVocales` (file une à la fois,
  répétition, rappel via `Horloge`, repli, `memeRencontre` par licences car le
  tournoi est relu entre deux pages).
- Modèle : `ReglagesAnnoncesVocales` (`Tournoi.getAnnoncesVocales()`).
- Sauvegarde : champ facultatif `annoncesVocales` (écrit seulement si touché),
  anciens fichiers relus, valeurs abîmées bornées (`AnnoncesVocalesPersistanceTest`).
- `ui.infra` : `SyntheseVocaleWindows` (PowerShell `-EncodedCommand` d'un script
  constant ; SSML dans un fichier temporaire désigné par variable
  d'environnement ; 60 s maximum ; fichier supprimé), `DiffuseurAnnonces`
  (fil de fond unique, une instance dans `ContexteApplication`).
- Salle : menu « Voix », « Annoncer », « Joueurs à la table », annonce
  automatique au lancement ; l'encart d'annonce n'est pas doublé pour « Appel
  vocal » seul. En-tête de la salle : actions en `FlowPane` (passaient en
  débordement avec le menu de plus).
- Limite connue : les rappels ne sont vérifiés que lorsque l'écran Jour J est
  affiché (minuterie de 20 s de la salle).
- Démo : non modifiée (`VERSION_DEMO` inchangée). Harnais de captures : mode
  `b017` (`outils-dev/run.sh <dossier> b017`) ; le mode `jourj` existant est
  cassé (« Suivre le tournoi » introuvable), non corrigé ici.

## Fusion

À faire par l'intégrateur.

## Chiffres

- Tests : 1263 → 1299 (`outils-dev/tests.sh`, Maven inaccessible), 0 échec.
- Version de la démo : inchangée.
