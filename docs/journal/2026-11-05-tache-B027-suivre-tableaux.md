# B027 : suivre tous les tableaux d'un tournoi publié — 2026-11-05

- Branche : `tache/B027-suivre-tableaux`
- Agent : developpeur-interface, designer-interface, relecteur-code, documentaliste
- Pull Request : à créer (étape `integrateur`)
- Statut : en attente de validation visuelle (les écrans n'ont été vus qu'en captures d'essai ; la carte « prochaine partie » n'a pas été vue)

## Pour Quentin

Une fois un tournoi publié, et dès qu'un tableau est lancé, la fiche du tournoi
bascule en **mode suivi**. Tout compte connecté peut la voir, même sans être
inscrit à un tableau. On passe d'un tableau à l'autre par une barre en haut :
on voit l'étape de chaque tableau (pointage, poules, tableau final, terminé),
les matchs en cours avec leur table, les poules et leur classement, l'arbre du
tableau final et le podium. Rien ne se modifie dans ce mode : c'est le
juge-arbitre qui pilote depuis la page Jour J.

Si le compte est inscrit à un tableau, ses tableaux passent en premier et sa
prochaine partie est mise en avant (adversaire, table, tableau, tour).
Un bouton « Actualiser » et une relecture automatique toutes les 30 secondes
gardent l'écran à jour.

Avant le jour J, la fiche reste celle d'avant (informations, tableaux, inscrits).
Un bouton permet de revenir à la fiche complète à tout moment.

## À tester à l'écran

- Compte de démo : un compte de Brunoy (clubs de la démo). Le
  « Tournoi de démonstration du jour J » est visible pour ces comptes ; il
  contient six tableaux à différentes étapes. Quentin n'est **pas** inscrit à ce
  tournoi : la carte « prochaine partie » n'apparaîtra donc pas.
- Chemin : onglet Tournois → fiche de « Tournoi de démonstration du jour J » →
  bouton « Suivre le tournoi en direct » (le bouton « Informations du tournoi »
  revient à la fiche complète). Autre chemin : Mes tournois → « Suivre le tournoi ».
- Ce qu'il faut regarder (thème clair et sombre) :
  - la barre des tableaux : clic sur chaque tableau, étape lisible, ligne
    « en cours » bien délimitée (fond arrondi, texte qui ne touche pas le bord) ;
  - les cartes de poule : elles ont une largeur fixe ; sur un nom de club long,
    « pts » peut passer à la ligne (connu, non bloquant) ;
  - l'arbre du tableau final en lecture seule (pas de saisie) ;
  - le bouton « Actualiser » et l'absence de toute action de saisie.
- Captures d'essai : `outils-dev/run.sh <dossier> [sombre] suivi` (nouveau mode du harnais).

## Questions pour Quentin

### 2026-11-05 — Bascule en mode suivi hors jour J
- Contexte : la bascule se fait dès qu'un tableau d'un tournoi publié est lancé,
  même si la date du tournoi n'est pas aujourd'hui. C'est ce qui permet de
  montrer la démo du 14 novembre depuis une date antérieure.
- Article FFTT : aucun (choix d'interface).
- Options : A) bascule dès qu'un tableau est lancé (comportement actuel) ;
  B) bascule seulement le jour J (date du tournoi = aujourd'hui).
- Choix provisoire de l'agent : A (option la plus simple à garder, réversible).
- Réponse de Quentin :

### 2026-11-05 — « Inscriptions ouvertes » pendant le tournoi
- Contexte : la fiche du tournoi affiche encore le libellé « Inscriptions ouvertes »
  pendant le tournoi (libellé calculé par `core.CalendrierTournoi`, à partir des
  dates d'inscription, sans tenir compte de l'état du tournoi).
- Article FFTT : aucun.
- Options : A) ne rien changer (comportement actuel) ; B) masquer ce libellé en
  mode suivi ; C) afficher « Tournoi en cours » à la place.
- Choix provisoire de l'agent : A, faute de réponse ; aucune modification de code
  sans accord.
- Réponse de Quentin :

## Notes pour l'état du projet

- Classes principales : `core.SuiviTournoi` (bascule, ordre des tableaux, matchs
  en cours, prochaine partie) ; `ui.ecran.PresentationSuiviTournoi` (logique
  d'affichage, testée sans JavaFX) ; `ui.ecran.EcranSuiviTournoi` (écran en lecture
  seule). `FicheTournoi` et `MesTournois` gagnent le lien vers le suivi ;
  `VuePhaseFinale` extrait son arbre en lecture seule (`arbreLectureSeule`,
  comportement inchangé).
- Format de sauvegarde : inchangé.
- Démo : inchangée (`ContexteApplication.VERSION_DEMO` reste `.demo-brunoy-v15`).
- Feuille de route : B027 en cours ; B011 (page Jour J) non touchée.

## Fusion

À remplir par l'`integrateur` (résultat de `python3 outils-dev/verifier-fusion.py`
après synchronisation avec `main`).

## Chiffres

- Tests : 662 après la tâche, 0 échec (valeur relevée dans les passations, via
  `outils-dev/tests.sh`) ; valeur avant la tâche non relevée dans les passations.
- Version de la démo : inchangée (`.demo-brunoy-v15`).
