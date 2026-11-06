# Questions ouvertes pour Quentin

Les agents notent ici les choix qu'ils n'ont pas voulu trancher seuls (règle
FFTT ambiguë, décision produit). Quentin répond sous chaque question, puis
l'entrée est déplacée dans « Tranché » ou supprimée.

Format d'une entrée :

```
### AAAA-MM-JJ — Titre court
- Contexte : tâche / fichier concerné
- Article FFTT : numéro + extrait court (si applicable)
- Options : A) … B) …
- Choix provisoire de l'agent : …
- Réponse de Quentin :
```

## En attente

### 2026-11-05 — PDFBox dans le cœur
- Contexte : `core/reglement/RenduPdfReglement.java` importe PDFBox, alors que
  la règle d'or veut un cœur sans dépendance externe.
- Options : A) tolérer (situation actuelle, documentée dans `CLAUDE.md`) ;
  B) déplacer le rendu PDF dans un package hors cœur (par exemple
  `com.pingmanager.export.pdf`), le cœur ne produisant que le
  `DocumentReglement`.
- Choix provisoire : A, sans l'étendre.
- Réponse de Quentin :

### 2026-11-05 — Lancer un tableau avec des repêchés qui n'ont pas pointé
- Contexte : écran de pointage du jour J (`EcranPointageTableau`),
  `ServiceLancementTableau.lancer`. Après la clôture, les repêchés sont inscrits
  mais « à pointer ». Le cœur les place en poule s'ils sont encore là au
  lancement (comportement inchangé).
- Options : A) l'écran bloque le tirage tant qu'un repêché n'a pas été confirmé
  ou retiré ; B) l'écran avertit seulement ; C) le lancement retire
  automatiquement les repêchés non pointés.
- Choix provisoire de l'agent : A (à l'écran seulement, le cœur n'est pas
  modifié ; `ResultatVerificationLancement.getNonPointes` donne la liste).
- Réponse de Quentin :

### 2026-11-05 — Tirage « public » et graine (IX.109)
- Contexte : carte « Tirage au sort des poules ».
- Article FFTT : IX.109 « […] un juge-arbitre qui est le responsable direct du
  tirage au sort […] ». Le texte ne parle ni de tirage public ni de graine.
- Options : A) graine de 6 chiffres proposée, modifiable, affichée et conservée
  avec le tableau (même graine + mêmes présents = même tirage) ; B) graine
  imposée par le logiciel, non modifiable ; C) graine tirée physiquement devant
  les joueurs et saisie par le JA (possible avec A).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-05 — Manches d'un match perdu par forfait ou abandon
- Contexte : saisie d'une issue non normale (`SaisieResultatMatch.enregistrerIssue`).
- Article FFTT : I.303 a) (0 point pour le fautif) ; I.303 c) départage par
  « le quotient des manches gagnées par les manches perdues » entre ex æquo. Le
  texte ne dit pas quelles manches compter pour une partie non jouée ou
  abandonnée.
- Options : A) aucune manche enregistrée (0-0 pour le quotient) ; B) score
  fictif au meilleur des manches (3-0, 11-0) au profit du vainqueur ;
  C) manches réellement jouées avant l'abandon.
- Choix provisoire de l'agent : A (les manches déjà saisies sont effacées quand
  on déclare l'issue). Réversible : changement localisé dans `SaisieResultatMatch`.
- Réponse de Quentin :

### 2026-11-05 — Inscriptions sur un tableau déjà en pointage ou lancé
- Contexte : `ServiceInscription` ne regarde pas le statut du tableau ; une
  inscription en ligne reste techniquement possible sur un tableau en pointage
  ou lancé (le joueur n'apparaîtrait pas dans les poules).
- Options : A) refuser l'inscription dès l'ouverture du pointage ; B) n'autoriser
  que l'inscription sur place par le JA pendant le pointage ; C) laisser tel quel.
- Choix provisoire de l'agent : C (non modifié dans ce lot, hors périmètre),
  signalé pour décision.
- Réponse de Quentin :

### 2026-11-05 — 3e place sans petite finale
- Contexte : vue « Phase finale » (`core/ServicePhaseFinale.podium`,
  `ui/ecran/PresentationPhaseFinale.lignesPodium`). Le seul format du
  registre (P3Q2) ne prévoit pas de petite finale : en pratique, aucun
  tableau n'en a aujourd'hui.
- Article FFTT : rien dans le chapitre IX (tournois, IX.101 à IX.111) ni en
  I.304 / I.305. La règle « lorsque plusieurs places ne sont pas jouées, les
  joueurs concernés marquent le même nombre de points » (barème de points du
  chapitre III, après III.501) concerne le Critérium fédéral, pas les tournois.
- Options : A) les deux demi-finalistes battus sont 3es ex aequo ;
  B) départage (points-classement, ou adversaire battu par le vainqueur) ;
  C) proposer une petite finale par tableau (nouveau format ou option du
  tableau, à afficher dans le règlement).
- Choix provisoire de l'agent : A (affiché « 3e ex aequo », avec une note).
  Si une petite finale existe, son vainqueur est seul 3e et son perdant 4e.
- Réponse de Quentin (5 novembre) : **prévoir la petite finale** → option C,
  réglable par tableau. Sans petite finale, garder A (3es ex aequo) en
  attendant une décision contraire.
- **Traité** (5 novembre, branche `jour-j/arbre-petite-finale`) : case
  « Petite finale (match pour la 3e place) » dans le formulaire du tableau
  (`ConfigurationTableau.avecPetiteFinale`), écrite dans le règlement généré,
  appliquée au tirage du tableau final ; sans elle, 3es ex aequo.

### 2026-11-05 — Graine du tirage du tableau final non enregistrée
- Contexte : `ServicePhaseFinale.construire` renvoie la graine du second
  tirage ; la vue l'affiche, mais le modèle `Tableau` ne la garde pas et le
  fichier JSON non plus. Après fermeture du logiciel, la vue indique « graine
  non conservée ».
- Article FFTT : IX.109 (le juge-arbitre est responsable du tirage au sort).
- Options : A) ajouter la graine au tableau et au JSON (champ facultatif,
  anciens fichiers relus sans elle, test `RoundTripTournoiTest`) ; B) en rester
  à l'affichage pendant la session.
- Choix provisoire de l'agent : B (pas de changement du format de
  sauvegarde sans accord).
- Réponse de Quentin :
- **Traité** (5 novembre, branche `jour-j/arbre-petite-finale`, à la demande
  du plan B de `docs/PLAN_INTERFACE.md`) : option A, champ JSON facultatif
  `graineTableauFinal` (anciens fichiers relus sans graine,
  `RoundTripTournoiTest`), affichée sur le tableau final.

### 2026-11-05 — Place des exempts (byes) dans le tableau final
- Contexte : `core/PlacementQualifiesService` (existant, non modifié) place
  les 1ers puis les 2es par tirage ; les places restées vides deviennent des
  byes, où qu'elles soient. Constaté avec la vue : deux byes peuvent se faire
  face au premier tour (un joueur est alors exempté deux tours de suite, ou
  le vainqueur d'un 1er tour saute le 2e), et deux exemptés peuvent se
  rencontrer au 2e tour (exemple : 26 qualifiés dans un tableau de 32).
- Article FFTT : I.304 / I.305 règlent les têtes de série, les 2es de poule
  et les clubs (y compris au 2e tour si le tableau est incomplet), mais ne
  disent pas où mettre les exempts.
- Options : A) laisser tel quel ; B) donner les byes en priorité aux têtes
  de série (1ers de poule), un bye par paire du premier tour au plus, comme
  dans les tableaux habituels ; C) demander la pratique du juge-arbitre / de
  GIRPE.
- Choix provisoire de l'agent : A (aucun changement du tirage), l'affichage
  gère tous les cas.
- Réponse de Quentin :

### 2026-11-05 — Corriger un résultat quand la suite est jouée
- Contexte : vue « Phase finale ». Un score peut être corrigé ; si la
  correction change le vainqueur, les matchs suivants non joués sont mis à
  jour. Si un match suivant est déjà en cours ou joué, la correction est
  refusée (`SUITE_DEJA_JOUEE`).
- Options : A) refus (actuel) ; B) proposer d'annuler en cascade les matchs
  suivants (avec confirmation du juge-arbitre).
- Choix provisoire de l'agent : A, le plus prudent.
- Réponse de Quentin :

### 2026-11-05 — Données de démo et supervision (sécurité S-01, S-02)
- Contexte : `RAPPORT_SECURITE.md`. Le compte ultra admin, le compte réel de
  Quentin et l'email de Serge BOULIER sont créés sur chaque poste.
- Options : A) mode démo activé à la demande, identités fictives partout,
  supervision retirée de la version de bureau ; B) mode démo, mais supervision
  gardée avec un rôle stocké et un mot de passe choisi à l'installation.
- Recommandation : A.
- Réponse de Quentin :

### 2026-11-05 — Preuve de possession d'une licence (sécurité S-05)
- Contexte : n'importe quel compte peut se lier à n'importe quelle licence.
- Options : A) code envoyé à l'email connu de la FFTT (dépend de l'API FFTT) ;
  B) validation par le club du joueur ; C) A quand l'API sera disponible, B
  en attendant.
- Réponse de Quentin :

### 2026-11-05 — Service d'envoi d'emails (sécurité S-03, S-16)
- Contexte : nécessaire pour vérifier les emails et envoyer les codes.
- Options : SMTP d'un compte dédié (Gmail, OVH…), service transactionnel
  (Brevo, Mailjet…). Identifiants hors du dépôt dans tous les cas.
- Réponse de Quentin :

### 2026-11-05 — Veille des dépendances (sécurité S-11)
- Options : A) Dependabot sur GitHub (aucune dépendance ajoutée au projet) ;
  B) plugin Maven OWASP dependency-check.
- Recommandation : A.
- Réponse de Quentin :

### 2026-11-05 — Temps de repos entre deux parties d'un même joueur (salle)
- Contexte : vue « Salle » du jour J (`core.ServiceSalle`, `Ordonnanceur`).
  Un match devient « prêt » dès que ses deux joueurs sont libres, même s'ils
  sortent à l'instant d'une autre table.
- Article FFTT : 3.3.1.2.2 « Le juge-arbitre est responsable de […] la
  programmation des parties en fonction de l'horaire retenu et des tables
  disponibles » ; IX.109 « […] de l'horaire des parties ». Aucun texte trouvé
  dans `docs/reglements/` sur un temps de repos minimal en tournoi individuel
  (recherche « repos », « retard », « minutes »).
- Options : A) aucun repos imposé, le juge-arbitre décide (la priorité favorise
  déjà les joueurs qui attendent depuis longtemps) ; B) délai minimal
  configurable par le club (clause d'organisateur), les matchs concernés
  apparaissant dans « Bloqués » avec le motif « au repos jusqu'à 10 h 42 ».
- Choix provisoire de l'agent : A (comportement de l'ordonnanceur inchangé).
- Réponse de Quentin :

### 2026-11-05 — Autosave de session et reprise à l'écran
- Contexte : vue « Salle ». Chaque action (envoi sur une table, annulation,
  score) enregistre aussitôt le fichier du tournoi, qui porte déjà le statut
  « en cours », le numéro de table et l'heure de début : rouvrir le tournoi
  rend la salle telle qu'elle était (testé). L'autosave de session
  (`ServiceAutosave`, `SessionSalle`) sait aussi la sauvegarder et la
  reprendre (testé), mais l'interface ne l'active pas et ne propose pas de
  reprise au démarrage (situation antérieure à ce lot).
- Options : A) garder l'enregistrement immédiat du tournoi comme seule source
  (choix actuel) ; B) activer en plus l'autosave de session pendant le jour J
  et proposer la reprise au démarrage (utile surtout pour l'apprentissage des
  durées de match, qui n'est pas encore affiché).
- Choix provisoire de l'agent : A (une seule source de vérité, pas de risque
  de divergence entre deux fichiers).
- Réponse de Quentin :

### 2026-11-05 — Durée « longue » d'une partie en salle
- Contexte : carte d'une table, la durée passe en rouge au-delà de 40 minutes
  (`PresentationSalle.DUREE_LONGUE`), pour attirer l'œil du juge-arbitre.
- Options : A) seuil fixe de 40 min ; B) seuil réglable par tournoi ; C) seuil
  tiré de l'estimateur de durées (`EstimateurDuree`, selon la phase et le
  niveau).
- Choix provisoire de l'agent : A (affichage seulement, aucune règle).
- Réponse de Quentin :

### 2026-11-05 — Tournoi publié puis annulé
- Contexte : une fois publié, le règlement est figé. Si le tournoi est ensuite
  annulé, la garde ne s'applique plus (elle vaut seulement pour l'état « publié »).
  Le règlement redevient donc modifiable, et `reglement()` régénère le texte en
  direct. La copie figée est conservée mais ignorée.
- Article FFTT : aucun (choix de l'application, pas une règle sportive).
- Options : A) garder le règlement figé même après annulation (sécurité, mais
  un tournoi annulé ne peut pas être corrigé) ; B) rendre modifiable après
  annulation (comportement actuel, plus souple).
- Choix provisoire de l'agent : B, le comportement actuel, qui est le plus
  réversible.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Enchaîner un joueur qui vient de finir
- Contexte : après un score, le premier match proposé peut concerner un joueur qui sort de table (capture j04 : Hugo MORIN rejoue aussitôt). L'écran suit l'ordonnanceur sans le modifier (B025 traite les règles de priorité et de repos).
- Article FFTT : aucun temps de repos minimal trouvé dans les textes fournis pour les tournois individuels ; question déjà ouverte (« temps de repos »).
- Options : A) garder l'ordre de l'ordonnanceur ; B) proposer en premier un match dont aucun joueur ne sort de table.
- Choix provisoire de l'agent : A (aucun changement de règle, B025).
- Réponse de Quentin :

### 2026-11-05 — Table choisie par « Table N »
- Contexte : l'envoi en un clic prend la plus petite table libre.
- Article FFTT : aucun (organisation de la salle).
- Options : A) plus petite table libre ; B) tables réservées par tableau (zone de la salle) ; C) table la plus proche de la table d'arbitrage.
- Choix provisoire de l'agent : A ; les autres tables restent accessibles par « Autre table… » ou glisser-déposer.
- Réponse de Quentin :

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

### 2026-11-05 — Liste « Mes prochains tournois » sur l'accueil
- Contexte : une liste de tournois avec une pastille d'état (inscrit, liste d'attente…) était sur l'accueil. Elle a été retirée de l'accueil ; seules les dates restent, sur la carte Agenda.
- Article FFTT : aucun (interface).
- Options : A) garder la liste avec pastilles sur l'accueil ; B) dates seulement sur la carte Agenda (choix actuel).
- Choix provisoire de l'agent : B (accueil plus court, pas de doublon avec l'Agenda).
- Réponse de Quentin :

### 2026-11-05 — Bandeau bleu doux pour la section club
- Contexte : la section « Gestion de votre club » est dans un bandeau bleu doux encadré, titre plus grand, tuile bleu plein. Charte v2, décision de design.
- Article FFTT : aucun (interface).
- Options : A) bandeau bleu doux (choix actuel) ; B) bloc gris neutre comme les autres cartes ; C) simple titre sans cadre.
- Choix provisoire de l'agent : A, pour bien la séparer des cartes personnelles.
- Réponse de Quentin :

### 2026-11-05 — Entrée « Agenda » provisoire dans la coquille
- Contexte : la carte Agenda mène à une page « à venir » en attendant la tâche B032. Cette entrée de menu est provisoire et sera remplacée par B032.
- Article FFTT : aucun (interface).
- Options : A) garder la page provisoire jusqu'à B032 (choix actuel) ; B) masquer l'entrée de menu tant que B032 n'est pas fusionnée.
- Choix provisoire de l'agent : A, réversible.
- Réponse de Quentin :

### 2026-11-05 — Liste d'attente dans l'agenda
- Contexte : un joueur qui n'est qu'en liste d'attente sur un tableau est-il
  inscrit au tournoi pour son agenda ?
- Article FFTT : aucun (choix d'interface, pas une règle sportive).
- Options : A) oui, la date reste dans l'agenda avec la mention « Liste d'attente » ;
  B) non, la date n'apparaît qu'après une inscription confirmée.
- Choix provisoire de l'agent : A (option prudente : le joueur ne rate pas la date,
  la mention évite la confusion ; réversible en une ligne).
- Réponse de Quentin :

### 2026-11-05 — Délai de désinscription en ligne
- Contexte : jusqu'à quand un joueur peut-il se désinscrire lui-même en ligne ?
- Article FFTT : non vérifié dans `docs/reglements/` pour cette tâche (à confirmer avant de fixer une règle).
- Options : A) tant que le tableau est en phase d'inscription, sans date limite
  ni remboursement ; B) jusqu'à une date limite fixée par l'organisateur.
- Choix provisoire de l'agent : A (aucun délai inventé ; la désinscription n'est
  possible qu'en phase d'inscription, le reste relève de l'organisateur).
- Réponse de Quentin :

### 2026-11-05 — Retrait manuel d'un tournoi de l'agenda
- Contexte : un joueur peut-il retirer lui-même une date de tournoi de son agenda ?
- Article FFTT : aucun (choix d'interface).
- Options : A) non, la date suit l'inscription (elle se retire seulement à la
  désinscription) ; B) oui, le joueur peut la masquer à la main.
- Choix provisoire de l'agent : A (une date automatique ne se modifie pas à la main,
  pour éviter un agenda qui ne correspond plus aux inscriptions).
- Réponse de Quentin :

### 2026-11-05 — Brouillon ou demande dont la date est passée
- Contexte : un brouillon, ou une demande d'homologation, dont le dernier jour est passé reste aujourd'hui dans la frise, et n'entre pas dans l'historique (l'historique ne reprend que les tournois annulés et les tournois homologués ou publiés).
- Article FFTT : aucun (choix d'affichage).
- Options : A) rester dans la frise, pour qu'on voie qu'il faut le traiter (choix actuel) ; B) basculer dans l'historique dès que la date est passée.
- Choix provisoire de l'agent : A (on ne fait pas disparaître un tournoi non traité).
- Réponse de Quentin : en attente.

### 2026-11-05 — Tournoi actuel sans tournoi en cours
- Contexte : le cahier des charges (section 4) dit « vide ou le tournoi en cours ». Sans tournoi en cours ni publié aujourd'hui, la carte affiche aujourd'hui le prochain tournoi publié (« Prochain tournoi »).
- Article FFTT : aucun (choix d'affichage).
- Options : A) afficher le prochain tournoi publié (choix actuel) ; B) laisser la carte vide tant qu'aucun tournoi n'est en cours.
- Choix provisoire de l'agent : A (la carte reste utile en dehors du jour J).
- Réponse de Quentin : en attente.

### 2026-11-05 — Juge-arbitre après la demande d'homologation
- Contexte : le juge-arbitre fait partie du dossier (IX.104). Après la demande, on ne peut plus ajouter, retirer ni changer le nom, le grade ou le rôle d'un JA. Sa licence, son email et son téléphone restent modifiables (à Brunoy, les licences sont encore « à renseigner »). Un JA malade ne peut donc être remplacé qu'en retirant la demande.
- Article FFTT : IX.104 (grade exigé) ; le règlement ne dit rien du changement de JA après homologation.
- Options : A) tout verrouiller sauf licence et contact (choix actuel) ; B) laisser changer le JA si le grade reste conforme.
- Choix provisoire de l'agent : A, le plus prudent.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Tableaux après la demande d'homologation
- Contexte : ajouter ou retirer un tableau, ou changer son nom, ses points, son public ou son horaire, est refusé dès la demande. Format, places, tarif, récompenses et heures de pointage restent libres.
- Article FFTT : aucun (choix de l'application, la grille 🔴 sert de référence).
- Options : A) verrouillage complet de ces champs (choix actuel) ; B) permettre un décalage d'horaire le même jour après homologation.
- Choix provisoire de l'agent : A.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Tournoi annulé
- Contexte : comme pour le règlement figé (B010), un tournoi annulé n'est plus verrouillé.
- Article FFTT : aucun.
- Options : A) garder les verrous après annulation ; B) déverrouiller (choix actuel, cohérent avec B010).
- Choix provisoire de l'agent : B.
- Réponse de Quentin : à compléter.

### 2026-11-05 — Seuils des couleurs de priorité
- Contexte : rouge à partir de 20 min d'attente (ou match important qui attend 10 min), orange à partir de 10 min, pour un match important, ou pour les premiers proposés (autant que de tables libres).
- Article FFTT : aucun ; le juge-arbitre programme les parties (3.3.1.2.2, IX.109). Pas de temps de repos minimal dans les textes fournis.
- Options : A) seuils fixes 10 / 20 min ; B) seuils réglables par tournoi ; C) seuils calculés sur la durée moyenne d'une partie (`EstimateurDuree`).
- Choix provisoire de l'agent : A (constantes `ReglePrioriteSalle.ATTENTE_MOYENNE` / `ATTENTE_LONGUE`).
- Réponse de Quentin :

### 2026-11-05 — « Stratégie d'ordonnancement choisie »
- Contexte : le cahier des charges parle d'une stratégie choisie, mais le logiciel n'a pas encore de choix de stratégie (B025). Aujourd'hui, « orange : prochain à envoyer » = les premiers matchs du classement de l'ordonnanceur, autant que de tables libres. En début de tournoi (beaucoup de tables libres), presque tout est orange.
- Article FFTT : aucun (organisation).
- Options : A) garder ce critère ; B) seulement le premier proposé ; C) attendre B025 pour brancher la stratégie choisie.
- Choix provisoire de l'agent : A, à revoir avec B025.
- Réponse de Quentin :

### 2026-11-05 — « Terminer la poule »
- Contexte : chaque partie d'une poule se lance sur une table séparément ; « Terminer la poule » est affiché sur la dernière partie restante de la poule (sinon « Terminer la rencontre »).
- Article FFTT : aucun.
- Options : A) comme ça ; B) envoyer une poule entière sur une table et la terminer d'un coup (saisie de toutes les parties).
- Choix provisoire de l'agent : A (réversible).
- Réponse de Quentin :

### 2026-11-05 — Joueurs qualifiés en attente du tableau final
- Contexte : un joueur qui a fini sa poule et attend la construction du tableau final n'a pas encore de match : il n'apparaît pas dans « Joueurs en attente ».
- Options : A) ne pas l'afficher ; B) l'afficher avec « attend le tableau final ».
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-05 — Ordre des rencontres de poule (I.301)
- Article : I.301 (ex. poule de trois, 3 tours : « 1-3 / 2-3 / 1-2 », variantes selon les qualifiés) ; texte donnant l'ordre des tours, pas de consigne sur la simultanéité.
- Comportement actuel : la salle propose les parties prêtes dans n'importe quel ordre, avec priorité de couleur ; l'ordre I.301 n'est pas rappelé ni contraint dans cet écran.
- Options : A) ne rien faire (le juge-arbitre juge) ; B) afficher un avertissement non bloquant si une partie est lancée avant celle qui la précède dans I.301.
- Réponse de Quentin :

### 2026-11-06 — Adoucir partout l'aplat ambre des rappels ?
- Contexte : `.rappel-attention` a été adouci dans ces écrans ; faut-il le faire partout ?
- Options : A) oui, partout B) seulement ici
- Choix provisoire de l'agent : B (réversible)
- Réponse de Quentin :

### 2026-11-06 — Téléphone absent du compte
- Contexte : le cahier des charges v0 (section 2) demande le téléphone dans le bloc du haut. Le compte n'a pas de champ téléphone : la page affiche « Non renseigné ».
- Article FFTT : aucun.
- Options : A) ne rien changer, le téléphone reste « Non renseigné » ; B) ajouter le champ au compte (le format JSON change, lecture des anciens fichiers à garder, test `RoundTripTournoiTest` à vérifier).
- Choix provisoire de l'agent : A (pas de changement de format sans accord).
- Réponse de Quentin :

### 2026-11-06 — Liste des droits affichée dans le tableau du club
- Contexte : la liste vient du modèle actuel. Organisateur de tournois et voir l'équipe : pour tous les membres. Le référent ne peut pas se dissocier du club. La gestion des autres administrateurs est « à venir » pour le référent.
- Article FFTT : aucun (clause d'organisation du club, pas une règle fédérale).
- Options : A) garder cette liste telle quelle ; B) revoir les lignes (ajouter, retirer, renommer) avant fusion.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Appel micro « Bientôt » en attendant B018
- Contexte : le champ de prononciation phonétique n'a pas de saisie tant que B018 n'est pas livré. La page affiche « Bientôt ».
- Article FFTT : aucun.
- Options : A) garder « Bientôt » ; B) masquer la section jusqu'à B018.
- Choix provisoire de l'agent : A (la section reste visible, comme demandé par le cahier des charges).
- Réponse de Quentin :

### 2026-11-06 — Tournoi en cours mais non accessible
- Contexte : un tournoi qui se joue aujourd'hui, et auquel on ne peut pas accéder (par exemple réservé à un comité), doit-il s'afficher « En cours » ou « Non accessible » ?
- Article FFTT : accès au tournoi, IX.101 / IX.102 (déjà cités dans le code). Aucun article trouvé sur l'affichage : je ne devine pas.
- Options : A) « En cours » d'abord, quelle que soit l'accessibilité (appliqué) ; B) « Non accessible » d'abord, « En cours » seulement si l'on peut participer.
- Choix provisoire de l'agent : A (l'information « le tournoi se joue maintenant » est la plus utile le jour même ; le motif reste dans la fiche).
- Réponse de Quentin :

### 2026-11-06 — Pastille « Complet » avec liste d'attente
- Contexte : tous les tableaux ouverts à son niveau sont pleins, mais une liste d'attente existe. Faut-il afficher « Complet » ou « À venir » ?
- Article FFTT : non trouvé dans le code ; à confirmer dans les textes 2026-2027 (liste d'attente).
- Options : A) « Complet » dès que les tableaux à son niveau sont pleins, avec la mention « liste d'attente éventuelle » (appliqué) ; B) « À venir » tant qu'une place d'attente reste possible.
- Choix provisoire de l'agent : A (la pastille dit tout de suite que l'inscription directe est fermée ; la liste d'attente est signalée dans le motif).
- Réponse de Quentin :

### 2026-11-06 — Victoires et défaites par abandon ou forfait
- Contexte : un match gagné ou perdu parce que l'adversaire a abandonné, refusé de jouer ou été disqualifié. Faut-il le compter dans mes victoires et défaites ?
- Article FFTT : abandon, III.109 (texte lu dans `docs/reglements/reglements_sportifs_2026_2027.txt`). Les mentions de « forfait » du texte visent surtout les rencontres par équipes ; je n'ai pas trouvé de règle de forfait pour un tournoi individuel. Je ne devine pas.
- Options : A) compté dans victoires ou défaites, et relevé à part (« dont forfaits et abandons ») pour être signalé (appliqué) ; B) exclu des totaux, seuls les matchs réellement joués comptent.
- Choix provisoire de l'agent : A. Le bilan reste fidèle au tableau, et le signalement permet de le lire tel quel. Passer en B revient à retirer ce compte dans `PerformancesJoueur`.
- Réponse de Quentin :

### 2026-11-06 — Tarif retenu pour les recettes attendues
- Contexte : un tableau peut avoir deux tarifs (en ligne / sur place), mais on ne sait pas encore comment chaque joueur paie.
- Article FFTT : aucun (clause d'organisateur).
- Options : A) tarif en ligne (sur place s'il n'y a pas d'en-ligne), comme le « montant à régler » déjà affiché au joueur ; B) tarif sur place ; C) selon le canal réel du joueur (à enregistrer).
- Choix provisoire de l'agent : A, pour rester cohérent avec le montant dû affiché.
- Réponse de Quentin :

### 2026-11-06 — Base de la dotation « % des inscriptions »
- Contexte : la part promise (ex. 50 % au vainqueur du tableau dames) se calcule sur quoi, avant le tournoi ?
- Article FFTT : aucun (règlement du tournoi : « part des droits d'engagement perçus »).
- Options : A) recettes attendues (tous les inscrits) ; B) seulement ce qui est encaissé ; C) les présents au pointage.
- Choix provisoire de l'agent : A (montant promis le plus élevé, donc solde le plus prudent).
- Réponse de Quentin :

### 2026-11-06 — Réduction de dotation (conditions de maintien) dans le solde
- Contexte : le règlement peut prévoir une dotation réduite sous un seuil de remplissage ou selon les absents.
- Article FFTT : aucun (IX.108 ne traite que des récompenses ; la réduction est une clause d'organisateur, `ConditionsMaintien`).
- Options : A) afficher la dotation pleine ; B) appliquer la réduction prévue d'après le remplissage actuel.
- Choix provisoire de l'agent : A (prudent : on ne compte pas sur une réduction qui n'est pas acquise).
- Réponse de Quentin :

### 2026-11-06 — Évolution des encaissements dans le temps
- Contexte : le cahier des charges parle de « données qui évoluent » ; le modèle ne garde pas la date d'un paiement (ni d'une inscription).
- Article FFTT : aucun.
- Options : A) ajouter une date au paiement (format de sauvegarde à étendre, rétrocompatible) pour tracer une courbe ; B) s'en tenir à l'état présent.
- Choix provisoire de l'agent : B pour l'instant (pas de changement du format de sauvegarde sans nécessité) ; la carte se recalcule à chaque ouverture.
- Réponse de Quentin :

### 2026-11-06 — Total arrondi une fois : écart possible de 0,01 € avec les lignes affichées
- Contexte : chaque ligne (tableau) est arrondie au centime, puis le total est la somme de ces lignes arrondies, arrondie une nouvelle fois. À la lecture du code, la somme affichée et le total concordent : l'écart de 0,01 € ne devrait pas apparaître, car chaque ligne est déjà un nombre de centimes. Il resterait possible seulement si l'affichage arrondissait autrement qu'au centime.
- Article FFTT : aucun.
- Options : A) accepter (recommandé : aucun écart attendu, pas de calcul en centimes entiers à refaire) ; B) passer tous les montants en centimes entiers pour supprimer tout doute (changement du cœur, sans effet visible prévu).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Tranché

### 2026-11-05 — Abandon ou disqualification en demi-finale, puis petite finale
- Contexte : `core/ServicePhaseFinale`, `ProgressionTableau`. Avec une petite
  finale, le perdant d'une demi-finale y est placé, quelle que soit l'issue de
  la demie : un joueur qui a déclaré forfait, abandonné (blessure) ou été
  disqualifié en demi-finale se retrouve en petite finale.
- Article FFTT : rien pour les tournois (chapitre IX). 3.5.3.1.1 autorise
  l'abandon pour maladie ou blessure ; 3.5.2.10 permet au juge-arbitre de
  disqualifier « pour le restant de la compétition ». III.109 (abandon :
  « considéré battu pour la partie ou les parties qui lui reste(nt) à
  disputer ») vise le Critérium fédéral.
- Options : A) le fautif est placé en petite finale ; le juge-arbitre y
  déclare ensuite son forfait (rien d'automatique) ; B) après un abandon, une
  disqualification ou un forfait en demie, la petite finale est donnée
  automatiquement à l'autre demi-finaliste battu ; C) B seulement pour la
  disqualification (3.5.2.10), A sinon.
- Choix provisoire de l'agent : A (aucune décision automatique, réversible,
  le juge-arbitre garde la main ; test
  `PetiteFinaleParTableauTest.petite_finale_attend_ses_deux_joueurs_avant_d_etre_jouable`).
- Réponse de Quentin :
