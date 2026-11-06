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

- [x] deux présentations
- [x] quatre statuts distincts et testés
- [x] captures clair et sombre regardées
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- developpeur-interface (2026-11-06) : statut calculé dans le cœur (`core.StatutCarteTournoi`, 4 cas, + `Entree.tableauxAMonNiveauComplets`, testé) ; `PresentationCarteTournois` (pastilles, cartes, compteur, agenda par jour, testée) ; `PageTournois` : segment « Carte des tournois » / « Mes tournois » (B031 non touché), filtres à gauche, bascule Carte / Agenda par jour, « Voir plus » sur chaque tournoi, marqueurs grisés pour les 2 statuts bloquants.
- Classes CSS posées (section « carte des tournois (B030) » en fin de theme.css, minimale) : pastille-statut-carte + statut-carte-en-cours/-a-venir/-non-accessible/-complet, bouton-voir-plus, filtres-carte-tournois, legende-statuts-carte, agenda-jour(-aujourdhui/-titre/-liste), agenda-ligne-tournoi, motif-complet. Captures clair/sombre : docs/captures/B030/.
- Pour le suivant : le harnais v2 casse avant la carte (boutons « Créer un tournoi » / « Suivre le tournoi » introuvables, et compiler avec LANG=C.UTF-8) ; captures faites avec une copie du harnais qui saute l'étape en échec. Erreur CSS préexistante theme.css ligne 2816 (« Expected COLON »).
- designer-interface (2026-11-06) : charte v2 sur la carte des tournois — pastilles des 4 statuts avec puce (pleine : en cours / non accessible ; anneau : à venir / complet) en plus de la couleur et du texte (variables existantes, deux thèmes) ; agenda : pastilles de largeur fixe, détail qui passe à la ligne, motif aligné, jour courant encadré ; « Voir plus » compact en bleu ; légende séparée par un filet. Seul ajout Java : la puce en graphique de la pastille (`PageTournois.pastille`).
- Erreur CSS ligne 2816 corrigée (accolade et ouverture de commentaire perdues à une fusion B028 : la règle `.icone-tuile.accueil-club-tuile .icone-tuile-trace` et l'en-tête B033 sont de nouveau valides).
- Captures avant / après : docs/captures/B030/avant/ et apres/ (clair, sombre, 1440). Non vérifié : rendu Windows, mise à l'échelle.

## Résultat
- Livré : onglet « Carte des tournois » avec deux présentations (carte avec filtres et « Voir plus » ; agenda par jour), quatre statuts calculés dans le cœur (`core.StatutCarteTournoi`, testés), fiche ouverte par « Voir plus » quel que soit le statut, correction de la ligne 2816 de `theme.css`.
- Questions ouvertes dans le journal `docs/journal/2026-11-06-tache-B030-carte-tournois.md` : statut « En cours » face à « Non accessible » ; pastille « Complet » avec liste d'attente (option appliquée : A pour les deux, à confirmer par Quentin).
- Tests : 812, 0 échec (dernier passage vert). Captures clair et sombre regardées par le documentaliste.
- relecteur-code (2026-11-06) : liaisons OK (règle d'or, aucun switch sur énumération, « Voir plus » et double-clic → `ouvrirFiche` quel que soit le statut, marqueurs de la carte branchés sur `statut()`, pas de TODO ni de `setStyle`, aucune donnée de démo touchée). Aucun défaut bloquant.
- Mineur : `filtres-carte-tournois`, `agenda-tournois` et `carte-des-tournois` sont posées en Java mais sans règle dans theme.css (marqueurs de structure, sans effet) ; `PresentationCarteTournois.carte` suppose `acces()` non nul comme le reste du catalogue.
- Vérifié par lecture : toutes les autres classes posées existent dans theme.css (variables de thème, donc clair et sombre) ; la bascule Carte / Mes tournois a son retour (segment).
