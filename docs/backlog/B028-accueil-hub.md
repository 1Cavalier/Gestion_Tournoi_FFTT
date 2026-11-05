---
id: B028
titre: Accueil : point d'entrée qui explique chaque section
type: interface
priorite: P1
taille: M
statut: en-cours
depend-de: []
fichiers: [ui/ecran/PageAccueil, ui/ecran/Coquille]
branche: tache/B028-accueil-hub (2026-11-05)
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 1. Refaire complètement l'accueil : cartes **Profil** (nom, prénom, points, sexe, phrase d'accès aux informations personnelles), **Tournois** (explication + accès à la carte des tournois), **Agenda** (prochaines dates ; carte « à venir » si B032 n'est pas encore fusionnée), **Statistiques** (carte « bientôt »). Puis une section **bien séparée** « Gestion de votre club », visible **seulement** pour un compte relié à un club : gestion des tournois, historique des tournois, buvette (à venir), autres (à venir). Chaque carte dit en une phrase à quoi sert la section et y mène.

## Critères de fin

- [ ] un compte joueur ne voit pas « Gestion de votre club », un compte de club la voit
- [ ] chaque carte mène à sa section
- [ ] captures clair et sombre regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur-interface (2026-11-05) : accueil refait (PageAccueil mince + PresentationAccueil testée, 9 tests). Cartes Profil, Tournois, Agenda (« Bientôt », 3 prochaines dates des tournois du joueur), Statistiques (« Bientôt ») ; section « Gestion de votre club » (gestion, historique, buvette, mention « autres ») seulement si le compte a un accès club. Coquille : entrée « Agenda » provisoire (PagePlaceholder) pour que la carte mène quelque part ; B032 la remplacera. Icones.AGENDA ajouté ; CSS en fin de theme.css (section B028). Captures clair/sombre non faites (pas d'affichage en session cloud).

## Résultat
