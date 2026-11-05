# Jour J plus simple : matchs proposés et avancement des tableaux (B011) — 2026-11-05

- Branche : `tache/B011-jour-j-simplifie`
- Agent : `developpeur-jour-j` (premier maillon de la chaîne)
- Pull Request : (complété par l'intégrateur)
- Statut : en cours de chaîne

## Pour Quentin

La page « Jour J » devient l'écran unique du juge-arbitre. À gauche, les
**matchs proposés** de tous les tableaux, dans l'ordre de l'ordonnanceur, avec
un bouton « Table N » qui envoie le match sur la première table libre en un
clic. Au centre, le bandeau **« Où en sont les tableaux »** (une ligne par
tableau : étape, avancement, jauge, parties en jeu, bouton vers le pointage,
les poules ou l'arbre), puis les **tables**. « Terminer » sur une table ouvre
la saisie du score à gauche, sans changer de page ; après l'enregistrement, un
bouton « Envoyer … à la table N » (touche Entrée) enchaîne le match suivant.
Tout (bandeau, chiffres, tables, propositions) se redessine à chaque action.

## À tester à l'écran

- Compte de démo : Quentin (compte habituel), « Tournoi de démonstration du jour J ».
- Chemin : Gestion des tournois → « Suivre le tournoi » (ou carte « Jour J » → « Ouvrir le jour J »).
- Scénario pas à pas :
  1. L'écran s'ouvre directement sur les matchs proposés (gauche), le bandeau des 6 tableaux et les 16 tables.
  2. Bandeau : F « Phase finale · Quarts de finale · 5 / 10 parties · 1 en jeu », A « Inscriptions », C « Poules en cours », B « Pointage ouvert », D « Prêt à lancer », E « Poules terminées ».
  3. Dans « Matchs proposés », cliquer « Table 1 » sur le premier match : la table 1 passe « En cours », le match disparaît de la liste, le bandeau indique une partie en jeu de plus pour son tableau.
  4. Sur la carte de la table 1, cliquer « Terminer » : la saisie s'ouvre à gauche. Taper 11-7, 9-11, 11-4, 11-8, « Enregistrer le score ».
  5. Message « Résultat enregistré : la table 1 est libre » et bouton « Envoyer X – Y à la table 1 » : appuyer sur Entrée (ou cliquer). Le match suivant part sur la table 1.
  6. Vérifier que le bandeau de C a avancé (6 / 12 parties) sans recharger.
  7. Erreur d'envoi : « Annuler » sur une table remet le match dans les propositions.
  8. Cliquer la ligne « C – Toutes séries » (ou « Poules ») : écran des poules ; « ← Jour J » revient à l'écran principal. Idem « F » → arbre, « B » → pointage.
  9. Ligne « A » : « Ouvrir le pointage » demande confirmation sous la ligne.
  10. Alt+P (« Proposer ») remplit toutes les tables libres d'un coup.
  11. Refaire en thème sombre.
- Captures avant / après : `docs/captures/jour-j-simplifie/avant/`, `docs/captures/jour-j-simplifie/apres/` (mode `jourj` du harnais : `outils-dev/run.sh <dossier> jourj [sombre]`).

## Questions pour Quentin

### 2026-11-05 — Enchaîner un joueur qui vient de finir
- Contexte : après un score, le premier match proposé peut concerner un joueur qui sort de table (capture j04 : Hugo MORIN rejoue aussitôt). L'écran suit l'ordonnanceur sans le modifier (B025 traite les règles de priorité et de repos).
- Article FFTT : aucun temps de repos minimal trouvé dans les textes fournis pour les tournois individuels ; question déjà ouverte (« temps de repos »).
- Options : A) garder l'ordre de l'ordonnanceur ; B) proposer en premier un match dont aucun joueur ne sort de table.
- Choix provisoire de l'agent : A (aucun changement de règle, B025).
- Réponse de Quentin :

### 2026-11-05 — Table choisie par « Table N »
- Contexte : l'envoi en un clic prend la plus petite table libre.
- Article FFTT : aucun (organisation de la salle).
- Options : A) plus petite table libre ; B) tables réservées par tableau (zone de la salle) ; C) table la plus proche de la table d'arbitrage.
- Choix provisoire de l'agent : A ; les autres tables restent accessibles par « Autre table… » ou glisser-déposer.
- Réponse de Quentin :

## Notes pour l'état du projet

- Défauts non bloquants restants : classes CSS `avancement-etape` / `avancement-jauge` sans règle ; `PresentationSalle.salleOuverte` utilisée seulement en test.
- Interface vue uniquement en captures Xvfb ; validation à l'écran par Quentin.

- Page Jour J = écran principal unique (`EcranJourJ` construit `EcranSalle` avec un bandeau) ; l'ancienne liste des tableaux et la carte « Salle et tables » disparaissent, les écrans de pointage, poules et arbre restent des vues secondaires ouvertes depuis le bandeau.
- Logique sans JavaFX : `PresentationAvancement` (étape, texte, jauge, vue de détail, tour en cours du tableau final, exemptions non comptées) ; `PresentationSalle.resumeLigne`, `suiteSurTable`, `premiereTableLibre`.
- Ordonnanceur et cœur inchangés ; format JSON inchangé ; démo inchangée (elle montre déjà toutes les étapes).

## Fusion

(complété par l'intégrateur)

## Chiffres

- Tests : 643 → 656 (mesurés avec `outils-dev/tests.sh`, Maven Central inaccessible)
- Version de la démo : inchangée
