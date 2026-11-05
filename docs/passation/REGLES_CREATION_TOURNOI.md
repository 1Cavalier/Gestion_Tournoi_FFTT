# Règles de création d'un tournoi — classification des champs

> Classification révisée le 12 octobre 2026, section Général retravaillée le 14 octobre 2026 avec Quentin.
> Base : code du cœur (`ServiceCycleTournoi`, `ServiceCompletudeTournoi`), apports métier, règlements sportifs 2026-2027.
> Légende : 🔴 obligatoire homologation · 🟠 obligatoire publication · 🟡 recommandé · ⚪ optionnel · ➕ champ nouveau · ❓ à confirmer dans le règlement FFTT 2026-2027 (formulaire SPID).

---

## Principe

- Un tournoi suit le cycle **Brouillon → Homologué → Publié** (+ Annulé).
- Deux paliers d'exigence : ce qui bloque l'**homologation**, et ce qui bloque la **publication** (ouverture des inscriptions).
- Le **recommandé** guide sans bloquer ; l'**optionnel** n'est pas suivi.
- Valeur par défaut + surcharge : ce qui vient de la base (club) est proposé, jamais figé, sauf l'identité du club (nom, n° d'affiliation), qui n'est pas saisissable.
- Garde-fou produit : garder le bloquant au strict nécessaire, utiliser largement le recommandé.

---

## 1. Général (organisation et contacts)

Ordre d'affichage du formulaire : Le tournoi → Club organisateur → Lieu → Dates du tournoi → Correspondant.

**Le tournoi**
- 🔴 Nom du tournoi : libre ; proposé par défaut « Tournoi de/du/d'/de l' + nom du club » (`PropositionsTournoi`).
- 🔴 Niveau du tournoi = **catégorie IX.102** : promotionnel, départemental, régional, national B, national A, international. Remplace l'ancien « type de tournoi », supprimé car il faisait doublon (le type promotionnel est devenu une catégorie). Exigé dès l'homologation puisqu'il fixe l'échelon. Il fixe aussi **qui peut s'inscrire** (onglet Tournois, `EligibiliteTournoi`) : départemental = licenciés du département du club organisateur, régional = de sa ligue, licence loisir = promotionnel seulement (IX.101).
- Échelon d'homologation : **calculé, plus saisi** (IX.103 / IX.105). National, international, ou dotation globale > 3 000 € → FFTT (commission sportive fédérale). Sinon → ligue (commission sportive régionale), qui peut déléguer au comité départemental. Affiché en lecture dans le formulaire.
- 🟠 Numéro d'homologation (délivré après homologation).
- 🟡 Phase concernée (points retenus) : 1re ou 2e phase de la saison du tournoi (`PhasePoints`). Le règlement écrit « les points retenus sont ceux de la 2e phase 2026-2027 ».

**Club organisateur** (repris de la base FFTT, non modifiable ; choix dans une liste si le compte gère plusieurs clubs)
- 🔴 Nom du club
- 🔴 N° d'affiliation (= numéro du club FFTT)

**Lieu** (sous le club)
- 🔴 Adresse : voie, code postal, ville. Par défaut l'adresse du club en base, modifiable car certains tournois ne se jouent pas dans la salle du club. Bouton « Reprendre l'adresse du club ».
- ➕ Position (latitude, longitude) : **calculée, pas saisie**. L'adresse est géocodée à l'enregistrement de la section (Géoplateforme IGN, ex-API Adresse) et à la création du brouillon ; la position est gardée tant que le lieu ne change pas. Sans réseau, rien n'est stocké et la carte se rabat sur la préfecture du département (« position approchée »).
- ⚪ Salle (nom), heure d'ouverture : à venir.

**Dates du tournoi**
- 🔴 Date de début / date de fin = premier et dernier jour **de l'ensemble du tournoi**, toutes journées comprises (pas une seule journée). Demandées dès la création, car elles figurent sur la demande d'homologation.

**Correspondant**
- 🔴 Nom, prénom, email : par défaut l'**administrateur principal** du club. S'il n'a pas de compte, le responsable déclaré en base avec l'email officiel du club ; à défaut, le compte connecté. Peut être une personne tierce. Bouton « Reprendre l'administrateur principal ».
- ⚪ Téléphone, autres infos.
- ⚪ Responsable (nom, email, téléphone), contact d'urgence le jour J : à venir.

Le **site web** passe en section 4 (Complémentaire).

## 2. Tableaux et inscriptions

L'écran est en deux parties (16 octobre 2026).

**Échéances** (en haut)
- Rappel : « Vous avez sélectionné votre tournoi du … au … » (dates de la section Général).
- 🟠 ➕ Ouverture des inscriptions (jour + heure) : **avant le premier jour du tournoi** (le calendrier grise les jours suivants). L'inscription en ligne (onglet Tournois) n'est possible qu'entre l'ouverture et la clôture.
- 🟠 Clôture des inscriptions (jour + heure) : **entre l'ouverture et le premier jour du tournoi inclus**.
- La **clôture du pointage n'est plus une échéance du tournoi** : chaque tableau a ses propres heures de pointage.

**Tableaux** (grille récapitulative, bouton « Ajouter un tableau » en haut à droite)
- 🔴 Au moins un tableau
- 🔴 Au moins un tableau réservé aux dames (IX.104), affiché au-dessus de la grille.
- Grille : nom, jour, heure de début, points, public, places, prix, récompenses ; « Modifier » et « Retirer » (impossible dès qu'il y a des inscrits).

**Formulaire d'un tableau** (page dédiée, ajouté à la grille à la validation)
- 🔴 Nom : libre ; par défaut la prochaine lettre libre (A, B…) ; noms usuels proposés : Non numérotés, Open, Féminin. Choisir « Féminin » règle le sexe sur femmes ; « Open » règle toutes séries ; « Non numérotés » règle toutes séries, hors numérotés 1 à 1000. Unique dans le tournoi.
- 🔴 Catégorie de points : par tranche (min–max), points maximum, ou toutes séries ; ⚪ exclusion des numérotés (rangs 1 à N) hors tranche. Le « type » exigé pour l'homologation en est déduit automatiquement.
- Catégories d'âge : choix « Toutes catégories » (par défaut) ou « Sélectionner des catégories », qui déplie la liste (poussins à vétérans 90, raccourcis « tous les jeunes », « tous les vétérans »). Les cases restent mémorisées si l'on replie la liste. Sexe : mixte, hommes uniquement, femmes uniquement. **Contrôlées à l'inscription** (catégorie tirée de la date de naissance de la licence, II.302 / II.303 ; refus `CATEGORIE_NON_AUTORISEE`).
- 🔴 Jour : un seul jour, **compris dans les dates du tournoi** (le calendrier grise les autres jours ; contrôle à la validation). Pré-choisi si le tournoi dure un jour.
- 🔴 Heure de début ; 🟡 début et fin du pointage ; ⚪ ➕ fin théorique. Contrôles : début pointage < fin pointage ≤ début ; fin théorique > début.
- 🟠 Capacité ; ⚪ liste d'attente (places, 0 = aucune).
- 🟠 Prix : en ligne **et/ou** sur place (au moins un), 0 = gratuit.
- 🟡 Récompenses et lots : une ligne par place ou plage de places ; argent (€), **% des inscriptions** (part des droits d'engagement perçus sur le tableau, total ≤ 100 % ; ex. tableau dames 50 / 30 / 10 / 10 %, pour ne pas jouer à perte ; `TypeRecompense.PART_INSCRIPTIONS`, hors dotation fixe), lot (texte libre, mis en avant) ou réduction (%).
- 🟠 Format de compétition (seul disponible : poule de 3, 2 qualifiés ; présélectionné).
- ⚪ **Petite finale (match pour la 3e place)** : case du groupe « Déroulement », décochée par défaut (valeur du format). Décision de Quentin du 5 novembre 2026 (`QUESTIONS.md`) : réglable **par tableau** ; c'est une clause d'organisateur, les règlements fédéraux ne l'imposant pas pour les tournois (chapitre IX). Portée par `ConfigurationTableau.avecPetiteFinale()`, appliquée à la construction du tableau final (`ServiceLancementTableau.construirePhaseFinale`) ; sans effet si le tableau final n'a pas deux demi-finales (deux qualifiés seulement). Sans petite finale, les deux perdants des demi-finales sont classés **3es ex aequo** (podium et règlement). Le règlement généré l'écrit dans l'article Déroulement (« Une petite finale … dans le tableau « X » ; dans les autres tableaux, … troisièmes ex aequo »). Case figée une fois le tableau final tiré. JSON : champ `petiteFinale` de la configuration ; un ancien fichier est relu sans petite finale.
- ⚪ Par tableau, à venir : best-of, taille de poule, qualifiés par poule, format de phase finale (portés par le format).

**Règles d'inscription** (carte sous la grille, 21 octobre 2026, un seul bouton « Enregistrer les règles ») :
- ⚪ Tableaux par jour (`QuotasTableaux`) : maximum de tableaux par joueur et par joueuse (vide = illimité) ; identique chaque jour, ou case « le nombre varie selon le jour » qui ouvre une ligne par jour du tournoi. Remplace l'ancienne règle « + 1 tableau pour les joueuses ». **Tableau supplémentaire des joueuses** : liste « seulement si elles jouent [tableau] » (par défaut le premier tableau réservé aux dames, ou « sans condition ») ; quand la limite des joueuses dépasse celle des joueurs, le surplus n'est accordé que si l'un de leurs tableaux du jour est celui-ci (`QuotasTableaux.tableauBonusJoueuses`, suit le renommage du tableau). **Contrôlé à l'inscription** (`ServiceInscription`), jour par jour. Deux cases en plus : **« Obligatoire »** (le jour où il est joué, une joueuse doit être inscrite au tableau choisi avant de prendre un autre tableau ; refus `TABLEAU_FEMININ_OBLIGATOIRE`) et **« Interdire deux tableaux qui se suivent dans la journée »** (ordre des débuts du jour ; refus `TABLEAUX_SUCCESSIFS`). Les deux sont écrites dans l'article « Nombre de tableaux par joueur ». À l'inscription en ligne, une sélection est examinée en bloc, tableau des joueuses d'abord (`ServiceInscription.simuler`), et enregistrée tout ou rien.
- ⚪ Maintien (`ConditionsMaintien`) : tableau annulable sous N inscrits, décision prise le [date ≤ 1er jour] ; réduction de dotation en un clic : maintenue / selon le remplissage (sous X % de places → −Y %) / selon les absents (par tranche de N absents → −Y %).
- 🟡 Forfaits (`PolitiqueForfaits`) : trois moments — pendant les inscriptions, après la clôture et avant le tournoi, le jour J — chacun avec des boutons [Aucun remboursement | Partiel | Complet], la part en % si partiel, case « sur justificatif » ; case globale « frais de paiement en ligne non remboursés ». Complet seulement si les trois moments sont réglés.

**Ordonnancement automatique** (⚪, carte tout en bas de l'écran Tableaux, `core.OrdonnancementTableaux`) :
- Tri du plus faible au plus fort (plafond de points ; toutes séries ensuite, celles qui excluent des numérotés d'abord).
- Répartition : un tableau sur x pour chacun des x jours du tournoi.
- Dans chaque journée : moitié faible / moitié moyenne alternées — faible, moyen, faible + 1, moyen + 1…
- Option « tableaux réservés aux dames en dernier de leur journée » (cochée par défaut) : ils sortent de l'alternance.
- Écarts en alternance : 1er écart puis 2e écart, en boucle (par défaut 90 puis 120 min : A → 1 h 30 → B → 2 h → C…).
- Paramètres : début du premier tableau chaque jour, fin du pointage M minutes avant, ouverture du pointage P minutes avant. « Proposer un ordre » affiche l'aperçu ; « Appliquer cet ordre » remplace jours, horaires et pointages (fin théorique décalée d'autant). Refusé si des joueurs sont inscrits ou si une journée déborde après minuit.

## 3. Informations sportives et techniques

Écran dédié (17 octobre 2026) en quatre parties.

**Juge-arbitre**
- Encart des obligations et recommandations, adapté à la catégorie :
  - 🔴 au moins un juge-arbitre désigné, nom et grade (IX.104) ;
  - 🔴 grade du JA principal : **JA national** en international et national A, **au moins JA3** ailleurs ; en promotionnel, à défaut de JA3, avis préalable de la commission d'arbitrage (IX.104) ;
  - 🔴 le JA ne peut pas jouer dans le tournoi (IX.109) ;
  - 🟡 faire valider la désignation par la commission d'arbitrage compétente (IX.109) ; prévoir des adjoints si plusieurs tableaux en même temps (conseil).
- Recherche par nom, prénom ou numéro dans la base des licences ; chaque résultat montre son grade de JA.
- « Désigner » **refuse avec un message** si : pas de grade de JA sur la licence ; grade insuffisant pour la catégorie (principal) ; déjà désigné ; inscrit comme joueur (IX.109) ; un principal existe déjà. En promotionnel, un grade inférieur passe avec un avertissement.
- Rôle : principal (un seul, grade contrôlé) ou adjoint (tout grade de JA). Tableau : nom, prénom, rôle, grade, « Retirer ».
- Grades alignés sur le règlement : **JA1, JA2, JA3, JA national** (les anciens « JA départemental / régional » des fichiers sont relus en JA1 / JA2).

**Arbitres** (⚪)
- Même recherche ; refus si la licence ne porte pas de grade d'arbitre ; tableau nom, prénom, grade (AR, AN, AI…).

**Salle et matériel**
- Balles : 🟠 provenance (fournies par le club / apportées par les joueurs) ; 🟡 couleur (blanche / orange, 2.3.3) ; 🟡 marque. **Liste noire** (balles, marques, matières refusées, avec raccourcis : celluloïd, non agréées ITTF/FFTT, entraînement) **affichée uniquement si les joueurs apportent leurs balles**.
- Tables : 🟠 lignes « nombre / marque / couleur » des tables de **compétition**, plusieurs marques et couleurs possibles ; le nombre de tables du tournoi = leur total. ⚪ **Salle d'échauffement : nombre de tables** (`Tournoi.tablesEchauffement`), hors compétition ; total affiché « 12 de compétition + 8 en échauffement, soit 20 » ; repris dans l'article Matériel.

**Rappel du règlement et aire de jeu**
- Rappels calculés selon la catégorie et les choix : grade JA (IX.104) ; tables NF EN 14468-1/-2 et marques agréées (3.2.1.1) ; surface foncée et mate (2.1.4) ; balles plastique 40 mm 2,7 g mates, couleur choisie (2.3) ; si balles des joueurs, annoncer couleur et refus ; en national/international espace ≥ 12 × 6 m × 4,5 m (3.2.3.1, IX.110) et éclairage ≥ 500 ou 750 lux (3.2.3.3), sinon 12 × 6 m en référence ; entourages foncés de 50 à 100 cm (3.2.3.2).
- ⚪ Aire de jeu : longueur, largeur, éclairage, pré-remplis 12 m / 6 m / 500 lux, modifiables, bouton « Remettre les valeurs réglementaires ».
- Retirés de cette section : **horaire prévu des finales** et **début des matchs** (désormais par tableau ; n'est plus exigé au niveau du tournoi pour publier).

## 4. Complémentaire

Écran dédié (21 octobre 2026).

**Règlement du tournoi** (🟠, exigé pour publier)
- Deux modes : **généré** à partir des informations saisies (proposé par défaut), ou **déposé** par le club (fichier PDF).
- Règlement généré, adaptatif (`core.reglement.GenerateurReglement`), articles numérotés selon les données :
  Organisation · Homologation · Participants (public IX.102, licence promotionnelle IX.101, saison, **phase des points**) · Tableaux (+ clause d'annulation sous N inscrits à la date choisie) · Nombre de tableaux par joueur (identique ou par jour) · Déroulement · Inscriptions (site, dates, liste d'attente, sur place, **paiement HelloAsso**) · Droits d'engagement (+ **remboursement selon les trois moments du forfait**) · Dotations (+ réduction selon remplissage ou absents, IX.108) · Pointage · Juge-arbitrage (IX.109) · Matériel (3.2.1.1) · Tenue et comportement · Résultats (IX.107) · Responsabilité · **articles ajoutés** · Acceptation · Contact.
- **Articles ajoutés** (`ArticleOptionnel`, menu « Ajouter un article », chacun une seule fois sauf l'article libre, « Retirer » sur chaque bloc) :
  - Droit à l'image et données personnelles (présent par défaut) ;
  - Buvette : horaires ;
  - Ouverture de la salle : heure ;
  - Tirage au sort public : minutes après la fin du pointage ;
  - Sur-classement des jeunes : catégorie minimale (poussins à juniors) ;
  - Forfait non excusé : perte des points de la première partie (IV.202.2), non imprimé en promotionnel ;
  - Informations pratiques : parking, vestiaires et douches, accès PMR, précisions ;
  - Article libre : titre + texte (plusieurs possibles).
- Ce qui manque reste visible dans le texte : **[à compléter : …]**, surligné, seul signal des manques : **ni bandeau « projet de règlement » ni check-list « à faire avant le dépôt »**, ni dans l'aperçu ni dans le PDF. L'écran Complémentaire indique simplement « N passages restent à compléter ».
- Aperçu dans l'application ; **téléchargement en PDF** (A4, police Inter intégrée, trous surlignés, en-tête de tableau répété en cas de saut de page, « page n / N » ; même contenu que l'aperçu). L'export HTML est retiré de l'écran. Le règlement est aussi téléchargeable depuis la fiche du tournoi (onglet Tournois) ; un règlement déposé y est copié tel quel.

**Feuille d'engagement** : n'est plus un fichier joint, elle est **générée** (bouton « Feuille d'engagement (PDF)… » de l'écran Complémentaire, à côté du règlement, et de la fiche du tournoi). Classe `core/reglement/GenerateurFeuilleEngagement` ; même rendu PDF que le règlement, titres « 1. Le tournoi » (`FORMAT_TITRE`). Six sections remplies depuis le tournoi : 1. Le tournoi (club, n° d'homologation ou « attribué par la FFTT », dates, salle, tables, JA ou « À désigner », clôture, tirage, correspondant) ; 2. Comment s'inscrire (lien de paiement) ; 3. Le joueur (cases vides) ; 4. Choix des tableaux (texte des quotas de l'article 5, grille □ / tableau / horaire / conditions / places / tarif en ligne / tarif sur place, format et pointage) ; 5. Paiement ; 6. Rappels et engagement (licence, catégories non admises, tenue et balles, phase des points, forfaits, autorisation parentale, signature). Pas de nom d'auteur ni de date de rédaction. Le champ de fichier joint reste relu dans les anciens fichiers.

**Image promotionnelle** (⚪) : PNG ou JPEG, avec vignette.

**Conditions de participation** (🟡), chacune expliquée en une phrase :
- **catégories non admises** (choix du club : poussins, benjamins, minimes ; `Tournoi.categoriesExclues`) : « Les poussins (garçons et filles) ne sont pas admis. » dans l'article Participants. Vérifié dans les règlements 2026-2027 : **aucune interdiction fédérale** de catégorie en tournoi (IX.101-IX.111) ni de lien avec les prix en argent ; « Les poussins ne peuvent pas participer » ne vise que les finales par classement (X.101). **Contrôlé à l'inscription** depuis le 2 novembre 2026 (catégorie tirée de la date de naissance de la licence ; refus `CATEGORIE_NON_ADMISE`) ;
- licence promotionnelle : **non cochée par défaut** ; admise de droit en promotionnel, ailleurs dans les tableaux qui lui sont réservés (IX.101) ;
- joueurs étrangers : **bloqués sauf en tournoi international** (case désactivée ailleurs). ❓ IX.102 ouvre aussi les tournois national B, régional et départemental aux licenciés de fédérations étrangères dont le territoire est contigu : à confirmer si l'on veut le proposer.

**Communication** : ⚪ site web du tournoi (repris dans l'article Inscriptions).

**Paiement** (🟡, en bas de l'écran, `Paiement`) :
- Choix de la plateforme : **HelloAsso** (seul disponible), Plateforme PingManager et Autre lien grisés « bientôt disponible ».
- Mode d'emploi en 4 étapes : se connecter à l'espace de l'association sur helloasso.com (ou le créer : SIRET ou RNA, compte bancaire) ; créer une billetterie avec un tarif par tableau au prix en ligne ; ajouter les champs n° de licence, nom, prénom, club ; publier et coller le lien.
- Lien : adresse web obligatoire (https://…) ; alerte si ce n'est pas une page helloasso.
- Repris dans l'article Inscriptions : « Le paiement se fait en ligne via HelloAsso : lien. Une inscription n'est validée qu'à réception du paiement. »
- Inscription en ligne (onglet Tournois) : étape « Phase de paiement — faite ultérieurement » ; le montant (tarif en ligne des tableaux obtenus) est noté « à régler », le lien du club est affiché, aucun appel HelloAsso pour l'instant.

---

## Homologation et publication (bas de la vue d'ensemble)

- Nouveau statut **Demande d'homologation** (`StatutTournoi.DEMANDE_HOMOLOGATION`) entre Brouillon et Homologué ; groupe dédié dans Gestion des tournois.
- **Demander l'homologation** : actif si le dossier d'homologation est complet (grille 🔴) ; brouillon → demande. « Retirer la demande » ramène au brouillon.
- **Publier le tournoi** : actif depuis la demande (ou l'état homologué) si tout le 🟠 est rempli, dont le numéro d'homologation saisi en section Général ; enregistre l'homologation puis publie (inscriptions ouvertes). Sinon, la raison est affichée sous le bouton.
- Un tournoi publié apparaît sur la **carte de l'onglet Tournois** jusqu'à son dernier jour ; la vue d'ensemble porte une carte **Inscriptions** (remplissage, accès à la vue par tableau / par joueur).

---

## Comptes et supervision

- **Compte ultra admin** (supervision PingManager) : `admin@pingmanager.fr` / `PingAdmin123/`, créé au démarrage, rattaché à aucun club (`DonneesDemo.ULTRA_ADMINS`). Menu **Supervision → Tous les tournois** : tous les tournois de tous les clubs (dossier `tournois` + récents), regroupés par club, avec statut et avancement ; « Ouvrir » donne l'accès complet au tournoi, avec un bandeau « Mode supervision ».
- **Gestion des tournois** d'un club : n'affiche plus que les tournois de ses clubs (n° d'affiliation, ou nom du club pour les anciens fichiers).
- **Nettoyage du 21/10/2026** (une seule fois, marqueur `tournois/.nettoyage-brunoy-2026-10-21`) : les autres tournois de Brunoy sont retirés de la liste et leurs fichiers déplacés dans `tournois/archives` ; le National B 2027 est recréé avec les décisions de la réunion.

---

## Code couleur à l'écran

- **Rond** devant le libellé = dépendance : bleu = homologation, cyan = publication, anneau bleu = recommandé, gris = optionnel.
- **Cadre** du champ = état, mis à jour en direct : vert = rempli, orange = obligatoire (homologation ou publication) et vide, gris = facultatif et vide.
- Une légende courte en tête de chaque formulaire et de la vue d'ensemble ; la vue d'ensemble reprend le même code (rond + badge « rempli / à remplir / facultatif »).

---

## Contrôles de cohérence (validation, pas des champs)

- Clôture des inscriptions < clôture du pointage < début des matchs
- Dates des tableaux comprises entre début et fin du tournoi
- Bornes de points des tableaux cohérentes avec le niveau
- Capacité totale réaliste vs nombre de tables et durée
- Pas de chevauchement horaire entre tableaux ouverts aux mêmes joueurs
- ➕ Délai de dépôt de la demande d'homologation : au moins deux mois avant la date retenue pour les tournois relevant de la commission fédérale (IX.104 a)

---

## État d'implémentation (au 21 octobre 2026)

**Fait :**
- `NiveauTournoi` : catégorie `PROMOTIONNEL` ajoutée, libellés lisibles. `TypeTournoi` déprécié : n'est plus lu par les règles ni l'écran, conservé uniquement pour relire les anciens fichiers. Un ancien type « promotionnel » ou « international » pré-remplit la catégorie dans le formulaire.
- `EchelonHomologation.pour(niveau, dotation)` (IX.103) ; `Tournoi.getEchelonHomologation()` calcule l'échelon dès que la catégorie est connue.
- `ServiceCycleTournoi` : l'homologation exige la **catégorie** (au lieu du type) ; la publication ne la redemande plus.
- `ServiceCompletudeTournoi` : section Général sans type ni échelon, catégorie 🔴, site web déplacé en Complémentaire.
- `Club.codePostal` (+ persistance) pour pré-remplir l'adresse ; `ContexteApplication.clubsGeres()` et `correspondantParDefaut(numeroClub)`.
- Création de brouillon : club, n° d'affiliation, adresse et correspondant repris de la base dès la création.
- Corrigé : enregistrer une adresse incomplète faisait échouer l'enregistrement.
- Tests : 301 verts (fixtures passées du type à la catégorie, plus des tests sur l'échelon, l'adresse du club et la proposition de nom).

- Lot tableaux (16/10) : `InfosReglement.ouvertureInscriptions` (doit précéder la clôture) ; `HorairesTableau.finTheorique` et `getJour()` ; `TarifInscription` avec canaux en ligne / sur place ; `Tournoi.remplacerTableau` / `retirerTableau` (refusés s'il y a des inscrits). Persistance rétrocompatible. Publication : exige l'ouverture des inscriptions, n'exige plus la clôture du pointage. Écrans `EcranTableaux` et `FormulaireTableau`. Tests : 312 verts.
- Lot sportif (17/10) : `GradeJugeArbitre` JA1/JA2/JA3/JA national (`depuisCode`, relecture tolérante) ; `NiveauTournoi` exige JA3 ou JA national, dérogation en promotionnel ; IX.104 **bloque l'homologation** (`ServiceCycleTournoi.gradeJugeArbitreConforme`) ; `RoleJugeArbitre` ; email du JA facultatif ; `AffectationArbitre`, `LotTables`, `CouleurBalles`, balles refusées ; `RegistreLicences.rechercher` ; officiels fictifs dans la démo. Écran `EcranSportif`. Tests : 319 verts.
- Brouillon de démo (21/10) : « Tournoi National B de Brunoy 2027 » (`ui.infra.TournoiDemoBrunoy`), repris du projet de règlement, option B (7 tableaux par jour, Non numérotés) : 14 tableaux avec horaires, places, prix et dotations (3 230 €), échéances, quotas par jour, dotation réduite sous 50 %, forfaits, JA fictifs de la démo, 20 tables, balles des joueurs, HelloAsso (lien d'exemple), articles buvette / droit à l'image / remise des récompenses. Créé une seule fois au démarrage (fichier `tournois/Tournoi_National_B_Brunoy_2027-demo`), à supprimer pour repartir de la version d'origine. Seul manque : le numéro d'homologation. Tests : 342 verts (après l'ajout du tableau exigé pour le 3e tableau des joueuses).
- Ordonnancement automatique des tableaux et étapes demande d'homologation / publication (21/10). Tests : 349 verts.
- Réunion du 21/10 (soir) : compte ultra admin + supervision, nettoyage de Brunoy, écarts 1 h 30 / 2 h et dames en dernier, joueuses obligées au tableau dames le samedi, pas de tableaux consécutifs, 12 + 8 tables, aucun remboursement ; National B 2027 : 13 tableaux (samedi 600, 1500, 900, 1800, 1200, 2000, TS dames ; dimanche 800, 1600, 1000, 1900, 1300, Non numérotés), dotation 2 710 €. Tests : 354 verts.
- Retouches du règlement (21/10, 23 h) : article « Nombre de tableaux » en liste + règle des tableaux consécutifs + note « * » pour le tableau dames obligatoire ; forfaits en une phrase quand la règle est la même aux trois moments (« aucun remboursement… toute inscription est définitive ») et formulation « est / n'est pas remboursé » sinon ; grille des dotations dans l'ordre des classements ; pointage (appel du premier de la liste d'attente) ; buvette en deux phrases. Brunoy : Non numérotés 200 / 100 / 50 €, ¼ de finalistes 10 € partout, dotation 3 390 € ; tournoi recréé une fois (marqueur `.demo-brunoy-v3`).
- Dotation en % des inscriptions (21/10, 23 h 45) : règlement « doté de X € en espèces, plus une part des inscriptions », grille « 50 % des inscr. », note sur la base de calcul ; réduction sous le seuil de remplissage chiffrable ou non (vide = « sa dotation en espèces peut être réduite ») ; titre + intro + début de tableau gardés ensemble dans le PDF. Brunoy : Toutes séries dames 50 / 30 / 10 / 10 % + ¼ à 10 €, dotation fixe 3 190 € ; recréé une fois (`.demo-brunoy-v4`). Tests : 356 verts.
- Lot règles d'inscription et paiement (21/10, phase 7) : `PhasePoints`, `QuotasTableaux`, `ConditionsMaintien`, `PolitiqueForfaits`, `ArticleOptionnel`, `Paiement` ; `OptionsReglement` réduit au mode + articles (les anciennes clauses ont rejoint les sections Général et Tableaux ; un ancien fichier avec « droit à l'image » est relu en article) ; persistance des nouveaux champs ; complétude : phase des points (Général), forfaits (Tableaux), lien de paiement (Complémentaire), tous 🟡 ; `RenduPdfReglement` (PDFBox 2.0.29, dépendance ajoutée au pom). Écrans : phase dans Général, `CarteReglesInscription` dans Tableaux, bibliothèque d'articles et carte Paiement dans Complémentaire. Tests : 334 verts.
- Lot complémentaire (21/10) : `OptionsReglement` (mode déposé/généré + clauses), `Tournoi.aUnReglement()` (exigé pour publier, l'un ou l'autre), image promotionnelle, conditions fermées par défaut ; `core.reglement` : `GenerateurReglement`, `DocumentReglement`, `RenduHtmlReglement`, `Formats`, `LibellesTableau` (libellés partagés avec l'écran). Écrans `EcranComplementaire` et `ApercuReglement`. Tests : 329 verts.

**Différé :**
- Faire bloquer l'homologation sur le n° d'affiliation dans `ServiceCycleTournoi` : il est désormais toujours rempli à la création, donc peu urgent. Le récapitulatif peut toutefois différer de la liste sur un ancien tournoi tant qu'il n'a pas été réenregistré.
- Délégation ligue → comité départemental : dépend de chaque ligue, à paramétrer si besoin.
- Champs ⚪ pratiques (contact urgence, tables & sol, balles couleur/ITTF, RGPD, infos pratiques, hébergement).

**À confirmer (❓) :** l'échelon d'un tournoi promotionnel (rattaché ici à la ligue au titre d'IX.105, faute de mention explicite à IX.103).
- Fin de la phase pré-tournoi (22/10, 0 h) : intro des dotations sans « plus une part des inscriptions » ; sous la grille, note des pourcentages et paragraphe de réduction séparés ; catégories non admises (Brunoy : poussins). Brunoy recréé une fois (`.demo-brunoy-v5`). Tests : 356 verts.
- Article « Nombre de tableaux » simplifié (23/10) : quand joueurs et joueuses ont la même limite chaque jour, « Les joueurs et les joueuses peuvent s'inscrire à 2 tableaux maximum par jour. Le samedi 17 avril, les joueuses peuvent s'inscrire à un troisième tableau au choix, à condition d'avoir sélectionné le tableau « Toutes séries dames »*. » ; sinon, liste jour par jour comme avant. Tests : 356 verts.
- Article « Nombre de tableaux », tableau dames imposé (23/10) : « Chaque joueur et chaque joueuse peut s'inscrire à 2 tableaux maximum par jour. Le samedi 17 avril, toute joueuse qui s'inscrit doit aussi participer au tableau « Toutes séries dames » : ce tableau imposé s'ajoute à ses 2 tableaux au choix, soit 3 tableaux au maximum. » (plus de note « * »). Si le tableau dames n'est pas obligatoire : « Le samedi 17 avril, les joueuses inscrites au tableau « Toutes séries dames » peuvent s'inscrire à un troisième tableau de leur choix. » Tests : 357 verts.
- Article « Nombre de tableaux » conditionnel (23/10) : le texte suit les options (tableau dames imposé → « s'ajoute à ses 2 tableaux au choix » ; en option → « les joueuses inscrites au tableau … peuvent s'inscrire à un troisième tableau » ; sans condition ; imposé sans surplus → « compte parmi ses 2 tableaux » ; limites différentes selon le jour ou le sexe → liste détaillée ; obligation un jour sans limite → note conservée). **Aperçu en direct** « Ce que dira le règlement » dans la carte Règles d'inscription (`GenerateurReglement.apercuNombreDeTableaux`). Tests : 364 verts.
- Article 4 et grille des dotations (26/10) : colonne « Tableau » = nom du tableau, nouvelle colonne « Points » (≤ 600 pts, Toutes séries…) ; « Joueurs admis » = Tous / Dames / Messieurs + catégories. Brunoy : tableaux par points nommés A à K dans l'ordre du planning (samedi A–F, dimanche G–K), Toutes séries dames et Non numérotés inchangés ; recréé une fois (`.demo-brunoy-v6`). Tests : 364 verts.
- Tournoi de Brunoy recalé sur le règlement validé par le club (26/10) : samedi 10 et dimanche 11 avril 2027, 157 Route de Brie (91800 Brunoy), inscriptions du 1er février au vendredi 9 avril 2027 à 20 h, tableaux « 600 pts »… (sans lettres), écarts 1 h puis 1 h 30 à partir de 8 h 30 (samedi 8 h 30 → 16 h pour le Toutes séries dames, dimanche 8 h 30 → 14 h 30), ¼ de finalistes 10 / 15 / 20 € selon le niveau (aucun pour le tableau dames), dotation 3 390 €, 20 tables Cornilleau / Joola bleues + salle d'échauffement de 8, balles fournies par le club (Nittaku, Tibhar, Butterfly, DHS, Joola, Vicats, Andro, Cornilleau), contact Serge BOULIER, **pas de juge-arbitre désigné** (la demande d'homologation reste bloquée tant qu'il manque). Recréé une fois (`.demo-brunoy-v7`). Tests : 367 verts.
- Brunoy (26/10, suite) : une lettre par tableau dans l'ordre du planning — samedi A ≤ 600, B ≤ 1500, C ≤ 900, D ≤ 1800, E ≤ 1200, F ≤ 2000, « G – Toutes séries dames » ; dimanche H ≤ 800, I ≤ 1600, J ≤ 1000, K ≤ 1900, L ≤ 1300, « M – Non numérotés » (hors n° 1 à 999). Recréé une fois (`.demo-brunoy-v8`). Tests : 367 verts.
- Tarif dans l'article Tableaux (29/10) : colonne « Tarif d'inscription* » (tarif en ligne, sinon « X € (sur place) »), note « * Tarif de l'inscription en ligne ; voir les modalités à l'article N « Droits d'engagement » » (numéro résolu une fois les articles numérotés). Article « Droits d'engagement » : avec deux tarifs, une phrase « L'inscription coûte … ; pour les tableaux …, elle coûte … » ; au-delà, grille. PDF : largeur minimale de colonne = mot le plus long (plus de chevauchement à 8 colonnes). Brunoy : 10 € en ligne pour F (≤ 2000), K (≤ 1900) et M (Non numérotés), sur place inchangé à 10 € ; recréé une fois (`.demo-brunoy-v9`). Tests : 367 verts.

- Juges-arbitres de Brunoy désignés (29/10) : **Samuel LOOBUYCK, JA3, principal** (grade minimum en national B, IX.104) et **William PRIOU, JA2, adjoint**. Numéros de licence pas encore communiqués : valeur provisoire « Licence à renseigner (…) », à remplacer. Le règlement écrit « Samuel LOOBUYCK, juge-arbitre de grade JA3, assisté de William PRIOU (JA2) » (le grade de l'adjoint est désormais cité) ; la feuille d'engagement cite les deux. La demande d'homologation n'est plus bloquée : seul le n° d'homologation reste à compléter. Recréé une fois (`.demo-brunoy-v10`). Tests : 370 verts.

- Onglet Tournois et inscriptions (2 novembre 2026) : adresse du lieu **géocodée** à l'enregistrement (Géoplateforme IGN), position gardée tant que le lieu ne change pas, repli sur la préfecture du département sans réseau (non stocké). **Catégories non admises** et **catégories par tableau** contrôlées à l'inscription (II.302 / II.303). Accès selon IX.102 (départemental : département du club organisateur ; régional : sa ligue) et IX.101 (licence loisir : promotionnel). Inscription en ligne entre l'ouverture et la clôture, sélection examinée en bloc et enregistrée tout ou rien, paiement « fait ultérieurement ». Brunoy **publié** en démo avec un n° d'homologation fictif et rempli par 375 joueurs fictifs ; cinq autres tournois publiés (`.demo-brunoy-v11`). Détail : `docs/passation/PASSATION_ONGLET_TOURNOIS.md`, section 7. Tests : 399 verts.

- Phase finale reliée au jour J (5 novembre 2026, branche `jour-j/arbre-petite-finale`) : option **petite finale par tableau** (formulaire du tableau, configuration, JSON `petiteFinale`, règlement généré, construction du tableau final, podium) ; **graine du tableau final** conservée avec le tableau (`Tableau.getGraineTableauFinal`, JSON `graineTableauFinal`, IX.109), anciens fichiers relus sans elles.
