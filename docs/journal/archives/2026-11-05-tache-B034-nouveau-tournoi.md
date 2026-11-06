# Nouveau tournoi : texte explicatif et sélecteur de dates (B034) — 2026-11-05

- Branche : `tache/B034-nouveau-tournoi`
- Agent : `developpeur-interface` (formulaire), `designer-interface` (charte, hauteurs), `relecteur-code` (relecture), `documentaliste` (ce journal)
- Pull Request : à venir (étape 5, `integrateur`)
- Statut : en cours de chaîne (en attente de fusion par l'`integrateur`)

## Pour Quentin

L'écran « Nouveau tournoi » change de présentation. En haut, un petit texte
explique à quoi servent les informations demandées : elles sont obligatoires
pour demander l'homologation, ou simplement pour gérer le tournoi. Les
informations sont regroupées dans deux cartes, « Le tournoi » et « Dates ».
Pour les dates, une **icône calendrier** ouvre le choix du jour ; on peut aussi
taper jj/mm/aaaa. Une date qui n'existe pas (comme le 31 février) est refusée.
Quand le début est choisi, la date de fin ne propose plus les jours
précédents. Un résumé indique la période retenue. La page suivante ne change pas.

## À tester à l'écran

- Compte de démo : compte habituel (aucun tournoi particulier requis).
- Chemin : Gestion des tournois → « Nouveau tournoi ».
- Ce qu'il faut regarder (thème clair et sombre) :
  1. Le texte explicatif apparaît au-dessus des cartes et se lit sans effort.
  2. Les deux cartes « Le tournoi » et « Dates » sont alignées ; le sélecteur de date a la même hauteur que les champs texte voisins (34 px).
  3. Cliquer l'icône calendrier : le choix du jour s'ouvre. Pour la date de fin, les jours avant le début sont grisés.
  4. Taper 31/02/2027 à la main : la date est refusée avec un message, le résumé ne change pas.
  5. Le résumé de période affiche bien le premier et le dernier jour choisis.
  6. Le bouton de la page suivante fonctionne comme avant.
- Captures : `docs/captures/B034/` (clair et sombre, avant et après dans `docs/captures/B034/design/`).
- Non capturés : le popup du calendrier et l'état « focus » des champs. À regarder à l'écran.

## Questions pour Quentin

Aucune.

## Notes pour l'état du projet

- Classes principales : `ui.ecran.FormulaireNouveauTournoi` (vue, extraite de `PageGestionTournoi`), `ui.ecran.PresentationNouveauTournoi` (libellés, vérification de date en mode strict, résumé de période, sans JavaFX).
- Saisie des dates : sélecteur `DatePicker` (classe `selecteur-calendrier`), règles CSS en fin de `theme.css` (section B034). Le tracé de l'icône calendrier est recopié dans le CSS (le CSS ne lit pas `Icones.GESTION`).
- Format de sauvegarde : inchangé. Démo : inchangée.
- Défauts non bloquants (relecture) : classes CSS `nouveau-tournoi` et `nouveau-tournoi-explication` sans règle (accroches) ; `PresentationNouveauTournoi.formater` utilisée seulement par le test ; nom complet du DatePicker dans `Ui.java`.
- JavaFX non vérifié visuellement en session : validation à l'écran par Quentin.

## Fusion

Non faite : c'est l'`integrateur` qui synchronise avec `main` et lance `python3 outils-dev/verifier-fusion.py`.

## Chiffres

- Tests : 675 → 687 (mesurés par `mvn -B test` ; nombre de `@Test` sur `main` et sur la branche ; 0 échec)
- Version de la démo : inchangée
