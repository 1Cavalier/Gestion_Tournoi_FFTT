---
id: B031
titre: Mes tournois : tournoi en cours, inscriptions et agenda, tournois passés
type: interface
priorite: P1
taille: M
statut: fait
depend-de: [B032]
fichiers: [ui/ecran/MesTournois]
branche: tache/B031-mes-tournois (2026-11-06)
pr:
---

## Ce qu'il faut faire

Référence : `docs/CAHIER_DES_CHARGES_V0.md`, section 3. En premier le **tournoi en cours** avec accès direct au suivi (B027). Puis **mes inscriptions** triées par date et l'**agenda** (B032 : dates personnelles, export .ics). Puis les **tournois passés** avec mes **performances** (victoires, défaites) ; un clic rouvre ma poule, les scores, le tableau principal (lecture seule, réutiliser `EcranSuiviTournoi`). **Retirer** la partie « organisés par le club ».

## Critères de fin

- [x] trois parties dans l'ordre
- [x] performances calculées (testé)
- [x] captures regardées
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Voir le cahier des charges v0.

## Passations
- developpeur-interface : `MesTournois` refait (en cours → inscriptions + agenda → passés) sur `PresentationMesTournois` (testée) ; performances dans `core.PerformancesJoueur` (testé) ; « organisés par le club » retiré ; désinscription via `ContexteApplication.desinscrireEnLigne` (enregistre tournoi + dépôt) ; agenda synchronisé à l'ouverture, dates perso ajout/retrait, export .ics (FileChooser).
- Tournoi passé : `EcranSuiviTournoi.enArchive()` (badge « Terminé », sans relecture auto ni « Actualiser »), ouvert par `PageTournois.revoirTournoi`. CSS : section « Mes tournois (B031) » en fin de theme.css ; corrigé au passage la fusion B030 qui avait cassé theme.css (règles `.agenda-jour-titre` / bandeau B029 entremêlés, erreurs d'analyse CSS l. 3037-3090).
- Harnais : mode `mestournois` (captures clair/sombre regardées). La démo n'a ni inscription à venir ni tournoi passé joué : désinscription et « Revoir poule et tableau » non capturés.
- designer-interface : réparation de theme.css vérifiée règle par règle contre B029 (b8e03ff) et B030 (7e49663) : 0 règle perdue, 0 doublon, accolades équilibrées, aucune erreur CSS au harnais.
- Charte v2 (section B031, variables existantes, clair et sombre) : liseré primaire complet + filet gauche pour le tournoi en cours, bloc date neutre pour les passés, étiquettes d'agenda de même largeur (titres alignés), espacements. Harnais : inscription préalable à Créteil (carte « Mes inscriptions » et désinscription capturées) ; captures avant/après dans docs/captures/B031/.
- Non capturé : carte « Tournoi en cours » (aucun tournoi de démo à la date du jour) et badge « Terminé ». Attention : `PresentationMesTournoisTest` échoue si la locale n'est pas UTF-8 (fichier « Passé.json ») ; vert avec LANG=C.UTF-8.
- correcteur : dépendance à la locale corrigée dans le test (helper `tf` : nom de fichier ASCII, `Path.of` refusait « Passé.json » sous LANG=C) ; titre du tournoi « Passé » inchangé.
- `mvn -B test` : 823 tests, 0 échec, avec LANG=C et LANG=C.UTF-8.

## Résultat
- Livré : onglet « Mes tournois » en trois parties (tournoi en cours, mes inscriptions et agenda avec export .ics, tournois passés en lecture seule avec mes performances). Partie « organisés par le club » retirée. Journal : `docs/journal/2026-11-06-tache-B031-mes-tournois.md`.
- Question ouverte pour Quentin : abandon et forfait comptés en victoires/défaites et signalés à part (option appliquée) ou exclus. Défaut connu non bloquant : « Informations du tournoi » depuis un tournoi passé ouvre le suivi en direct.
- Javadoc présente sur `PerformancesJoueur`, `PresentationMesTournois` et `MesTournois`. Tests : 812 → 830, 0 échec. Démo inchangée.
- verificateur-modele : `PerformancesJoueur` +5 tests (poule seule, abandon/refus/disqualification, liste d'attente, petite finale, nulls) et `DesinscriptionEnLigneContexteTest` (2 tests : tournoi et agenda relus à froid, refus sans effet). Aucun défaut.
- Règle d'or : aucun import interdit ajouté dans model/core/ordonnancement. Note : pas de « double forfait » dans le modèle (un seul fautif par match).
- `mvn -B test` : 830 tests, 0 échec.
- relecteur-code : liaisons OK (export .ics FileChooser vers ExportIcs, désinscription suivie de sauvegarde tournoi + agenda, réouverture en lecture seule via enArchive, partie club retirée de MesTournois, pas de switch, pas d'import interdit). Aucun défaut bloquant.
- Remarques mineures : « Informations du tournoi » depuis un tournoi passé repasse par FicheTournoi.build, donc suivi direct « En cours » avec relecture auto (lecture seule, non bloquant) ; `mesTournoisClub` n'est plus utilisé que par un test et `.intertitre-groupe` du CSS plus que par du CSS ; classes marqueurs sans règle CSS (mes-tournois-en-cours, -inscriptions, -passes, -agenda, mes-agenda-liste).
