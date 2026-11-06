---
id: B030
titre: Carte des tournois : vue carte avec « Voir plus », vue agenda par jour, statuts visibles
type: interface
priorite: P1
taille: L
statut: en-cours
depend-de: []
fichiers: [ui/ecran/PageTournois, ui/ecran/CarteTournois, ui/ecran/FicheTournoi, ui/carte, core/CatalogueTournois]
branche: tache/B030-carte-tournois (2026-11-05)
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

- developpeur-interface (2026-11-06) : statut calculé dans le cœur (`core.StatutCarteTournoi`, 4 cas, + `Entree.tableauxAMonNiveauComplets`, testé) ; `PresentationCarteTournois` (pastilles, cartes, compteur, agenda par jour, testée) ; `PageTournois` : segment « Carte des tournois » / « Mes tournois » (B031 non touché), filtres à gauche, bascule Carte / Agenda par jour, « Voir plus » sur chaque tournoi, marqueurs grisés pour les 2 statuts bloquants.
- Classes CSS posées (section « carte des tournois (B030) » en fin de theme.css, minimale) : pastille-statut-carte + statut-carte-en-cours/-a-venir/-non-accessible/-complet, bouton-voir-plus, filtres-carte-tournois, legende-statuts-carte, agenda-jour(-aujourdhui/-titre/-liste), agenda-ligne-tournoi, motif-complet. Captures clair/sombre : docs/captures/B030/.
- Pour le suivant : le harnais v2 casse avant la carte (boutons « Créer un tournoi » / « Suivre le tournoi » introuvables, et compiler avec LANG=C.UTF-8) ; captures faites avec une copie du harnais qui saute l'étape en échec. Erreur CSS préexistante theme.css ligne 2816 (« Expected COLON »).

## Résultat
