# REFACTOR_PLAN — nuit du 13 novembre 2026

## Constat de départ (mesuré, voir `REFACTOR_LOG.md`)

Le code est déjà propre : la recherche outillée ne trouve presque rien à retirer.

| Piste de la mission | Mesure sur `main` | Conclusion |
|---|---|---|
| Imports inutilisés | 0 | rien à faire |
| Membres privés jamais utilisés | 0 | rien à faire |
| Code commenté, `TODO`, `FIXME` | 0 | rien à faire |
| Blocs dupliqués à l'identique | 12 blocs de 8 lignes ou plus (124 lignes), 47 blocs de 5 lignes ou plus (321 lignes) sur 66 697 | quelques extractions utiles (T1 à T5) |
| Clones de même forme | presque uniquement les méthodes des builders (`InfosReglement`, `FicheLicence`, `ConfigurationTableau`) | incompressible sans dépendance (Lombok) ni changement d'API |
| Chaînes de `if` sur une énumération | 346 tests, dont 14 méthodes qui ne font que renvoyer une constante | tables de correspondance (T6) |
| Classes de valeur en `record` | déjà 177 records | reste : 20 classes internes privées, à examiner (T7) |
| Méthodes non privées jamais appelées | 21 (dont 16 accesseurs d'objets du modèle) | on ne retire que celles qui ne sont pas publiques (T8) ; les autres sont listées dans le rapport |
| Grosses classes | `EcranSalle` 1 476, `GenerateurReglement` 1 119, `ContexteApplication` 951, `Ui` 939, `Tournoi` 904 | voir « Écarté cette nuit » |

**Conséquence, dite franchement** : avec la parité fonctionnelle absolue, l'API publique
conservée, aucune dépendance nouvelle et l'interdiction du `switch` sur énumération, le
gain honnête en lignes est de l'ordre de **quelques centaines de lignes (moins de 1 %)**.
Aller plus loin demanderait de toucher à l'API (records à la place des classes à
accesseurs), aux écrans non vérifiables à l'œil cette nuit, ou d'ajouter Lombok : trois
choses que la mission ou `CLAUDE.md` interdisent. Le plan ci-dessous fait tout ce qui est
sûr, et le rapport proposera le reste comme décisions.

## Filets de sécurité, passés à chaque transformation

- **S1** tests JUnit (1 498 après la phase 1) ; **S2** surface publique (`javap`) : tout
  retrait est interdit, tout ajout est noté ; **S3** sorties de référence identiques
  (JSON des deux tournois de démo et leur relecture, règlement et feuille d'engagement en
  texte et en PDF, feuilles de poule, de match et arbre en PDF, pages HTML du suivi,
  exports CSV, salle, complétude, finances, simulations à graine fixe : 30 fichiers,
  2,4 Mo) ; **S4** parcours automatique de l'interface sous écran virtuel (arbre des
  composants de chaque écran atteint), pour toute transformation qui touche `ui`.

## Transformations, de la moins risquée à la plus risquée

| N° | Transformation | Fichiers | Gain estimé | Risque | Filets |
|---|---|---|---|---|---|
| T1 | `RenduPdfFeuilles` réutilise `propre`, `imprimable` et `rgb` de `RenduPdfReglement` (copies identiques ; le sens `ui → core` est permis, aucun nouvel import PDFBox dans le cœur) | `core/reglement/RenduPdfReglement`, `ui/infra/RenduPdfFeuilles` | −30 | très faible | S1 S2 S3 |
| T2 | Code de vérification à 6 chiffres : partie commune de `ServiceComptesOrganisateurs` et `ServiceLiaisonClub` | `core` | −10 | très faible | S1 S2 |
| T3 | Libellés d'une partie (nom et détail d'un joueur, issue, tour, détail du score, podium) écrits une seule fois dans le cœur, utilisés par le suivi sur téléphone, la phase finale, le suivi à l'écran et les feuilles ; les variantes voulues (« Score », « Match joué », « Éliminé ») restent chez l'appelant | `core`, `ui/diffusion/InstantaneSuivi`, `ui/ecran/Presentation*`, `core/feuilles/GenerateurFeuilles` | −80 | faible | S1 S2 S3 S4 |
| T4 | Échappement XML/HTML : examiner si `Html.echapper` et `GenerateurSsml.echapperXml` peuvent partager leur boucle (ils diffèrent sur l'apostrophe et les caractères interdits) | `core/GenerateurSsml`, `ui/diffusion/Html` | −10 ou abandon | faible | S1 S3 |
| T5 | Autres doublons de 5 à 9 lignes dans un même paquet (`CompteOrganisateur`/`CompteUtilisateur`, `EcranComplementaire`/`EcranSportif`, `LecteurClubsCsv`/`LecteurLicencesCsv`, `RegistreLicencesFichier`/`RegistreLicencesFFTT`) : extraction seulement si le résultat est plus court **et** plus clair | divers | −30 | faible | S1 S2 S3 S4 |
| T6 | Chaînes « `if (x == ENUM.A) return "…";` » → table `EnumMap` avec valeur par défaut (même résultat, y compris pour `null`) | `PresentationJourJ`, `PresentationPhaseFinale`, `DispositionArbre`, `StatutPastille`, `InstantaneSuivi`, `GenerateurFeuilles` | −80 | faible | S1 S3 S4 |
| T7 | Classes internes privées qui ne portent que des champs `final` → `record` privé | `Coquille.Entree` et semblables | −40 | faible | S1 S4 |
| T8 | Retrait des méthodes non publiques jamais appelées (ni par le code, ni par les tests) | `BlocSaisieSms`, `AssistantInscription`, `PageTournois`, `PresentationRemplissageFinances` | −30 | faible | S1 S4 |
| T9 | Finitions : références périmées dans les commentaires (`docs/…`, `QUESTIONS.md`, fichiers supprimés du dépôt), README (chiffres, section 2) | commentaires, `README.md` | 0 | nul | S1 |

## Écarté cette nuit (proposé dans le rapport)

- **Découper `EcranSalle`, `Ui`, `ContexteApplication`** : aucun gain en lignes, écrans
  couverts à 19,5 % et non vérifiables à l'œil, et ce sont les « points de passage
  communs » où `CLAUDE.md` demande des modifications minimales (d'autres sessions
  fusionnent dans `main` en parallèle : conflits assurés).
- **Classes à accesseurs → records** (`InfosReglement`, `FicheLicence`, `Club`…) : change
  les noms publics (`getNom()` → `nom()`), donc 215 classes de test à réécrire.
- **Modèle de configuration central** : il existe déjà (`ConfigurationTableau`,
  `InfosReglement`, `Tournoi`, avec builders, validation et valeurs par défaut au même
  endroit).
- **Retrait des 16 accesseurs publics jamais appelés** : API publique, on garde.
- **Formatage automatique de tout le projet** : aucun outil de formatage n'est configuré ;
  en imposer un réécrirait des milliers de lignes et gênerait les fusions en cours.
