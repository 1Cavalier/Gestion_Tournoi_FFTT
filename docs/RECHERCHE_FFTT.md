# Recherche : intégration de PingManager avec la FFTT

> Veille technique du **5 novembre 2026**. Objet : moyens d'interroger les
> données de la FFTT (licences, points mensuels, clubs) et d'échanger avec SPID V2
> pour l'homologation et la remontée des résultats de tournois individuels.
> Aucun espace authentifié n'a été consulté, aucun identifiant n'a été essayé.
> Chaque affirmation renvoie à une source numérotée `[n]` (section 6).

**Méthode et limite à connaître.** Depuis la session de travail, les domaines
`www2.fftt.com` et `www.pongiste.fr` ne sont pas joignables (refusés par le
réseau de la session). La lecture des pages a donc été faite à partir des
**extraits indexés par le moteur de recherche** (titres, passages cités), et non
par une lecture intégrale des PDF. Ce qui est noté « vérifié » ci-dessous repose
sur un extrait d'un document officiel FFTT. Il faudra relire en entier les deux
PDF clés [3] [4] avant toute décision. Pour cela, autoriser `www.fftt.com` et
`www2.fftt.com` dans les réglages réseau de la session, ou les ouvrir depuis un
navigateur.

---

## 1. Résumé en dix lignes

1. La FFTT publie une **API officielle** (héritière de « Smartping 2.0 ») : formulaire et CGU mis en ligne en janvier 2026 [1][3][4].
2. Pour y accéder, on envoie un **formulaire** à `interfaces.informatiques@fftt.email`. La FFTT juge le projet et donne un identifiant et une clé [1][2][4].
3. Les **CGU standard** visent un usage privé dans une application « libre et gratuite », sans usage commercial. **PingManager, logiciel vendu, n'y entre pas** : il faut une autorisation expresse en plus [3].
4. L'API couvre ce dont PingManager a besoin : joueur, licence (dont le certificat médical), club, listes de joueurs, points [4][5]. D'après un forum, les données ne doivent pas être stockées plus de **24 h** sans accord [7].
5. **Correction pour `ETAT_DU_PROJET.md` §4** : GIRPE sert aux **rencontres par équipes**. Les **tournois individuels** se gèrent et se remontent avec **SPIDD** (SPID déconnecté, logiciel du juge-arbitre) [13][17][19][20].
6. L'homologation se demande dans **SPID V2 « Mon Espace Club »** (règlement et feuille d'engagement en PDF) [15][16]. Le format des résultats est « donné lors de l'homologation », à rendre sous 8 jours (IX.107) [25].
7. Aucun **format d'échange SPIDD/SPID** pour les résultats n'est public. Le support indiqué est `spidv2@fftt.email` [13] (l'ancienne adresse `spidv2@fftt.org` apparaît encore dans une fiche de 2021 [15]).
8. Aucun **fichier public sous licence ouverte** n'a été trouvé pour les licenciés, les points ou les clubs. Seule piste pour les clubs : le numéro FFTT relevé par OpenStreetMap (ODbL) [34].
9. Les **concurrents** repérés ne se branchent pas à SPID (PlaniTournoi) ou ne disent pas comment ils le font (PingTournoi, Paak) [29][30][31].
10. **Recommandation** : commencer tout de suite par l'import de fichiers fournis par le club et la coexistence avec SPIDD (sans risque), et en parallèle **écrire à la FFTT** (courriel en section 5) pour obtenir un accord commercial sur l'API et le format de remontée. Le moissonnage du site est exclu.

---

## 2. État des lieux sourcé

Légende : **[V]** vérifié (source officielle FFTT ou texte réglementaire, datée) ·
**[S]** source secondaire (forum, dépôt tiers, ligue ou comité, presse) ·
**[?]** inconnu.

### 2.1 API officielles (« Smartping », API FFTT)

| # | Constat | Statut | Sources |
|---|---|---|---|
| A1 | La FFTT a une page « API » sur l'ancien site (www2) et sur le nouveau site (`www.fftt.com/api/`). | [V] | [1][2] |
| A2 | Procédure : lire la documentation technique, remplir le formulaire, s'engager à respecter les CGU, envoyer le tout à `interfaces.informatiques@fftt.email`. La FFTT juge la pertinence du projet et peut refuser un projet « qui ne respecte pas ses intérêts et ceux de ses licenciés ». Si elle accepte, le service informatique ouvre les API demandées et donne un identifiant et une clé personnels. | [V] | [1][2] |
| A3 | La FFTT ne fournit **aucun support à l'intégration** en dehors des documentations techniques publiées. | [V] | [4] |
| A4 | La FFTT contrôle régulièrement l'usage des API et peut **suspendre l'accès immédiatement** si les CGU ne sont pas respectées. | [V] | [1][3] |
| A5 | **CGU (PDF de janvier 2026)** : le partenaire peut offrir un accès « limité et non exclusif » aux données fédérales à des utilisateurs finaux, « pour leur seul usage privé, au sein d'un site internet ou d'une application **libre et gratuite** ». C'est un « droit d'utilisation **non commercial** ». L'**usage professionnel** y est défini comme « tout usage autre que l'usage privé ». Au-delà, il faut une « autorisation **expresse et complémentaire** ». | [V] (extraits) | [3] |
| A6 | CGU : utiliser l'API entraîne une collecte et un stockage de données. Le texte relève du droit français. | [V] (extraits) | [3] |
| A7 | **Stockage** : une version antérieure des CGU (clause 5.2), citée sur un forum, interdit de stocker les données fédérales sans autorisation expresse, mais permet un **cache de 24 h**. La clause du PDF de 2026 (numérotée 4.2 ?) n'a pas pu être lue en entier. | [S] | [7] |
| A8 | Le formulaire de 2026 renvoie aux « Spécifications techniques API Smartping v2.0 » et liste des services au choix, dont `xml_joueur` (détail d'un joueur), `xml_licence` (détail d'un licencié), `xml_club_detail` (détail d'un club), `xml_liste_joueur` (base classement) et `xml_liste_joueur_o` (base SPID). | [V] | [4] |
| A9 | La spécification Smartping 2.0 (édition 2015, copie en ligne chez un tiers) prévoit un champ **certificat médical** (valeurs : « Certificat », « Ni entraînement ni compétition », « Quadruple ») et le service `xml_licence_b` (licence et classement, filtrable par club). | [S] (copie tierce d'un document officiel) | [5] |
| A10 | Les **points mensuels** et la progression se déduisent des champs du joueur (`point`, `apoint`, `valinit`) selon du code tiers. | [S] | [12][6] |
| A11 | L'adresse de l'API est passée sur `https://apiv2.fftt.com/mobile/pxml/…`. Selon un forum, les requêtes ne sont acceptées que depuis le **domaine déclaré** dans le formulaire. | [S] | [6][8] |
| A12 | Les requêtes sont authentifiées par un identifiant d'application, une clé et un horodatage chiffré (HMAC-SHA1 / MD5), avec un numéro de série par utilisateur. | [S] | [9][7] |
| A13 | Plusieurs clients libres existent (PHP `alamirault/fftt-api` en version 0.1.4 du 22 juin 2026, TypeScript, C#, Rust). Ils montrent que l'API fonctionne encore en 2026. Tous demandent d'obtenir ses identifiants auprès de la FFTT. | [S] | [9][10][11] |
| A14 | Usage **commercial** : conditions (prix, convention, quotas) ? | [?] | — |
| A15 | **Délai** de réponse au formulaire, **limites de débit**, disponibilité garantie ? | [?] | — |
| A16 | Une application **de bureau** distribuée chez plusieurs clients est-elle compatible avec la restriction par domaine et avec une clé unique ? | [?] | — |
| A17 | Le service donne-t-il la **situation du certificat médical** à jour (II.606.1) et les **points de début de phase** distincts des points mensuels ? | [?] (probable d'après [5], à confirmer) | — |

### 2.2 Formats d'échange avec SPID, SPIDD et GIRPE

| # | Constat | Statut | Sources |
|---|---|---|---|
| F1 | **Homologation** : la demande se fait dans SPID V2, « Mon Espace Club » → « Sportifs » → « Homolog. de tournois » → « Nouvelle demande ». Le règlement et la feuille d'engagement doivent être en **PDF**. Le statut passe ensuite d'« en attente » à homologué (ou refusé), avec un numéro d'homologation. | [V] (fiche FFTT de 2021, rediffusée en décembre 2025) | [15][16] |
| F2 | Pour la commission sportive fédérale, la demande comprend le règlement, le « **document informatique détaillant le format du tournoi** », les droits et le juge-arbitre désigné. Elle part au moins **deux mois** avant le tournoi et passe par le comité puis la ligue (IX.104). | [V] | [25] |
| F3 | **Résultats** : « dans les huit jours », sous un « **format informatique qui sera donné lors de l'homologation** » (IX.107). | [V] | [25] |
| F4 | **SPIDD** (« SPID déconnecté ») est le logiciel des juges-arbitres pour les **épreuves individuelles**. Avant l'épreuve, il récupère depuis SPID les licenciés, épreuves, tableaux et inscrits. Pendant l'épreuve, il fonctionne **sans internet** (plan d'occupation des tables, tableaux, feuilles de parties). Après, il **remonte les résultats** à la reconnexion. | [V] | [17][18][19] |
| F5 | SPIDD V2 en est à la version 2.3.6.3 (Windows, .NET 4.8). La page est mise à jour au 5 octobre 2026 et indique « Propriété de la FFTT ». Support : `spidv2@fftt.email`. Des tutoriels existent, dont « gestion d'une épreuve individuelle ». | [V] | [13][14] |
| F6 | Des règlements de ligues et comités imposent ou décrivent la saisie et la remontée par SPIDD pour les tournois homologués. Le juge-arbitre doit « maîtriser SPIDD ou être assisté ». | [S] (textes régionaux) | [20][21][22] |
| F7 | **GIRPE** = gestion informatisée des rencontres **par équipes**. Il produit un fichier `.rpe` et remonte vers SPID (version 7.4 et plus). Il ne concerne pas les tournois individuels. | [V]/[S] | [23][24] |
| F8 | SPID V2 impose une **double authentification** (code par courriel). | [S] | [28] |
| F9 | Format d'**import dans SPIDD** ou d'**export SPIDD → SPID**, et ce que contient le « document informatique » de l'homologation. | [?] Non public | — |
| F10 | Un logiciel tiers peut-il **déposer lui-même** des résultats dans SPID, ou doit-il passer par SPIDD (import d'un fichier) ? | [?] | — |

### 2.3 Fichiers publics (clubs, points, classements) et leur licence

| # | Constat | Statut | Sources |
|---|---|---|---|
| P1 | Aucun jeu de données FFTT (licenciés, points, clubs) n'a été trouvé sur data.gouv.fr ni sous Licence Ouverte. | [S] (recherche négative) | — |
| P2 | Le classement nominatif est consultable par les licenciés (site SPID, application « FFTT ») pour vérifier une licence (II.606.2). Ce n'est pas un fichier réutilisable. | [V] | [26][40] |
| P3 | Les règlements généraux 2026-2027 précisent que les données SPID sur les associations et les licenciés « ne peuvent être utilisées qu'à des fins administratives et sportives par la FFTT et les organes déconcentrés », dans le respect du RGPD. | [V] | [26] |
| P4 | OpenStreetMap a une clé `ref:FR:FFTT` (numéro de club FFTT). Les données OSM sont sous **ODbL** (attribution, partage à l'identique d'une base dérivée). La couverture est partielle. | [S] | [34] |
| P5 | Liste officielle des clubs (numéro, nom, adresse de la salle, courriel officiel) téléchargeable sous licence connue ? | [?] (accessible par l'API `xml_club_detail` [4], sous CGU) | — |

### 2.4 Ce que font les logiciels du marché

| # | Logiciel | Ce qui est dit de l'intégration FFTT | Statut | Sources |
|---|---|---|---|---|
| L1 | **SPIDD** (FFTT, gratuit pour les JA) | Seul outil documenté qui récupère les données de SPID et y remonte les résultats de tournois individuels. | [V] | [13][17] |
| L2 | **PlaniTournoi** | Vise aussi les tournois homologués, mais n'est « pas encore intégré au système FFTT SPID ». | [S] | [29] |
| L3 | **PingTournoi** (ping-tournoi.fr, gratuit) | Se dit « connecté à la FFTT » (licenciés et classement officiel). Ne dit pas s'il exporte vers SPID. Être gratuit le place a priori dans le cadre des CGU standard [3]. | [S] | [30] |
| L4 | **Paak** (gestion de club) | Annonce « licenciés FFTT ». Méthode non documentée. | [S] | [31] |
| L5 | Bibliothèques et sites libres (pongiste.fr, extensions, clients API) | Utilisent l'API officielle avec identifiants FFTT. | [S] | [9][10][11][41] |
| L6 | Hors France : **TTT-Organizer** (Swiss Table Tennis) | Logiciel de tournoi mis à disposition par la fédération suisse **sous contrat d'utilisation**. Exemple d'un partenariat éditeur–fédération. | [S] | [33] |
| L7 | **Tennis de Table Tournament Manager** (Java, GPLv2) | Aucune intégration FFTT. | [S] | [32] |
| L8 | Un concurrent payant a-t-il une **convention commerciale** avec la FFTT ? | — | [?] | — |

### 2.5 Contraintes légales

| # | Constat | Statut | Sources |
|---|---|---|---|
| J1 | **RGPD, cadre des licences** : la licence est encadrée par le code du sport (art. L.131-6) et peut faire apparaître le niveau (classement) du licencié. Toute réutilisation pour une autre finalité doit être compatible avec la finalité initiale (art. 6.4 RGPD). | [V] (CNIL) | [35][36] |
| J2 | Les données SPID sont réservées aux fins administratives et sportives de la FFTT et de ses organes (préambule des règlements administratifs). | [V] | [26] |
| J3 | Accès API : non commercial par défaut. Un usage professionnel ou payant demande une autorisation expresse [A5]. Accès suspendable [A4]. | [V] | [3][1] |
| J4 | **Moissonnage** de fftt.com, SPID ou de l'application : **exclu**. Il serait contraire à [3] et [26], au droit du producteur de base de données (CPI, art. L.342-1 : interdiction d'extraire une partie substantielle) et au contrôle d'accès de SPID V2 (double authentification [28]). | [V] (textes) | [3][26][38] |
| J5 | La **situation vis-à-vis du certificat médical** est une donnée liée à la santé. Traitement minimal recommandé : afficher « en règle / pas en règle », ne pas conserver plus que nécessaire. Avis du DPO de la FFTT ou d'un juriste à prévoir. | [?] (analyse à faire) | [5] |
| J6 | Selon la presse spécialisée, la CNIL a lancé en 2026 des contrôles sur une trentaine de fédérations après des fuites de données. Cela incite la FFTT à la prudence avec les tiers. | [S] | [37] |
| J7 | Rôles RGPD : le club organisateur est **responsable de traitement** des inscriptions. Quentin est **éditeur**. Il devient **sous-traitant** (contrat art. 28 obligatoire) dès qu'un serveur à lui traite des données (relais API, version web). | [?] (analyse juridique à confirmer) | [35] |
| J8 | Nom et adresse du **DPO** de la FFTT : non trouvés. | [?] | — |

---

## 3. Options classées

Échelles : faisabilité (haute, moyenne, basse), coût pour Quentin, délai avant
mise en service, risque juridique.

| Rang | Option | Faisabilité | Coût | Délai | Risque juridique | Avis |
|---|---|---|---|---|---|---|
| 1 | **Import de fichiers fournis par le club** (classeur déjà en place, export de sa liste d'inscrits ou de ses joueurs) + **contrôle visuel de la licence** par le JA (II.606.2) | Haute (déjà en partie fait) | Nul | Immédiat | Faible : le club traite ses propres inscrits | **À faire tout de suite** |
| 2 | **Coexistence avec SPIDD** : PingManager gère la salle et produit des feuilles et une liste de résultats lisibles, le JA ressaisit dans SPIDD pour la remontée | Haute | Faible | Court | Faible | **Solution par défaut** tant que le format n'est pas connu (double saisie) |
| 3 | **API FFTT avec autorisation commerciale écrite** (convention) : vérification de licence, points, clubs | Moyenne : dépend de la FFTT [A2][A5] | Inconnu (redevance ?) + serveur relais probable [A11] | Inconnu (semaines à mois) | Faible **si** la convention couvre le stockage et la vente | **Cible**, demande à lancer maintenant |
| 4 | **Export au format fédéral** (fichier que SPIDD importe, ou dépôt direct dans SPID) | Basse tant que le format n'est pas fourni [F9][F10] | Moyen (développement + tests avec un JA) | Après réponse de la FFTT | Faible avec l'accord de la FFTT | Demande à lancer en même temps que l'option 3 |
| 5 | **Clé API du club** : chaque club demande sa propre clé et la saisit dans PingManager | Incertaine (le produit reste vendu, restriction par domaine) | Faible | Inconnu | **Moyen** : risque de contourner les CGU | Seulement si la FFTT le valide par écrit |
| 6 | **Clubs depuis OpenStreetMap** (`ref:FR:FFTT`, ODbL) | Moyenne (couverture partielle, sans courriel officiel) | Faible | Court | Faible si attribution et partage à l'identique respectés | Complément possible pour la carte, pas pour l'inscription |
| 7 | **API FFTT sous CGU standard** dans un produit vendu | — | — | — | **Élevé** (contraire à [3]) | **Exclu** |
| 8 | **Moissonnage** de fftt.com, SPID V2 ou de l'application | — | — | — | **Élevé** ([3][26][38]) | **Exclu** |

**Recommandation** : mettre en place les options 1 et 2 dès maintenant (elles
ne dépendent de personne) et envoyer **cette semaine** la demande qui couvre les
options 3 et 4 (section 5). Ne pas brancher l'API tant que l'accord écrit
n'est pas reçu.

---

## 4. Architecture proposée (sans code d'intégration)

Principe : le cœur ne connaît que des **ports**. Les adaptateurs FFTT vivent
**hors du cœur**, dans un nouveau paquetage, par exemple `com.pingmanager.federation`,
au même niveau que `importation` (dépendances : `ui` → `federation` → `core`).

**Ports déjà en place** (lecture seule, la fédération reste la source de vérité) :
- `core.RegistreLicences` : `trouverParLicence`, `existe`, `rechercher`. Il
  renvoie `model.FicheLicence`, qui porte déjà les points de début de phase et
  les points mensuels (IV.201.3), la catégorie, le club et le statut.
  Implémentation actuelle : `ui.infra.RegistreLicencesFFTT` (en mémoire, démo).
- `core.RegistreClubs` : `trouverParNumero`, `existe`.

**À prévoir** (noms indicatifs, à valider avec Quentin) :
1. `FicheLicence` : ajouter un champ **situation du certificat médical**
   (énumération « en règle / sans pratique / inconnue », II.606.1) et la
   **date de fraîcheur** de la donnée. Ainsi, l'écran J-1 peut afficher
   « vérifié auprès de la FFTT le … ».
2. Port **`ExportResultatsFederaux`** (cœur) : à partir d'un tournoi terminé,
   produire un modèle neutre (tableaux, parties, licences, scores, forfaits, n°
   d'homologation). Implémentations hors du cœur : `ExportResultatsTableur`
   (immédiat, pour l'option 2), puis `ExportResultatsSpidd` quand le format sera
   connu.
3. Implémentations de `RegistreLicences` / `RegistreClubs` hors du cœur :
   - `RegistreLicencesImporte` : alimenté par les fichiers du club (option 1) ;
   - `RegistreLicencesApiFftt` : seulement après l'accord écrit. Il passe par
     `java.net.http` et le XML du JDK, donc **sans nouvelle dépendance Maven**.
     Son **cache est limité à la durée autorisée** (24 h d'après [7], à confirmer).
     Les identifiants **ne sont jamais embarqués** dans le logiciel distribué.
     Si la FFTT impose un domaine déclaré [A11], un **relais serveur** de Quentin
     porte la clé, ce qui fait de Quentin un sous-traitant RGPD (J7).
4. **Instantané d'inscription** : les points enregistrés dans le tournoi
   (sauvegarde JSON) sont une copie datée faite **au titre du tournoi**. Il faut
   demander à la FFTT si cette conservation est permise au-delà de 24 h (voir
   courriel). Sinon, ne garder que le numéro de licence et relire les points le
   jour J.
5. Ordre de repli à l'écran : API (si autorisée) → fichier importé → saisie
   manuelle contrôlée par le JA sur présentation de l'attestation.

---

## 5. Démarches à faire par Quentin

1. **Relire en entier** les CGU [3] et le formulaire [4] depuis un navigateur
   (fftt.com), et noter le numéro et le texte exact de la clause sur le stockage.
2. **Remplir le formulaire API** [4] : cocher les services `xml_licence`
   (ou `xml_licence_b`), `xml_joueur`, `xml_liste_joueur`, `xml_club_detail`,
   et décrire honnêtement le caractère **commercial** du projet.
3. **Envoyer le courriel ci-dessous**, formulaire en pièce jointe, à
   `interfaces.informatiques@fftt.email`, avec `spidv2@fftt.email` en copie
   (support SPID V2 / SPIDD indiqué sur la page officielle [13]). En cas d'échec,
   renvoyer à `spidv2@fftt.org`, l'adresse donnée dans `ETAT_DU_PROJET.md` et
   dans la fiche de 2021 [15].
4. **Passer par le terrain** : demander à la ligue ou au comité de Brunoy et au
   juge-arbitre du National B de Brunoy (10-11 avril 2027) le « format
   informatique » reçu lors d'une homologation (IX.107) et une démonstration de
   SPIDD.
5. **Demander l'avis d'un juriste ou d'un DPO** sur les rôles RGPD (J7) et sur
   la donnée « certificat médical » (J5), avant toute version web ou tout relais.
6. **Relancer après 3 semaines** sans réponse. Pendant ce temps, livrer les
   options 1 et 2.
7. Mettre à jour `ETAT_DU_PROJET.md` §4 : remplacer « GIRPE » par « SPIDD » pour
   les tournois individuels (F4, F7).

### Brouillon du courriel (prêt à envoyer)

> **À :** interfaces.informatiques@fftt.email
> **Cc :** spidv2@fftt.email
> **Objet :** Demande d'accès aux API fédérales (usage commercial) et format de remontée des résultats de tournois homologués – logiciel PingManager
> **Pièce jointe :** formulaire de demande d'accès API complété et signé
>
> Madame, Monsieur,
>
> Je développe **PingManager**, un logiciel de gestion de **tournois individuels
> homologués** (inscriptions, tableaux, poules, ordonnancement des tables,
> saisie des résultats en salle). Il est destiné aux clubs organisateurs et
> sera **commercialisé** : d'abord une application de bureau, puis une version
> web. PingManager ne remplace pas SPID V2 ni SPIDD. Il veut faciliter le travail
> des organisateurs et des juges-arbitres dans le respect des règlements
> fédéraux (titre IX des règlements sportifs).
>
> Je vous adresse ci-joint le formulaire de demande d'accès aux API. Comme le
> projet est commercial, je comprends qu'il sort du cadre des conditions
> générales d'utilisation standard (usage privé, application libre et
> gratuite). Je sollicite donc l'**autorisation expresse et complémentaire**
> prévue par ces conditions. Pourriez-vous m'indiquer :
>
> 1. si la FFTT accepte d'accorder un accès pour un logiciel payant, et à
>    quelles conditions (convention, redevance, engagements, durée) ;
> 2. les services auxquels je pourrais accéder, pour ces seuls besoins :
>    vérifier une licence le jour du tournoi (nom, club, catégorie, situation
>    vis-à-vis de la certification médicale), lire les points (début de phase et
>    mensuels) et consulter le référentiel des clubs ;
> 3. les règles de conservation : un organisateur peut-il garder, dans le
>    dossier de son tournoi, les points d'un inscrit relevés à la date de
>    l'inscription, au-delà de la durée de mise en cache autorisée ?
> 4. les contraintes techniques : limites de débit, restriction par domaine,
>    et façon de procéder pour un logiciel installé chez plusieurs clubs
>    (identifiant unique de l'éditeur avec un serveur relais, ou identifiant
>    propre à chaque club) ;
> 5. les éventuelles exigences de sécurité et de protection des données
>    personnelles (RGPD), ainsi que les coordonnées de votre délégué à la
>    protection des données.
>
> Par ailleurs, l'article IX.107 des règlements sportifs prévoit que les
> résultats parviennent à la FFTT « sous un format informatique qui sera donné
> lors de l'homologation ». Pour que PingManager produise un fichier conforme
> et évite toute double saisie, pourriez-vous me communiquer, ou me dire à qui
> m'adresser pour obtenir :
>
> 6. la **documentation du format de remontée des résultats** des tournois
>    individuels homologués (fichier importable dans SPIDD ou déposable dans
>    SPID V2) ;
> 7. le contenu attendu du « document informatique détaillant le format du
>    tournoi » joint à la demande d'homologation (article IX.104) ;
> 8. la procédure éventuelle de **validation ou de reconnaissance** d'un
>    logiciel tiers par la FFTT (tests, fichier d'essai, environnement de
>    recette).
>
> Je reste disponible pour vous présenter le logiciel lors d'un échange ou
> d'une démonstration. Je m'engage à n'accéder à aucun espace authentifié ni
> à aucune donnée fédérale sans votre accord écrit.
>
> Je vous remercie par avance pour votre réponse et vous prie d'agréer,
> Madame, Monsieur, l'expression de mes salutations distinguées.
>
> [à compléter : Prénom NOM]
> [à compléter : société ou statut (auto-entrepreneur, SIRET)]
> [à compléter : adresse postale]
> [à compléter : téléphone]
> [à compléter : adresse électronique]
> [à compléter : club et numéro de licence FFTT, le cas échéant]
> [à compléter : site web ou page de présentation de PingManager]

---

## 6. Sources

Toutes consultées le **5 novembre 2026** (à travers les extraits du moteur de
recherche, voir l'avertissement en tête). La date indiquée est celle de la
publication ou de la mise à jour quand elle est connue.

1. FFTT, page « API » (nouveau site) — https://www.fftt.com/api/ — date non indiquée.
2. FFTT, page « API » (ancien site, injoignable depuis la session) — https://www2.fftt.com/site/mediatheque/autres-medias/api — date non indiquée.
3. FFTT, « Conditions générales d'utilisation des données fédérales » (PDF) — https://www.fftt.com/wp-content/uploads/2026/01/api-conditions-generales-d-utilisations-1485.pdf — déposé en janvier 2026.
4. FFTT, formulaire de demande d'accès API (PDF) — https://www.fftt.com/wp-content/uploads/2026/01/formulaire-api-1484.pdf — déposé en janvier 2026.
5. FFTT, « Spécifications techniques de l'API Smartping 2.0 » (2015), copie chez un tiers — https://docplayer.fr/74478678-Specifications-techniques-de-l-api-smartping-2-0.html — secondaire.
6. Forum tennis-de-table.com, « API Smartping 2.0 » — https://forum.tennis-de-table.com/t/api-smartping-2-0/1998?page=3 et https://forum.tennis-de-table.com/t/api-smartping-2-0/1998/72 — messages de 2021 à 2024 environ.
7. Forum tennis-de-table.com, « Api fftt » — https://forum.tennis-de-table.com/t/api-fftt/6592 — date non relevée.
8. Forum tennis2table, « API FFTT » — https://tennis2table.com/forum/general/api-fftt/ — date non relevée.
9. GitHub, omichalo/FFTT-API-typescript — https://github.com/omichalo/FFTT-API-typescript — consulté le 5 novembre 2026.
10. Packagist, alamirault/fftt-api (version 0.1.4 du 22 juin 2026) — https://packagist.org/packages/alamirault/fftt-api
11. GitHub, FlGoldschmitt/FFTT.Api.Client (README) — https://github.com/FlGoldschmitt/FFTT.Api.Client/blob/main/README.md
12. Pastebin, « Webservice SmartPing (v2.0) » — https://pastebin.com/FF1GHdMz — date inconnue.
13. FFTT, page « SPIDD V2 » (version 2.3.6.3, mise à jour du 5 octobre 2026, support `spidv2@fftt.email`) — https://www.fftt.com/deploiements/spidd-v2/
14. FFTT, « Installation et prise en main de SPIDD v2 » (version 2.3.2.0) — https://www.fftt.com/deploiements/spidd-v2/Installation%20et%20prise%20en%20main%20de%20SPIDD%20v2.3.2.0.pdf
15. FFTT, « SPID V2 – Mon Espace Club – Demande d'homologation de tournois » (2021, contact `spidv2@fftt.org`) — https://www.fftt.com/doc/administratif/documents/SPIDV2_Mon_Espace_Club_Demande_Homologation_Tournois.pdf
16. Même fiche, version 2 rediffusée par le CD95 (décembre 2025) — https://www.cd95tt.com/wp-content/uploads/2025/12/SPID-V2-Mon-espace-club-demande-homologation-tournois-v2.pdf
17. FFTT, ancienne page « SPID Déconnecté » — http://webv1.fftt.com/administratif/telechargement/spidd/spidd.htm — date inconnue (ancienne).
18. FFTT, Guide des formations 2014-2015, « SPID déconnecté » — http://www.fftt.com/ttformation/SPID_deconnecte.pdf
19. FFTT, page « Arbitrage » — https://www.fftt.com/arbitrage/ — date non indiquée.
20. Ligue des Pays de la Loire, réglementation des tournois 2023-2024 — https://tennisdetablepaysdelaloire.org/wp-content/uploads/2023/08/reglementation-tournois-version-2023-2024.pdf
21. Ligue des Hauts-de-France, règlement des tournois (2021) — https://liguehdftt.fr/wp-content/uploads/2021/05/Reglement-tournois-HAUTS-DE-FRANCE-_Valide-JPD_-statuts-et-reglements-1.pdf
22. CD93, règlement du tournoi national B 2025-2026 — https://tennis2table.com/wp-content/uploads/t2t-reglements/2-reglement-du-tournoi-national-b-du-cd93tt-2025-2026-d8842701a4.pdf
23. GIRPE, questions-réponses — http://www.girpe.com/questionsReponses.php?p=484 ; forum tennis2table « Mode d'emploi de GIRPE » — https://tennis2table.com/forum/reglement/mode-d-emploi-de-girpe-logiciel-fftt/paged/3/
24. FFTT, « GIRPE, mode d'emploi » — https://www.fftt.com/site/articles/girpe-mode-d-emploi
25. FFTT, Règlements sportifs 2026-2027 (juillet 2026), art. IX.102 à IX.107 — copie locale `docs/reglements/reglements_sportifs_2026_2027.txt`.
26. FFTT, Règlements généraux 2026-2027 (juillet 2026), préambule « Utilisation des données » et art. II.606 — copie locale `docs/reglements/reglements_generaux_2026_2027.txt`.
27. FFTT, Circulaire administrative et financière 2026/2027 (12 juin 2026) — https://www.fftt.com/doc/administratif/documents/2026-06-12-FFTT-cadm2026-2027-V2.pdf
28. Ligue des Hauts-de-France, « Mise en place de la double authentification SPID V2 » — https://liguehdftt.fr/mise-en-place-de-la-double-authentification-spid-v2/
29. tennis2table, « PlaniTournoi » — https://tennis2table.com/clubs/tournois/plani-tournoi.html
30. PingTournoi — https://ping-tournoi.fr/
31. Paak, « logiciel tennis de table » — https://paak.club/logiciel-tennis-de-table/
32. SourceForge, Table Tennis Tournament Manager — https://www.sourceforge.net/projects/tttm
33. Swiss Table Tennis, contrat d'utilisation TTT-Organizer (2024) — https://swisstabletennis.ch/wp-content/uploads/2024/08/Contrat-dutilisation-1.pdf
34. Wiki OpenStreetMap, « Key:ref:FR:FFTT » — https://wiki.openstreetmap.org/wiki/Key:ref:FR:FFTT
35. CNIL, « Licences sportives : les principales règles à respecter » — https://cnil.fr/fr/node/165843
36. CNIL, sport amateur : réutilisation des informations personnelles — https://www.cnil.fr/fr/sport-amateur-hors-contrat/tester-votre-conformite-au-rgpd/reutilisation
37. Leto, « CNIL 2026 : 30 fédérations sportives sous surveillance » (presse spécialisée) — https://www.leto.legal/news/cnil-controles-federations-sportives-rgpd-2026
38. Légifrance, Code de la propriété intellectuelle, droit du producteur de base de données (art. L.342-1 et suivants) — https://www.legifrance.gouv.fr/codes/id/LEGIARTI000006279253/2006-08-03 (le lien relevé pointe sur une version historique : citer la version en vigueur).
39. tennis-de-table.com, « La FFTT publie une nouvelle version de Smartping pour Android » — https://www.tennis-de-table.com/la-fftt-publie-une-nouvelle-version-de-smartping-pour-android/ — ancien.
40. Application « FFTT » (App Store) — https://apps.apple.com/pg/app/fftt/id1148342433
41. GitHub, SuperPingPong (outils tiers autour des tournois FFTT) — https://github.com/SuperPingPong
