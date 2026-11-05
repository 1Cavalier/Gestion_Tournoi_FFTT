---
id: B027
titre: Suivre tous les tableaux d'un tournoi publié, même sans y être inscrit
type: interface
priorite: P1
taille: L
statut: en-cours
depend-de: []
fichiers: [ui/ecran/FicheTournoi, ui/ecran/PageTournois, ui/ecran/MesTournois, nouvel écran ui/ecran (suivi public), core (vue publique en lecture)]
branche: tache/B027-suivre-tableaux (prise le 2026-11-05)
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

- developpeur-interface (5 nov.) : cœur `core.SuiviTournoi` (bascule, ordre des tableaux, matchs en cours, prochaine partie, 6 tests) + `PresentationSuiviTournoi` (13 tests sur la démo du jour J) + `EcranSuiviTournoi` lecture seule (barre des tableaux, matchs en cours, podium, arbre, poules, Actualiser + relecture 30 s) ; bascule dans `FicheTournoi.build()` (bouton « Informations du tournoi » / « Suivre le tournoi en direct »), libellé « Suivre le tournoi » dans Mes tournois ; arbre en lecture seule extrait de `VuePhaseFinale` (`arbreLectureSeule`, `legendeArbre`, comportement inchangé) ; CSS section « Suivi d'un tournoi en cours (B027) ».
- Captures clair/sombre regardées (`outils-dev/run.sh <dossier> [sombre] suivi`, nouveau mode du harnais). Non vu en capture : la carte « prochaine partie » (Quentin n'est pas inscrit à la démo du jour J), couverte par les tests. 662 tests, 0 échec (`outils-dev/tests.sh`, JavaFX 11 d'apt).
- Question pour Quentin : la fiche bascule pour un tournoi PUBLIE dont un tableau est lancé, même si la date du tournoi n'est pas aujourd'hui (démo du 14 nov.) ; faut-il aussi exiger le jour J ?
- designer-interface (5 nov.) : passe charte v2 sur `EcranSuiviTournoi` et sa section CSS : tokens tous déclarés dans les deux thèmes, aucun hexadécimal ni `setStyle`, contrastes ≥ 4,5:1 (vérifiés clair et sombre, 1440 px).
- Seule correction : `.ligne-liste.suivi-ligne-en-cours` (fond arrondi 8, décollé du filet, retrait de 10 px : le texte touchait le bord bleu). 662 tests, 0 échec.
- À reprendre plus tard (non bloquant, Java) : cartes de poule à largeur fixe 540 → largeurs inégales quand les badges « Place qualificative » s'ajoutent, et détail club long replié sur 2 lignes (« … · 1274 / pts »).

## Résultat
