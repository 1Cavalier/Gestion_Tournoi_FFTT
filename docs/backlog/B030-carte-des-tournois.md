---
id: B030
titre: Carte des tournois : vue carte avec « Voir plus », vue agenda par jour, statuts visibles
type: interface
priorite: P1
taille: L
statut: a-faire
depend-de: []
fichiers: [ui/ecran/PageTournois, ui/ecran/CarteTournois, ui/ecran/FicheTournoi, ui/carte, core/CatalogueTournois]
branche:
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 3. L'onglet Tournois est scindé en **« Mes tournois »** et **« Carte des tournois »**. Carte des tournois, deux présentations au choix : (1) **carte** avec les filtres à gauche et un bouton **« Voir plus »** par tournoi (plus de double-clic obligatoire) ; (2) **grille / agenda** ordonnée **par jour**, très lisible. La fiche d'un tournoi s'ouvre **toujours**, même sans pouvoir s'inscrire. **Distinction visuelle** (pastille + couleur + texte) : en cours, à venir, non accessible (motif), non accessible car **tableaux pleins** ou **aucun tableau à son niveau** (à calculer dans le cœur, testé).

## Critères de fin

- [ ] deux présentations
- [ ] quatre statuts distincts et testés
- [ ] captures clair et sombre regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
