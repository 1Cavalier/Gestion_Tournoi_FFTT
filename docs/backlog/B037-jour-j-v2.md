---
id: B037
titre: Jour J v2 : priorités en couleur, joueurs en attente, salle, lancer, terminer, annonce
type: jour-j
priorite: P1
taille: L
statut: fait
depend-de: []
fichiers: [ui/ecran/EcranJourJ, ui/ecran/EcranSalle, ui/ecran/PresentationJourJ, ui/ecran/PresentationSalle, core/ServiceSalle]
branche: tache/B037-jour-j-v2 (prise le 2026-11-05)
pr: 33
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 7. Sur l'écran du jour J (B011) : **matchs proposés** avec priorité en **couleur** rouge / orange / vert (attente longue, match important, stratégie choisie ; règle de calcul dans le cœur, testée) ; remplacer « bloqués » par la **liste des joueurs en attente avec leur chrono** ; clic sur un joueur → **pourquoi** il ne peut pas jouer et le ou les tableaux avec **son nom surligné** ; **voir le jour suivant** ; tableaux dans l'**ordre des horaires de début** ; « Tables » devient **« Salle »** (grille simple tant que le plan B024 n'existe pas) ; bouton **« Lancer un match »** manuel ; gros bouton **« Terminer la rencontre / la poule »**, petit bouton rouge **« Annuler »** avec confirmation ; au lancement : **« Comment annoncer la rencontre ? »** — rien / SMS / appel vocal / les deux, choix **mémorisé** par tournoi et modifiable (les envois réels viendront avec B017 et B019 : en attendant, l'annonce est affichée à l'écran).

## Critères de fin

- [x] priorités et attente calculées et testées
- [x] jour suivant, lancer, terminer, annuler avec confirmation
- [x] choix d'annonce mémorisé
- [x] captures regardées
- [x] `mvn -B test` à 0 échec (708 tests, mesuré le 2026-11-05)

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
- `documentaliste` (2026-11-05) : journal mis en forme (`docs/journal/2026-11-05-tache-B037-jour-j-v2.md`), questions regroupées, défauts mineurs notés, chiffres mesurés (708). `REGLES_CREATION_TOURNOI.md` : note sur `modeAnnonce` (hors création). `CONFORMITE_FFTT.md` non modifié.

## Résultat

Jour J v2 livré sur la branche `tache/B037-jour-j-v2` : priorités rouge / orange / vert calculées dans le cœur et testées, joueurs en attente avec chrono, « pourquoi » par joueur, jour suivant, « Lancer un match », « Terminer la rencontre / la poule », « Annuler » avec confirmation, choix d'annonce mémorisé par tournoi. 675 → 708 tests, 0 échec. Validation visuelle à faire par Quentin (captures dans `docs/captures/jour-j-v2/apres/`). Fusion laissée à l'intégrateur.
