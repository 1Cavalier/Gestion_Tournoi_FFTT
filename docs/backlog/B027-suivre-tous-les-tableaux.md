---
id: B027
titre: Suivre tous les tableaux d'un tournoi publié, même sans y être inscrit
type: interface
priorite: P1
taille: L
statut: a-faire
depend-de: []
fichiers: [ui/ecran/FicheTournoi, ui/ecran/PageTournois, ui/ecran/MesTournois, nouvel écran ui/ecran (suivi public), core (vue publique en lecture)]
branche:
pr:
---

## Ce qu'il faut faire

Demande de Quentin (5 novembre, 17 h) : une fois le tournoi **publié**, et
surtout **le jour J**, l'affichage du tournoi doit changer pour qu'on puisse
**naviguer entre les tableaux**, **même si l'on n'est inscrit à aucun**.

Depuis l'onglet Tournois (fiche du tournoi) et « Mes tournois », pour tout
compte connecté :
- **avant le jour J** : la fiche actuelle (informations, tableaux, inscrits) ;
- **à partir du lancement du premier tableau** : la fiche bascule en **mode
  suivi** :
  - une barre ou une liste des **tableaux** (nom, couleur si B015 faite, étape :
    pointage / poules x sur y jouées / tableau final : tour en cours / terminé),
    et un clic pour passer de l'un à l'autre ;
  - pour le tableau choisi : les **poules** (composition, résultats, classement
    en direct, qualifiés), puis l'**arbre** du tableau final en **lecture
    seule** (réutiliser l'affichage de `VuePhaseFinale` / `DispositionArbre`
    sans la saisie), les **matchs en cours** avec leur table, le **podium**
    quand le tableau est fini ;
  - si le joueur connecté est inscrit : ses tableaux en premier et sa
    **prochaine partie** mise en avant (adversaire, table, tableau, tour) ;
  - mise à jour : bouton « Actualiser » et relecture automatique du fichier
    du tournoi toutes les 30 s environ.
- **Aucune action** possible dans ce mode (pas de saisie, pas d'envoi sur une
  table) : c'est le juge-arbitre qui pilote depuis la page Jour J (B011).

Logique d'affichage dans une classe `Presentation…` testée sans JavaFX ;
réutiliser `SuiviPoule`, `PresentationPhaseFinale`, `DispositionArbre`.

## Critères de fin

- [ ] un compte non inscrit navigue entre tous les tableaux d'un tournoi en cours (démo du jour J)
- [ ] poules, arbre et matchs en cours visibles en lecture seule
- [ ] le joueur inscrit voit sa prochaine partie en premier
- [ ] captures du mode suivi en clair et sombre, regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Ne pas toucher à la page Jour J du juge-arbitre (tâche B011, en parallèle
possible) : écrans séparés. Le suivi sur téléphone (B021) pourra réutiliser la
même logique de présentation.

## Passations

## Résultat
