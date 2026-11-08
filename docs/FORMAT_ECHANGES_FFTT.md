# Fichiers d'échange FFTT — format provisoire PingManager v1 (B023)

**Provisoire.** Ni la FFTT ni SPIDD ne publient de format d'échange pour les
tournois individuels (voir `docs/RECHERCHE_FFTT.md` §2.2 et §4.1). Ce format
sert, en attendant, à importer la liste des licenciés et des clubs fournie par
le club, et à remettre les résultats au juge-arbitre (coexistence avec SPIDD).
Il sera remplacé par le format fédéral dès qu'il sera connu (IX.107 : format
« donné lors de l'homologation »).

Code : `importation.LecteurLicencesCsv`, `LecteurClubsCsv`,
`RegistreLicencesFichier`, `RegistreClubsFichier`, `ExportResultatsCsv`
(port `core.ExportResultatsFederaux`, préparation `core.ServiceResultatsFederaux`).

## Règles communes

- CSV, séparateur **point-virgule**, **UTF-8 avec BOM** (à la lecture : BOM
  facultatif, repli Windows-1252 ; .xlsx et .ods acceptés aussi).
- Une ligne d'en-têtes obligatoire ; colonnes lues **par leur nom** (ordre libre,
  casse et accents ignorés) ; colonnes inconnues ignorées.
- Dates `jj/mm/aaaa`, heures `hh:mm`.
- Lecture d'un fichier non fiable : 10 Mo au plus, 20 000 lignes au plus,
  cellule de plus de 200 caractères → ligne refusée ; caractères de contrôle et
  espaces de bord retirés ; ligne invalide refusée avec son numéro ; valeur
  facultative mal formée ignorée et signalée.
- Écriture : cellule entre guillemets si elle contient `;`, `"` ou un saut de
  ligne (guillemets doublés) ; cellule commençant par `=`, `+`, `-`, `@`,
  tabulation ou retour chariot préfixée d'une apostrophe (pas de formule
  exécutée à l'ouverture dans un tableur).

## `licencies.csv` (import)

| Colonne | Obligatoire | Contenu |
|---|---|---|
| Licence | oui | lettres et chiffres, 12 au plus ; doublon refusé |
| Nom, Prénom | oui | |
| Sexe | oui | `M` (ou `H`) / `F` |
| N° club | oui | 8 chiffres ; 7 chiffres complétés d'un zéro en tête |
| Club | non | nom du club |
| Points | oui | entier positif (points de début de phase) |
| Points mensuels | non | vide = Points |
| Catégorie, Nationalité, Type de licence | non | texte |
| Date de naissance | non | `jj/mm/aaaa` |
| Certificat | non | `EN_REGLE`, `SANS_PRATIQUE`, `INCONNU` (vide ou autre = `INCONNU`) |
| Relevé le | non | `jj/mm/aaaa` ; la date la plus ancienne est retenue |
| Validation | non | lue, pas encore conservée |

## `clubs.csv` (import)

`N° club` (oui, 8 chiffres), `Nom` (oui), `Département` (déduit du code postal
si vide), `Adresse 1`, `Adresse 2`, `Code postal` (5 chiffres), `Ville`,
`Courriel officiel`, `Nom du responsable`, `Prénom du responsable`.
Département, ville et courriel officiel sont exigés par le modèle `Club`
(courriel = destinataire du code de liaison) : sans eux la ligne est refusée.

## `parties.csv` (export)

`N° homologation` ; `Tableau` ; `Phase` (`POULE` / `TABLEAU`) ; `Étape`
(nom de poule ou de tour) ; `N° partie` (ordre dans le tableau) ; `Date` ;
`Heure` (fin, sinon lancement) ; `Table` ; `Licence A` ; `Nom A` ; `Prénom A` ;
`Club A` ; `Points A` ; idem `B` ; `Score` (manches du point de vue de A,
séparées par une espace : `11-7 9-11 11-5`) ; `Manches A` ; `Manches B` ;
`Vainqueur` (licence) ; `Issue` (`NORMALE`, `FORFAIT`, `ABANDON`,
`REFUS_DE_JOUER`, `DISQUALIFICATION`) ; `Fautif` (licence, vide si normale).
Parties « exempt » et places « à déterminer » non exportées.

## `classements.csv` (export)

`N° homologation` ; `Tableau` ; `Rang` ; `Licence` ; `Nom` ; `Prénom` ; `Club` ;
`Points`. 1er, 2e ; 3e (et 4e) par la petite finale, sinon deux 3es ex aequo ;
perdants d'un tour antérieur ex aequo au rang « nombre de parties du tour + 1 »
(5e en quarts, 9e en huitièmes). Joueurs éliminés en poule non classés.

Dans l'application, les fichiers sont enregistrés sous
`resultats-<tableau>-parties.csv` et `resultats-<tableau>-classements.csv`.
Aucune date de naissance ni certificat dans l'export.
