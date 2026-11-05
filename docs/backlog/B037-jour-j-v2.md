---
id: B037
titre: Jour J v2 : priorités en couleur, joueurs en attente, salle, lancer, terminer, annonce
type: jour-j
priorite: P1
taille: L
statut: en-cours
depend-de: []
fichiers: [ui/ecran/EcranJourJ, ui/ecran/EcranSalle, ui/ecran/PresentationJourJ, ui/ecran/PresentationSalle, core/ServiceSalle]
branche: tache/B037-jour-j-v2 (prise le 2026-11-05)
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 7. Sur l'écran du jour J (B011) : **matchs proposés** avec priorité en **couleur** rouge / orange / vert (attente longue, match important, stratégie choisie ; règle de calcul dans le cœur, testée) ; remplacer « bloqués » par la **liste des joueurs en attente avec leur chrono** ; clic sur un joueur → **pourquoi** il ne peut pas jouer et le ou les tableaux avec **son nom surligné** ; **voir le jour suivant** ; tableaux dans l'**ordre des horaires de début** ; « Tables » devient **« Salle »** (grille simple tant que le plan B024 n'existe pas) ; bouton **« Lancer un match »** manuel ; gros bouton **« Terminer la rencontre / la poule »**, petit bouton rouge **« Annuler »** avec confirmation ; au lancement : **« Comment annoncer la rencontre ? »** — rien / SMS / appel vocal / les deux, choix **mémorisé** par tournoi et modifiable (les envois réels viendront avec B017 et B019 : en attendant, l'annonce est affichée à l'écran).

## Critères de fin

- [x] priorités et attente calculées et testées
- [x] jour suivant, lancer, terminer, annuler avec confirmation
- [x] choix d'annonce mémorisé
- [x] captures regardées
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations

- `developpeur-jour-j` (2026-11-05) : tout implémenté (cœur `ReglePrioriteSalle`, joueurs en attente, `AnnonceRencontre`, `ModeAnnonce` + JSON ; écran `EcranSalle`/`EcranJourJ` ; présentation testée). 675 → 703 tests verts.
- Captures regardées (clair et sombre) : `docs/captures/jour-j-v2/apres/` ; démo sur un seul jour, « Jour suivant » non capturé. 4 questions dans le journal.
- À relire : `EcranSalle` (gros fichier, flux « annonce » avant chaque lancement), `resumeLigne` devenue inutilisée par l'écran.
- `verificateur-modele` (2026-11-05) : +5 tests (seuils 9:59/10/20, attente nulle/négative, annonce, JSON tous modes), 708 verts. Invariants et cas limites OK, aucun défaut bloquant.
- Mineur : `modeAnnonce` de type non texte dans le JSON (nombre, booléen) lève `Json` au lieu de redemander ; rouge dès 10 min pour tout tour de 2 parties réelles ou moins (y compris 1er tour à byes).
- Règle d'or : aucun import interdit ajouté.
- `correcteur` (2026-11-05) : `lireModeAnnonce` n'accepte qu'une chaîne (nombre, booléen, objet, tableau → redemander) ; test `mode_d_annonce_tous_les_modes_et_valeurs_degenerees` étendu (rouge avant, vert après).
- `EcranSalle.choixAnnonce` : « Retenir ce choix » enregistre aussitôt le tournoi (même si le lancement échoue), erreur d'enregistrement affichée. Non testé : logique JavaFX non extractible simplement.
- 708 tests, 0 échec.

## Résultat
