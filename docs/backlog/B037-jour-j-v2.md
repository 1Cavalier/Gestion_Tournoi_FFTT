---
id: B037
titre: Jour J v2 : priorités en couleur, joueurs en attente, salle, lancer, terminer, annonce
type: jour-j
priorite: P1
taille: L
statut: a-faire
depend-de: []
fichiers: [ui/ecran/EcranJourJ, ui/ecran/EcranSalle, ui/ecran/PresentationJourJ, ui/ecran/PresentationSalle, core/ServiceSalle]
branche:
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 7. Sur l'écran du jour J (B011) : **matchs proposés** avec priorité en **couleur** rouge / orange / vert (attente longue, match important, stratégie choisie ; règle de calcul dans le cœur, testée) ; remplacer « bloqués » par la **liste des joueurs en attente avec leur chrono** ; clic sur un joueur → **pourquoi** il ne peut pas jouer et le ou les tableaux avec **son nom surligné** ; **voir le jour suivant** ; tableaux dans l'**ordre des horaires de début** ; « Tables » devient **« Salle »** (grille simple tant que le plan B024 n'existe pas) ; bouton **« Lancer un match »** manuel ; gros bouton **« Terminer la rencontre / la poule »**, petit bouton rouge **« Annuler »** avec confirmation ; au lancement : **« Comment annoncer la rencontre ? »** — rien / SMS / appel vocal / les deux, choix **mémorisé** par tournoi et modifiable (les envois réels viendront avec B017 et B019 : en attendant, l'annonce est affichée à l'écran).

## Critères de fin

- [ ] priorités et attente calculées et testées
- [ ] jour suivant, lancer, terminer, annuler avec confirmation
- [ ] choix d'annonce mémorisé
- [ ] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

## Résultat
