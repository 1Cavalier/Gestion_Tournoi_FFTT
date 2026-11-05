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

- [ ] deux blocs distincts
- [ ] avertissement visible et confirmation avant liaison
- [ ] tableau des droits si relié
- [ ] captures clair et sombre regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations
- designer-interface : charte v2 appliquée — colonnes alignées en haut (identité + Tournoi + Appel à gauche, licence à droite, plus de carte étirée à vide), avatar dans « Mes informations », tableaux droits/équipe pleine largeur à en-tête teinté et lignes filetées (classes .profil-tableau* en section B029 de theme.css). Logique inchangée.
- Réparé au passage dans theme.css une fusion cassée venue de main (accolade et ouverture de commentaire perdues entre B028 et B033 : règle accueil-club-tuile et section B033 ignorées).
- Harnais : mode `profil` (outils-dev/run.sh) ; captures avant/après clair et sombre dans docs/captures/B029/ ; mvn -B test vert. Non vérifié : rendu Windows, mise à l'échelle.

## Résultat
