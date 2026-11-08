# Rapport d'audit de sécurité — PingManager

- **Date** : 5 novembre 2026
- **Branche** : `audit/securite` (aucun code de production modifié)
- **Périmètre** : tout le dépôt (`src/main`, `src/test`, ressources, `pom.xml`, `docs/`, `outils-dev/`, historique git : 8 commits)
- **Méthode** : lecture du code (pas de supposition), vérifications à l'exécution (droits des fichiers, rendu PDF, parseurs), tests JUnit 5 de démonstration
- **Tests** : `mvn -B test` → **426 tests, 0 échec, 0 erreur** (412 existants + 14 ajoutés par cet audit)

---

## 1. Résumé

PingManager est aujourd'hui un **logiciel de bureau mono-poste** : toutes les données sont dans
`~/.pingmanager/pingmanager` (ou `$XDG_DATA_HOME/pingmanager`), en JSON. Dans ce modèle, quelqu'un
qui a la main sur la session du système d'exploitation peut de toute façon lire et modifier les
fichiers : les contrôles de l'interface ne protègent que **les utilisateurs du logiciel entre eux**
(poste partagé du club, PC de la table de marque, joueurs qui s'inscrivent sur le poste du club).
Le rapport distingue donc pour chaque constat le risque **bureau** (réel aujourd'hui) et le risque
**web** (qui deviendra réel au passage en ligne).

**Points forts constatés** (à conserver) :
- XML durci à l'import (DTD refusée, entités externes désactivées, XInclude coupé) : pas de XXE ;
- PBKDF2-HMAC-SHA256, sel aléatoire de 16 octets (`SecureRandom`), comparaison en temps constant ;
- codes de vérification tirés par `SecureRandom`, seule leur empreinte est gardée ;
- écriture par fichier temporaire + renommage ; fichiers JSON créés en **0600** sur Linux/macOS (vérifié) ;
- carte : Leaflet embarqué (pas de CDN), noms de tournois échappés côté Java (`CarteTournois.js`) **et**
  côté page (`echapper()`) : pas d'injection HTML/JS par un nom de tournoi ;
- HTML et PDF du règlement : PDFBox encode le texte (le rendu HTML, inutilisé, a été retiré le 6 novembre 2026) ;
- géocodage en HTTPS, délais courts (2 s / 3 s), pas de redirection suivie, aucune donnée personnelle envoyée ;
- aucune clé d'API, aucun jeton, aucune clé privée dans l'arbre ni dans l'historique ;
- pas de désérialisation Java, pas de réflexion pilotée par les données, pas d'exécution de processus.

**Points faibles majeurs** : ce qui est fait pour la **démonstration** est livré tel quel dans le
produit. Un compte « ultra admin » au mot de passe écrit dans le code, le compte personnel réel de
Quentin et l'accès « administrateur principal » de Brunoy pré-attribué à une adresse email sont
créés **sur chaque poste**. Combinés à une création de compte sans vérification d'email et à une
liaison de licence sans preuve, ils permettent à n'importe quel utilisateur d'un poste d'obtenir les
droits d'un autre. Ces points doivent être corrigés **avant toute diffusion payante** (lot 1).

### Constats par gravité

| Gravité  | Nombre | Identifiants |
|----------|-------:|--------------|
| Critique | 1 | S-01 |
| Élevée   | 3 | S-02, S-03, S-05 |
| Moyenne  | 11 | S-04, S-06, S-07, S-08, S-09, S-10, S-11, S-12, S-13, S-15, S-16 |
| Faible   | 5 | S-14, S-17, S-18, S-19, S-20 |
| Info     | 3 | S-21, S-22, S-23 |
| **Total** | **23** | |

---

## 2. Tableau des constats (triés par gravité)

Colonne « Bureau / Web » : risque réel pour le logiciel de bureau actuel / risque au passage web.

| Id | Gravité | Fichier:ligne | Description | Scénario d'exploitation | Correctif proposé | Bureau / Web |
|----|---------|---------------|-------------|-------------------------|-------------------|--------------|
| S-01 | **Critique** | `ui/infra/DonneesDemo.java:35-37`, `ui/ContexteApplication.java:170-178, 298-300`, `ui/AppPingManager.java:34` | Compte « ultra admin » (supervision et modification de **tous** les tournois de tous les clubs) créé à chaque démarrage avec le mot de passe (mot de passe en clair, masqué dans ce rapport) écrit dans le code, la doc (`docs/passation/*.md`) et affiché sur la console. Le privilège dépend uniquement de l'email (`ULTRA_ADMINS`). | Un bénévole, un joueur ou un acheteur qui a lu la doc (ou décompilé le jar) se connecte en ultra admin sur le poste du club et modifie ou annule les tournois. | Supprimer toute création de compte privilégié dans la version vendue ; profil démo activé explicitement (`-Dpingmanager.demo=true`) ; supervision réservée à un rôle stocké et non à un email codé en dur ; ne plus afficher le mémo sur la console. | Critique / Critique |
| S-02 | **Élevée** | `ui/infra/DonneesDemo.java:30-31, 40-42, 93-107, 129-150`, `ui/ContexteApplication.java:140-153`, `docs/passation/*.md`, tests | Données personnelles **réelles** dans le code et la doc : email, mot de passe (mot de passe en clair, masqué dans ce rapport), n° de licence et date de naissance de Quentin ; email de Serge BOULIER. Le compte de Quentin (admin de Brunoy) est créé sur chaque poste. | Tout acheteur obtient un compte administrateur de Brunoy et les données de deux personnes réelles ; si le dépôt GitHub est public, ces données y sont exposées (RGPD : base légale et minimisation absentes). | Remplacer par des identités fictives (`@exemple.fr`, licences 99…) ; ne créer aucun compte par défaut hors mode démo ; si le dépôt a été public, changer le mot de passe Gmail s'il était réutilisé et envisager la réécriture de l'historique (décision de Quentin). | Élevée / Élevée |
| S-03 | **Élevée** | `ui/ContexteApplication.java:131-137, 388-398`, `core/ServiceAuthentification.java:199-211` | La création de compte ne vérifie pas l'email. Or des accès sont **pré-enregistrés par email** (référent de Brunoy en `ADMIN_PRINCIPAL`). | Quiconque crée un compte avec `<email du référent de Brunoy>` (mot de passe « x » accepté) devient immédiatement administrateur principal de Brunoy. Avec la future base FFTT (référents pré-enregistrés par club), cela vaudra pour **tous** les clubs. | Vérifier l'email à la création (code envoyé à l'adresse, compte inactif tant que non confirmé) ; n'activer un accès pré-enregistré qu'après cette vérification. | Élevée / Critique |
| S-05 | **Élevée** | `core/ServiceLiaisonLicence.java:39-42`, `model/CompteUtilisateur.java:114`, `ui/ContexteApplication.java:433-441, 682-689` | Lier une licence ne demande aucune preuve et aucune unicité : plusieurs comptes peuvent porter la même licence. | Un compte se lie à la licence d'un tiers, lit sa fiche (date de naissance, nationalité, points) et l'inscrit à des tournois (« à régler ») sans son accord. | Unicité licence ↔ compte ; preuve de possession (code à l'email connu de la FFTT, ou validation par le club) ; journaliser les inscriptions faites au nom d'une licence. | Élevée / Critique |
| S-04 | Moyenne | `core/ServiceLiaisonClub.java:109-118`, `core/DemandeVerification.java:21-37` | La `DemandeVerification` ne mémorise pas le **club** visé ; `confirmer` reçoit le numéro de club séparément. | Demander un code pour **son** club (code reçu dans sa boîte), puis le confirmer avec le numéro d'un **autre** club : accès accordé. L'écran actuel (`PageCompte.java:263`) repasse le même numéro, donc pas exploitable par l'interface aujourd'hui, mais tout nouvel appelant (API web) l'est. | Ajouter `numeroClub` à `DemandeVerification` et refuser toute divergence (`ResultatLiaisonClub.demandeNonConforme()`). | Moyenne / Critique |
| S-06 | Moyenne | `ui/ecran/FicheTournoi.java:373-379`, `ui/ecran/EcranComplementaire.java:157-159, 657`, `persistence/ConfigMapper.java:268-276` | Le « règlement déposé » est un **chemin absolu libre** stocké dans le fichier du tournoi, relu sans contrôle, puis recopié tel quel par l'onglet Tournois (accessible à tout compte). Le PDF n'est ni copié dans le dossier de données ni vérifié. | Un fichier `.ptt.json` reçu d'un autre club, ou un organisateur, fait pointer le règlement vers `pingmanager-application.json` (empreintes de tous les comptes) : un joueur qui clique « Règlement (PDF)… » l'exporte. Le règlement homologué peut aussi changer après coup (fichier externe), et le lien casse sur un autre poste. Au web : lecture arbitraire de fichiers du serveur. | Copier le PDF dans `dossierDonnees/documents/<id>.pdf` (nom généré, contrôle `%PDF-` et taille ≤ 20 Mo, empreinte SHA-256 stockée) ; ne stocker qu'un nom relatif ; refuser tout chemin hors de ce dossier à la lecture. | Moyenne / Critique |
| S-07 | Moyenne | `importation/LecteurTableur.java:92, 157, 406-408, 524-535, 585-592` | Import : (a) `<row r="2000000000">` crée autant de lignes vides (la limite de 20 000 porte sur le nombre d'éléments, pas le numéro) ; (b) chaque entrée zip est lue **entièrement** avant le contrôle des 200 Mo (bombe zip : une entrée de 1,9 Go décompressés tient dans 2 Mo) ; (c) 200 Mo de XML en DOM dépasse la mémoire ; (d) taille du fichier non bornée ; (e) `NumberFormatException` non convertie en `ImportException`. | Un « tableur des inscrits » envoyé par email au club fige ou fait tomber le logiciel (`OutOfMemoryError`) au moment de l'import, éventuellement le jour du tournoi. | Refuser `r` > `MAX_LIGNES` ; lecture d'entrée bornée (flux limité à 20 Mo par entrée, 50 Mo au total, ratio de compression ≤ 100) ; fichier ≤ 20 Mo ; convertir toute `RuntimeException` en `ImportException`. | Moyenne / Élevée |
| S-08 | Moyenne | `core/ServiceLiaisonClub.java:129`, `core/ServiceComptesOrganisateurs.java:191`, `ui/ContexteApplication.java:405-410`, `ui/ecran/EcranConnexion.java:185-204` | Aucune limite d'essais : ni sur le code à 6 chiffres (10⁶ valeurs, 30 min), ni sur la connexion. Énumération des comptes : « Un compte existe déjà avec cet email », et réponse immédiate si l'email est inconnu (PBKDF2 sinon). | Au web, un script teste les codes ou les mots de passe sans frein ; sur le poste, essais manuels illimités. | Compteur d'essais dans la demande (5 puis invalidation) ; temporisation progressive à la connexion ; hachage factice si email inconnu ; message de création neutre. | Faible / Élevée |
| S-09 | Moyenne | `persistence/json/JsonParser.java:37-112`, appelants `ui/ContexteApplication.java:247-251, 624-628`, `importation/LecteurTableur.java:114-117`, `ui/infra/GeocodeurGeoplateforme.java:55` | Analyseur récursif sans limite de profondeur ni de taille : `[[[[…` provoque un `StackOverflowError`, qui est une `Error` et n'est pas interceptée par les `catch (RuntimeException)` / `catch (Exception)`. | Un `.ptt.json` piégé déposé dans le dossier `tournois` (ou ouvert via les récents) empêche **le démarrage** du logiciel, à chaque lancement (le nettoyage et la liste lisent tous les fichiers). Même effet sur l'import JSON de joueurs. | Profondeur maximale (64) et taille maximale (50 Mo) dans `JsonParser`, levant une `JsonException` ; à défaut, `catch (StackOverflowError)` local converti en `JsonException`. | Moyenne / Élevée |
| S-10 | Moyenne | `ui/ecran/CarteTournois.java:53-55, 68-83`, `ui/ecran/PageTournois.java:356`, `resources/.../carte/leaflet.js` (attribution) | Le pont Java `window.java` est injecté à **chaque** chargement réussi, quelle que soit la page ; la navigation de la WebView n'est pas bloquée. Le lien « Leaflet » de l'attribution charge `https://leafletjs.com` **dans** la WebView, qui reçoit alors le pont. `Pont.ouvrir(id)` accepte n'importe quel chemin (`Path.of(id)`). | Un site tiers chargé dans la WebView appelle `java.ouvrir("/chemin/quelconque")` ; selon la version d'OpenJFX, l'accès réflexif via le pont (`getClass()`) est un vecteur historique d'exécution de code. La carte est aussi perdue pour l'utilisateur. | N'injecter le pont que si `moteur.getLocation()` est la page locale ; bloquer toute navigation (écouteur sur `locationProperty`, ouverture des liens dans le navigateur système via `HostServices`) ; `attributionControl.setPrefix(false)` ou lien neutralisé ; `Pont.ouvrir` n'accepte que les identifiants affichés ; ajouter une CSP `<meta>` (`default-src 'none'; img-src https://data.geopf.fr https://tile.openstreetmap.org data:; style-src 'unsafe-inline'; script-src 'unsafe-inline'`). | Moyenne / sans objet (pas de WebView côté web) |
| S-11 | Moyenne | `pom.xml:18-19, 29-38, 41-45` | JavaFX **21.0.5** (octobre 2024) : la WebView embarque WebKit, corrigé à chaque mise à jour trimestrielle (21.0.6 et suivantes) ; deux ans de correctifs manquent. PDFBox **2.0.29** : pas de CVE connue (à ma connaissance) pour un usage en écriture seule, mais branche 2.0 en fin de maintenance. JUnit 5.10.2 : test uniquement. | La WebView charge des tuiles distantes (HTTPS) : une faille WebKit exploitable par image ou contenu malformé reste ouverte. | Monter JavaFX à la dernière 21.0.x LTS ; PDFBox à la dernière 2.0.x (puis 3.0.x) ; mettre en place une veille (OWASP dependency-check ou Dependabot — nouveau plugin : accord de Quentin requis). | Moyenne / Moyenne |
| S-12 | Moyenne | `ui/ecran/EcranConnexion.java:169`, `core/ServiceAuthentification.java:199-211`, `ui/infra/HachageMotDePassePBKDF2.java:33-36` | Aucune politique de mot de passe (1 caractère accepté) ni contrôle de format d'email. | Comptes d'organisateurs devinables ; adresses invalides qui bloqueront la vérification par email. | Longueur minimale de 10 caractères, liste de mots de passe courants refusés, format d'email vérifié (`Resultat…` avec motif). | Moyenne / Élevée |
| S-13 | Moyenne | `ui/ContexteApplication.java:306-319, 582-602, 682-689`, `ui/ecran/EcranCompletion.java:69`, `ui/ecran/Coquille.java:117-135`, `model/NiveauAcces.java:18-19` | Les habilitations ne sont vérifiées **que** par l'affichage de la navigation : `chargerTournoi`, `enregistrerTournoi`, `EcranCompletion` n'en vérifient aucune. Repli par **nom** de club quand le n° d'affiliation manque (homonymes : « ASPTT », « US … »). Niveaux d'accès sans effet (un « niveau 1 » peut tout faire ; le principal ne peut pas retirer un membre). Mode supervision sans journal d'audit. Pas de verrouillage de session (poste partagé). | Sur le poste : un tournoi sans n° d'affiliation d'un club homonyme apparaît comme le sien et est modifiable. Au web : tout appel direct contourne l'interface. | Méthode unique `exigerDroit(tournoi, action)` dans le contexte, appelée par toute lecture/écriture de gestion ; supprimer le repli par nom (migrer les anciens fichiers) ; matrice des droits par niveau ; journal des actions de supervision (qui, quand, quoi) ; verrouillage après inactivité. | Moyenne / Critique |
| S-15 | Moyenne | `persistence/TournoiMapper.java:122-156`, `persistence/ApplicationMapper.java:75-165`, `persistence/DepotTournoi.java:49-53`, `ui/ContexteApplication.java:232-269` | RGPD : fichiers en clair contenant nom, prénom, licence, sexe, catégorie d'âge, points des joueurs, **sanctions disciplinaires** (cartons, motifs), téléphone et email du correspondant, comptes et empreintes. Aucune durée de conservation, archives jamais purgées, pas d'export ni d'effacement, pas d'information des personnes. Tuiles et géocodage transmettent l'adresse IP à l'IGN et à OpenStreetMap. | Perte ou vol du portable du club = fuite des données de centaines de joueurs ; demande d'effacement d'un joueur impossible à satisfaire. | Registre de traitement et mention d'information ; purge des tournois clos après N mois (paramétrable) ; fonction « effacer un joueur » ; chiffrement au repos optionnel (clé dérivée du mot de passe) ; ne pas journaliser de données personnelles. | Moyenne / Élevée |
| S-16 | Moyenne | `ui/infra/EmailConsole.java:59-75`, `ui/ContexteApplication.java:107` | Le seul envoi d'email existant écrit le **code de vérification** et les identités en clair sur la sortie standard. Aucun envoi réel : en version vendue, la preuve « accès à la boîte officielle du club » n'existe pas. | Lancé depuis un terminal (ou avec une console attachée sous Windows), le demandeur lit lui-même le code et se rattache à n'importe quel club du référentiel. | Ne jamais écrire le code dans un journal ; implémentation SMTP (TLS obligatoire, `STARTTLS` exigé, identifiants hors du code, délais) ou service tiers ; `EmailConsole` réservée au mode démo. | Moyenne / Élevée |
| S-14 | Faible | `persistence/DepotTournoi.java:22-24, 65-86`, `persistence/DepotApplication.java:261-272`, `ui/ContexteApplication.java:105` | Écriture « atomique » sans `fsync` (le commentaire l'annonce pourtant) ; temporaire non supprimé en cas d'échec ; pas de verrou entre deux instances (le dernier écrivain écrase les comptes et accès) ; pas de copie `.bak` ; un dépôt applicatif illisible empêche le démarrage. Dossiers en 0755 (noms de tournois visibles par les autres comptes du système), marqueurs en 0644. | Coupure de courant en salle ou double lancement : perte des comptes ou du dernier état du tournoi. | `FileChannel.force(true)` avant le renommage ; suppression du temporaire dans un `finally` ; `FileLock` sur un fichier `.verrou` ; garder `pingmanager-application.json.bak` ; dossiers créés en 0700. | Faible / sans objet |
| S-17 (corrigée le 2026-11-08) | Faible | `core/reglement/RenduPdfReglement.java` (`imprimable`) | Un caractère absent de la police Inter (émoji, idéogramme) fait échouer **toute** la génération PDF (`IllegalArgumentException: No glyph for U+1F3D3`). | Un nom de tournoi ou de joueur importé avec un émoji empêche de produire le règlement et la feuille d'engagement. | Filtrer dans `propre()` : `font.hasGlyph(codePoint)` sinon `?` (ou police de repli). | Faible / Faible |
| S-18 | Faible | `ui/ecran/FormulairesSection.java:313-321`, `ui/infra/GeocodeurGeoplateforme.java:34-58`, `resources/.../carte/carte.html` (FONDS) | Géocodage **synchrone** sur le fil JavaFX (gel jusqu'à 5 s) ; corps de réponse non borné, analysé par `JsonParser` (S-09). Tuiles OpenStreetMap : la politique d'usage d'OSM exclut l'usage intensif par une application commerciale. | Salle sans réseau stable : interface figée à chaque enregistrement d'adresse. | Appel dans un `Task` en arrière-plan ; `BodyHandlers` borné (64 Ko) ; vérifier la politique OSM ou garder l'IGN seul. | Faible / Faible |
| S-19 | Faible | `ui/infra/HachageMotDePassePBKDF2.java:25, 54` | Le nombre d'itérations est lu dans l'empreinte sans plancher ni plafond ; 120 000 itérations est sous la recommandation OWASP actuelle pour PBKDF2-HMAC-SHA256 (600 000) ; pas de re-hachage à la connexion. | Empreinte à 1 itération acceptée ; à 2³¹-1 itérations, la connexion se bloque (déni de service). | Bornes [120 000 ; 2 000 000] ; paramètre porté à 600 000 ; re-hachage transparent après connexion réussie ; `MessageDigest.isEqual` pour la comparaison. | Faible / Moyenne |
| S-20 | Faible | `ui/ContexteApplication.java:604-610`, `persistence/DepotApplication.java:343-349`, `ui/ContexteApplication.java:280-295` | Nom de brouillon bien assaini (pas de traversée), mais longueur non bornée (> 255 caractères : échec de création). Les « récents » sont des chemins absolus arbitraires relus du dépôt applicatif, puis lus et réécrits sans contrôle. | Un nom de tournoi très long empêche la création ; un dépôt applicatif modifié fait lire/écrire des fichiers hors du dossier de données. | Tronquer le nom de fichier (80 caractères) ; n'accepter dans les récents que des fichiers `.ptt.json` réguliers, et prévenir avant d'écrire hors du dossier de données. | Faible / sans objet |
| S-21 | Info | `importation/ModeleImport.java:63-79`, `ui/ecran/EcranImportJoueurs.java:447-466` | Injection de formules (CSV/xlsx) : aucun export de données saisies aujourd'hui (seuls des modèles statiques sont écrits). | — | Pour tout futur export (listes d'inscrits, résultats) : préfixer d'une apostrophe les cellules commençant par `=`, `+`, `-`, `@`, tabulation ou retour chariot. | Info / Moyenne |
| S-22 | Info | dépôt entier, historique git | Secrets : aucune clé, aucun jeton, aucun certificat dans l'arbre ni l'historique ; `.gitignore` correct. Seuls « secrets » : les mots de passe de démo (S-01, S-02). | — | Garder cette hygiène ; quand SMTP ou API FFTT arriveront, identifiants hors du dépôt (variables d'environnement ou coffre du système). | Info / Info |
| S-23 | Info | `persistence/ServiceAutosave.java`, `persistence/ServiceFichierTournoi.java:248-259` | L'autosave n'est branché nulle part dans l'interface. Quand il le sera : instantané pris sur un autre fil pendant que le fil JavaFX modifie le modèle (incohérences, erreurs avalées silencieusement) ; fichiers `.autosave.json` = mêmes données personnelles, à inclure dans la purge. | — | Prendre l'instantané sur le fil JavaFX (copie sérialisée) puis écrire en arrière-plan ; signaler un autosave raté. | Info / sans objet |

---

## 3. Détail des constats

### S-01 — Compte « ultra admin » au mot de passe écrit dans le code (Critique)

- `DonneesDemo.java:35-37` : `EMAIL_ULTRA_ADMIN = "admin@pingmanager.fr"`, `MDP_ULTRA_ADMIN = "<masqué>"`, `ULTRA_ADMINS = Set.of(EMAIL_ULTRA_ADMIN)`.
- `ContexteApplication.java:170-178` crée ce compte au démarrage s'il n'existe pas ; `:298-300` accorde la supervision sur la seule base de l'email.
- `AppPingManager.java:34` imprime `memoDemo()` (qui contient les deux mots de passe) à chaque lancement.
- Le mot de passe figure aussi dans `docs/passation/REGLES_CREATION_TOURNOI.md:167`, `PASSATION_PHASE_INSCRIPTIONS.md:10`, `PASSATION_ONGLET_TOURNOIS.md:52`.
- Effet : sur **tout** poste équipé, menu Supervision → « Tous les tournois » → ouvrir et modifier n'importe quel tournoi (« Vos modifications sont enregistrées dans le tournoi du club »).
- Si le compte est supprimé du JSON, n'importe qui peut le recréer avec cet email et retrouve la supervision (privilège attaché à l'email).
- Démonstration : `FaillesSecuriteContexteTest.ultra_admin_ouvert_avec_le_mot_de_passe_ecrit_dans_le_code_faille_S01`.

### S-02 — Données personnelles réelles et compte du développeur sur chaque poste (Élevée)

- `DonneesDemo.java:30-31, 93-107` : email Gmail, licence 9141442, **date de naissance** (20/01/2003), nationalité de Quentin ; `:40-42` : email de Serge BOULIER.
- `ContexteApplication.java:140-153` : compte l’email personnel de Quentin / (mot de passe en clair, masqué dans ce rapport) créé et rendu administrateur de Brunoy.
- Présent aussi dans les tests et `outils-dev/harness/.../Capture.java:59`.
- Démonstration : `FaillesSecuriteContexteTest.compte_personnel_du_developpeur_cree_sur_chaque_poste_faille_S02`.
- Remarque : modifier `DonneesDemo`/`TournoiDemoBrunoy` est une décision de Quentin (CLAUDE.md) ; voir « Questions ouvertes ».

### S-03 — Accès pré-enregistrés par email et création de compte sans vérification (Élevée)

- `ContexteApplication.java:131-137` enregistre `AccesClub(CLUB_BRUNOY, "<email du référent de Brunoy>", …, ADMIN_PRINCIPAL)` sans compte.
- `creerCompteEtConnecter` (`:388-398`) ne fait qu'un contrôle d'unicité ; `accesDe(email)` rattache ensuite tous les accès portant cet email.
- Démonstration : `FaillesSecuriteContexteTest.compte_cree_avec_l_email_du_referent_herite_de_l_acces_principal_faille_S03` (compte créé avec le mot de passe « x », accès principal obtenu).

### S-04 — Code de rattachement non lié au club (Moyenne, Critique au web)

- `ServiceLiaisonClub.confirmer(compte, numeroClub, demande, code)` relit le club depuis le paramètre (`:118`) ; `DemandeVerification` ne porte que email, empreinte et expiration.
- Démonstration : `FaillesSecuriteLiaisonsTest.code_recu_pour_son_club_ouvre_l_acces_a_un_autre_club_faille_S04`.
- Aujourd'hui, `PageCompte.validerCode` repasse `numeroClubEnCours` : pas d'exploitation par l'écran, mais le service du cœur est faux par construction.

### S-05 — Liaison de licence sans preuve (Élevée)

- `ServiceLiaisonLicence.lier` : recherche puis `compte.lierLicence(...)`, sans unicité ni preuve.
- Conséquences : fiche personnelle d'un tiers visible (`PageCompte`), inscription en ligne en son nom (`ContexteApplication.inscrireEnLigne`), apparition dans « Mes tournois ».
- Démonstration : `FaillesSecuriteLiaisonsTest.n_importe_quel_compte_se_lie_a_la_licence_d_un_tiers_faille_S05`.

### S-06 — Règlement déposé : chemin libre recopié tel quel (Moyenne)

- `EcranComplementaire.java:157-159` stocke `f.getAbsolutePath()` ; `TournoiMapper`/`ConfigMapper` le relisent sans contrôle ; `FicheTournoi.java:376-379` fait `Files.copy(Path.of(emplacement), cible)` depuis l'onglet public.
- Pas de vérification que le fichier est un PDF : un fichier piégé est diffusé tel quel aux joueurs.
- Démonstration : `FaillesSecuritePersistanceTest.reglement_depose_pointant_vers_le_depot_applicatif_relu_sans_controle_faille_S06`.

### S-07 — Import de tableurs : déni de service par la taille (Moyenne)

- Démonstrations : `FaillesSecuriteLecteurTableurTest.numero_de_ligne_geant_contourne_la_limite_de_lignes_faille_S07` (fichier < 1 Ko → 60 000 lignes) et `…numero_de_ligne_non_numerique_echappe_au_message_d_import_faille_S07`.
- Contrôle en place vérifié : `…doctype_et_entite_externe_refuses_controle_en_place` (XXE refusé). Ce test-là reste tel quel.
- CSV : limité à 20 000 lignes, mais ni la largeur ni la longueur des cellules ne sont bornées ; le fichier est lu en entier.
- Les noms d'entrées zip ne servent jamais de chemin : pas de « zip slip ».

### S-08 — Force brute et énumération (Moyenne)

- Démonstration : `FaillesSecuriteLiaisonsTest.code_de_rattachement_sans_limite_d_essais_faille_S08` (200 essais faux, puis le bon code reste accepté).
- Le coût PBKDF2 (≈ 50-100 ms par essai) freine sans empêcher : en 30 minutes, environ 2 à 4 % de l'espace des codes est testable par un seul client, sans limite de clients au web.

### S-09 — `JsonParser` sans limite de profondeur (Moyenne)

- Démonstration : `FaillesSecuriteJsonParserTest.imbrication_profonde_provoque_un_debordement_de_pile_faille_S09` (pile fixée à 256 Ko pour un résultat reproductible).
- Autres remarques : nombres hors bornes acceptés (`1e999` → infini puis `(int)` → `Integer.MAX_VALUE`), clés dupliquées (la dernière gagne), `\u+041` accepté. Sans gravité propre, à durcir avec la profondeur.

### S-10 — WebView : pont Java exposé à toute page (Moyenne)

- `CarteTournois.java:68-72` : `if (etat == SUCCEEDED) fenetre.setMember("java", pont)` sans vérifier la page.
- `leaflet.js` 1.9.4 ajoute par défaut le préfixe d'attribution `<a href="https://leafletjs.com">Leaflet</a>` ; aucun filtre de navigation n'est posé.
- Les données injectées sont correctement échappées (`js()` : `'`, `\`, saut de ligne, `<` ; `echapper()` : `& < > "`). Ce n'est pas une injection, c'est un défaut de confinement.

### S-11 — Dépendances (Moyenne)

- Versions lues dans `pom.xml` : JavaFX 21.0.5, PDFBox 2.0.29, JUnit 5.10.2, surefire 3.2.5, javafx-maven-plugin 0.0.8.
- Aucune base de vulnérabilités n'a été interrogée pendant l'audit (pas d'outil installé, aucun ajout de plugin sans accord) : les mentions de CVE ci-dessus reposent sur mes connaissances et doivent être confirmées par un outil de veille.

### S-12 à S-23

Voir le tableau : chaque ligne indique fichier, scénario et correctif. Démonstrations associées :
`FaillesSecuriteContexteTest.depot_applicatif_en_clair_avec_emails_et_empreintes_faille_S15`,
`RenduPdfCaracteresHorsPoliceTest` (S-17 **corrigée** le 8 novembre 2026 : caractère hors police imprimé « ? »),
`FaillesSecuriteHachageTest.empreinte_a_une_seule_iteration_acceptee_faille_S19`.

Vérification à l'exécution des droits (umask 022, Linux) :
`pingmanager-application.json` et `*.ptt.json` en `-rw-------` ; dossiers `pingmanager/` et `tournois/` en `drwxr-xr-x` ;
marqueurs `.demo-brunoy-v12` et `.nettoyage-…` en `-rw-r--r--` ; PDF exportés en 0644 (emplacement choisi par l'utilisateur).

---

## 4. Tests de démonstration ajoutés (14)

Ils **passent sur le code actuel parce qu'il est vulnérable**. Lors de la correction de chaque
faille, le test correspondant doit être **inversé** (il vérifiera alors le refus) et renommé sans le
suffixe `faille_Sxx`. Seul `doctype_et_entite_externe_refuses_controle_en_place` vérifie une
protection existante et reste tel quel.

| Fichier | Test | Faille |
|---------|------|--------|
| `src/test/java/com/pingmanager/ui/FaillesSecuriteContexteTest.java` | `ultra_admin_ouvert_avec_le_mot_de_passe_ecrit_dans_le_code_faille_S01` | S-01 |
| idem | `compte_personnel_du_developpeur_cree_sur_chaque_poste_faille_S02` | S-02 |
| idem | `compte_cree_avec_l_email_du_referent_herite_de_l_acces_principal_faille_S03` | S-03, S-12 |
| idem | `depot_applicatif_en_clair_avec_emails_et_empreintes_faille_S15` | S-15 |
| `src/test/java/com/pingmanager/core/FaillesSecuriteLiaisonsTest.java` | `code_recu_pour_son_club_ouvre_l_acces_a_un_autre_club_faille_S04` | S-04 |
| idem | `code_de_rattachement_sans_limite_d_essais_faille_S08` | S-08 |
| idem | `n_importe_quel_compte_se_lie_a_la_licence_d_un_tiers_faille_S05` | S-05 |
| `src/test/java/com/pingmanager/persistence/FaillesSecuritePersistanceTest.java` | `reglement_depose_pointant_vers_le_depot_applicatif_relu_sans_controle_faille_S06` | S-06 |
| `src/test/java/com/pingmanager/importation/FaillesSecuriteLecteurTableurTest.java` | `numero_de_ligne_geant_contourne_la_limite_de_lignes_faille_S07` | S-07 |
| idem | `numero_de_ligne_non_numerique_echappe_au_message_d_import_faille_S07` | S-07 |
| idem | `doctype_et_entite_externe_refuses_controle_en_place` | contrôle existant |
| `src/test/java/com/pingmanager/persistence/json/FaillesSecuriteJsonParserTest.java` | `imbrication_profonde_provoque_un_debordement_de_pile_faille_S09` | S-09 |
| `src/test/java/com/pingmanager/core/reglement/RenduPdfCaracteresHorsPoliceTest.java` (ex-`FaillesSecuriteRenduPdfTest`, inversé après correction) | `emoji_dans_le_titre_fait_echouer_le_pdf_faille_S17` | S-17 |
| `src/test/java/com/pingmanager/ui/infra/FaillesSecuriteHachageTest.java` | `empreinte_a_une_seule_iteration_acceptee_faille_S19` | S-19 |

Non démontrés par test (comportement d'interface, réseau ou mémoire) : S-10, S-11, S-13, S-14, S-16, S-18, S-20 à S-23, et la bombe zip de S-07 (une démonstration ferait tomber la JVM de test).

---

## 5. Plan de correction par lots

Ordre : gravité d'abord, puis effort. Chaque lot est livrable seul (branche + Pull Request), avec
`mvn -B test` à 0 échec. Les tests `faille_Sxx` concernés sont inversés dans le lot qui corrige la faille.

### Lot 1 — Urgent, avant toute diffusion ou vente (effort : 1 à 2 jours)

Objectif : plus aucun compte, mot de passe ou donnée réelle livrés dans le produit.

1. **S-01, S-02** : introduire un mode démo explicite (`-Dpingmanager.demo=true` ou case au premier lancement) ; hors démo, `amorcerProfilDemo()` ne crée **rien** ; supprimer `ULTRA_ADMINS` codé en dur au profit d'un rôle stocké (ou retirer la supervision du produit de bureau) ; remplacer les identités réelles par des fictives (`@exemple.fr`) dans le code, les tests, la doc et le harnais ; supprimer `System.out.println(memoDemo())`.
2. **S-03** : vérifier l'email à la création (réutiliser `GenerateurCode` + `EnvoiEmail`), compte inactif tant que non confirmé ; n'activer les accès pré-enregistrés qu'après vérification.
3. **S-12** : politique de mot de passe (≥ 10 caractères, liste noire courte) et format d'email, avec `ResultatCreationCompte` à motif précis.
4. **S-16** : ne plus imprimer le code ; `EmailConsole` réservée au mode démo (hors démo, liaison club indisponible tant qu'aucun envoi réel n'existe, message clair).

Tests à ajouter / inverser : inverser S01, S02, S03 ; `creation_de_compte_refusee_si_mot_de_passe_trop_court`, `acces_pre_enregistre_inactif_tant_que_l_email_n_est_pas_verifie`, `aucun_compte_cree_hors_mode_demo`, `code_de_verification_absent_de_la_sortie_standard`.

### Lot 2 — Identité et habilitations dans le cœur (effort : 2 à 3 jours)

1. **S-04** : `DemandeVerification` porte `numeroClub` ; `confirmer` refuse un club différent (rétrocompatibilité sans objet : la demande n'est pas persistée).
2. **S-08** : compteur d'essais (5) dans la demande, invalidation ensuite ; temporisation à la connexion ; hachage factice si email inconnu ; message de création neutre.
3. **S-05** : unicité licence ↔ compte (index dans `DepotApplication`) et preuve de possession (à arbitrer par Quentin : code à l'email FFTT ou validation par le club).
4. **S-13** : `ContexteApplication.exigerDroit(tournoi, action)` appelé par toute lecture/écriture de gestion ; suppression du repli par nom de club ; matrice des droits par `NiveauAcces` ; journal de supervision.
5. **S-19** : bornes d'itérations, 600 000 itérations, re-hachage transparent.

Tests : inverser S04, S05, S08, S19 ; `demande_refusee_apres_cinq_codes_faux`, `licence_deja_liee_refusee_pour_un_second_compte`, `enregistrement_refuse_sans_droit_sur_le_club`, `tournoi_sans_numero_d_affiliation_n_est_pas_attribue_par_homonymie`, `empreinte_sous_le_parametre_courant_re_hachee_a_la_connexion`.

### Lot 3 — Fichiers et parseurs (effort : 2 jours)

1. **S-09** : profondeur (64) et taille (50 Mo) maximales dans `JsonParser`, `JsonException` claire.
2. **S-07** : bornes de `LecteurTableur` (numéro de ligne, taille d'entrée et totale, ratio de compression, taille du fichier) ; toute `RuntimeException` convertie en `ImportException`.
3. **S-06** : copie du règlement déposé dans le dossier de données (contrôle `%PDF-`, taille, SHA-256), chemin relatif ; refus à la lecture d'un chemin hors du dossier ; garder la relecture des anciens fichiers (chemin absolu → proposé à la recopie), test `RoundTripTournoiTest` à compléter.
4. **S-17** : fait le 8 novembre 2026 (`RenduPdfReglement.imprimable`).
5. **S-14, S-20** : `force(true)`, nettoyage du temporaire, verrou d'instance, `.bak` du dépôt applicatif, dossiers en 0700, nom de fichier tronqué, récents filtrés.

Tests : inverser S06, S07 (×2), S09, S17 ; `bombe_zip_refusee_sans_tout_decompresser`, `depot_applicatif_illisible_repli_sur_la_copie_bak`, `seconde_instance_refusee_par_le_verrou`, `nom_de_tournoi_de_500_caracteres_cree_un_fichier`.

### Lot 4 — WebView, réseau, dépendances (effort : 1 jour, validation visuelle par Quentin)

1. **S-10** : pont injecté uniquement sur la page locale, navigation bloquée, liens ouverts dans le navigateur système, `Pont.ouvrir` limité aux identifiants affichés, CSP.
2. **S-11** : montée de JavaFX (dernière 21.0.x) et PDFBox (dernière 2.0.x) ; mise en place d'une veille (outil à valider par Quentin, nouvelle dépendance de build).
3. **S-18** : géocodage en arrière-plan, réponse bornée ; vérifier la politique d'usage OSM ou se limiter à l'IGN.

Tests : `CarteTournois.js` déjà couvert ; ajouter `pont_refuse_un_identifiant_inconnu` (méthode statique testable), `geocodeur_refuse_une_reponse_trop_volumineuse`.

### Lot 5 — RGPD et préparation du passage web (effort : à planifier)

1. **S-15** : registre de traitement, mention d'information, purge paramétrable des tournois clos et des archives, effacement d'un joueur, chiffrement au repos optionnel ; inclure les `.autosave.json` (S-23).
2. **S-21** : neutralisation des formules dans tout futur export tableur.
3. **S-23** : autosave sûr (instantané sur le fil JavaFX).
4. Préparation web : toutes les vérifications d'habilitation côté serveur (aucune confiance dans l'interface), sessions à durée limitée, limitation de débit, journal d'audit, HTTPS partout, en-têtes de sécurité, stockage des documents hors de toute racine servie.

Tests : `purge_supprime_les_tournois_clos_depuis_plus_de_n_mois`, `effacement_d_un_joueur_retire_ses_donnees_de_tous_les_tournois`, `export_neutralise_une_cellule_commencant_par_egal`.

---

## 6. Questions ouvertes pour Quentin

1. **Données de démo** (S-01, S-02) : CLAUDE.md interdit de toucher à `TournoiDemoBrunoy` sans demande. Faut-il un mode démo explicite, et peut-on remplacer les identités réelles (votre email, licence, date de naissance ; l'email de Serge BOULIER) par des fictives ? Le dépôt GitHub est-il public ? Si oui : réécriture de l'historique à décider.
2. **Supervision** (S-01, S-13) : la supervision « ultra admin » doit-elle exister dans le logiciel de bureau vendu, ou seulement dans la future version web (avec authentification forte) ?
3. **Preuve de licence** (S-05) : code envoyé à l'email connu de la FFTT, validation par le club, ou autre ?
4. **Envoi d'emails** (S-03, S-16) : quel prestataire SMTP (identifiants hors du dépôt) ?
5. **Outil de veille des dépendances** (S-11) : accord pour un plugin de build (OWASP dependency-check) ou Dependabot côté GitHub ?

---

## 7. Résultat des tests

Commande : `mvn -B test` (Maven Central accessible, `outils-dev/tests.sh` non nécessaire).

```
Tests run: 426, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

412 tests existants + 14 tests ajoutés par l'audit. Aucun test existant modifié, désactivé ou affaibli.
Aucun code de production modifié. L'interface JavaFX (S-10, S-13, S-18) n'a pas pu être vérifiée
visuellement en session cloud : la validation à l'écran revient à Quentin.
