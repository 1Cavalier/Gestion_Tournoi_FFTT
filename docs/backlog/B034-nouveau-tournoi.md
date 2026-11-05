---
id: B034
titre: Nouveau tournoi : texte explicatif et sélecteur de dates
type: interface
priorite: P1
taille: S
statut: en-cours
depend-de: []
fichiers: [ui/ecran (formulaire de création)]
branche: tache/B034-nouveau-tournoi
date: 2026-11-05
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 5. Texte explicatif (ces informations sont obligatoires pour demander l'homologation, ou simplement pour gérer le tournoi), **icône calendrier** pour choisir les dates (sélecteur de date), nouvelle mise en page v2, puis page suivante comme aujourd'hui.

## Critères de fin

- [x] texte et sélecteur de dates
- [x] captures regardées
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur-interface : formulaire extrait de `PageGestionTournoi` dans `FormulaireNouveauTournoi` (vue) + `PresentationNouveauTournoi` (texte explicatif, vérification, résumé de période, 12 tests) ; cartes v2 « Le tournoi » / « Dates », `DatePicker` à icône calendrier (classe `selecteur-calendrier`, jours avant le début grisés pour la fin), `Ui.champEtiquete(…, DatePicker, …)`.
  Page suivante inchangée (`afficherCompletion`). Captures clair/sombre regardées : `docs/captures/B034/`. Reste au designer : hauteur du sélecteur de date (~2 px de moins que les champs texte), règles CSS en fin de `theme.css` (section B034).
- designer-interface : charte v2 vérifiée clair/sombre (aucune couleur en dur, rien dans le Java) ; sélecteur de date aligné sur les champs texte (34 px, marge 8/12, règle `.selecteur-calendrier > .text-field` dans la section B034).
  Captures `docs/captures/B034/design/avant|apres/nouveau-tournoi-{clair,sombre}.png`, regardées et mesurées.
  Tracé de l'icône calendrier laissé recopié dans `theme.css` (le CSS ne peut pas lire `Icones.GESTION`) ; popup du calendrier et état focus non capturés.

## Résultat
