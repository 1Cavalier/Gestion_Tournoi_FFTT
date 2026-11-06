---
id: B029
titre: Mon profil : bloc personnel et licence, bloc club avec avertissement et droits
type: interface
priorite: P1
taille: M
statut: en-cours
depend-de: []
fichiers: [ui/ecran/PageCompte]
branche: tache/B029-mon-profil (2026-11-05)
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 2. Bloc du haut pour tous : nom, prénom, email, téléphone, et à côté la **licence FFTT officielle** avec toutes ses informations ; puis sections **Tournoi** et **Appel micro : prononciation phonétique** (champ d'affichage et lien vers la future saisie de B018 si elle n'existe pas encore). Bloc du bas **Club** : relier son compte, avec un **avertissement bien visible avant** de confirmer (encadré d'alerte, confirmation explicite) ; si relié, un **tableau des droits** (ce que le compte peut faire pour le club).

## Critères de fin

- [x] deux blocs distincts
- [x] avertissement visible et confirmation avant liaison
- [x] tableau des droits si relié
- [x] captures clair et sombre regardées (designer-interface, `docs/captures/B029/`)
- [x] `mvn -B test` à 0 échec (750 tests)

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations
- designer-interface : charte v2 appliquée — colonnes alignées en haut (identité + Tournoi + Appel à gauche, licence à droite, plus de carte étirée à vide), avatar dans « Mes informations », tableaux droits/équipe pleine largeur à en-tête teinté et lignes filetées (classes .profil-tableau* en section B029 de theme.css). Logique inchangée.
- Réparé au passage dans theme.css une fusion cassée venue de main (accolade et ouverture de commentaire perdues entre B028 et B033 : règle accueil-club-tuile et section B033 ignorées).
- Harnais : mode `profil` (outils-dev/run.sh) ; captures avant/après clair et sombre dans docs/captures/B029/ ; mvn -B test vert. Non vérifié : rendu Windows, mise à l'échelle.
- developpeur-interface : `PresentationProfil` (texte et règles, sans JavaFX) testée par 10 tests ; `PageCompte` refaite autour ; `Ui.encartAlerte` ajouté ; liaison impossible sans case de confirmation ; tableau des droits relié à la présentation.
- relecteur-code : aucun point bloquant. Les classes `profil-bloc-personnel` et `profil-tableau-membres` n'ont pas de règle propre : commentaire ajouté dans theme.css (documentaliste).
- documentaliste : réparation de theme.css vérifiée (accolades et commentaires équilibrés, variables utilisées toutes définies) ; journal `docs/journal/2026-11-06-tache-B029-mon-profil.md`.

## Résultat

- Statut : en attente de fusion (`integrateur`).
- Livré : « Mon profil » en deux blocs. Haut : identité, Tournoi, Appel micro à gauche, licence FFTT à droite. Bas (bloc Club) : liaison avec un encart d'alerte et une case à cocher obligatoire, puis tableau des droits si le compte est relié.
- Tests : 750 après (mesuré par `mvn -B test`), soit 740 avant (calcul : 10 nouveaux tests dans `PresentationProfilTest`, aucun supprimé), 0 échec. Captures : `docs/captures/B029/avant|apres/` (clair et sombre).
- Questions ouvertes pour Quentin : téléphone absent du compte (ajout = changement du JSON) ; liste des droits par rôle à valider ; appel micro « Bientôt » en attendant B018. Détail dans le journal.
- Non vérifié : rendu Windows, mise à l'échelle.
