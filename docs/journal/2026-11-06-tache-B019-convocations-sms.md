# Convocations par SMS (B019) — 2026-11-06

- Branche : `tache/B019-convocations-sms`
- Agent : `developpeur` (étape de pipeline ; l'agent `developpeur-interface` passe ensuite)
- Pull Request : à créer par l'`integrateur`
- Statut : en cours (étape de pipeline, non fusionné)

## Pour Quentin

Le club peut prévenir les joueurs et les arbitres par SMS (jamais d'appel).
Le numéro de portable est facultatif : on ne l'enregistre que si la personne
a donné son accord, et il est effacé le lendemain du dernier jour du tournoi.
Quand un match part sur une table, les deux joueurs reçoivent un SMS si
l'annonce choisie comprend le SMS et si le club a coché l'envoi automatique.
On peut aussi envoyer à la main. Pour l'instant **aucun SMS ne part
vraiment** : les messages s'affichent à l'écran (simulation).

## À tester à l'écran

- Compte de démo : celui de Brunoy.
- Chemin : Gestion des tournois → un tournoi → Ouvrir le jour J → bouton « SMS… ».
- À regarder (thème clair et sombre) : réglages (envoi automatique,
  expéditeur, limite), saisie d'un numéro sans puis avec la case d'accord,
  « Retirer », « Effacer tous les numéros » (deux clics), puis lancer un match
  avec l'annonce « SMS » : message « SMS : 1 SMS envoyé… », journal et SMS
  simulés en bas de l'écran « SMS… ». Écran non vérifié visuellement (session
  cloud).

## Ce qui a été fait

- Cœur : `TexteSms` (alphabet GSM 7 bits, longueur, nombre de SMS, lettres
  françaises hors alphabet remplacées : ê → e, ç → c, œ → oe), `TexteConvocation`
  (poule, tour de tableau, classement, arbitrage ; toujours un seul SMS de
  160 caractères : club retiré puis noms abrégés si besoin), `ContexteConvocation`,
  `NumeroPortable`, `ExpediteurSms` (charte AF2M, nom du club, repli
  `PINGMANAGER`, jamais `PINGTOURNOI`), port `EnvoiSms` + `ResultatEnvoiSms`,
  `ServiceConvocationsSms` (consentement, limite, journal, effacement).
- Modèle : `ConvocationsSms` (sur `Tournoi`), `ContactSms`, `MessageSms`,
  `TypeConvocation`, `StatutEnvoiSms`. Le journal ne contient pas le numéro.
- Persistance : `ConvocationsSmsMapper`, champ facultatif `convocationsSms`.
- Interface : `ui.infra.EnvoiSmsSimule`, `ContexteApplication.convocationsSms()`
  / `smsSimule()`, `EcranSalle.avecSms(…)` (envoi au lancement, bouton « SMS… »),
  `EcranConvocationsSms`, `PresentationConvocationsSms`, branchement dans `EcranJourJ`.

- Interface (`developpeur-interface`, 2026-11-08) :
  - saisie facultative du portable avec **case d'accord jamais cochée d'avance**
    à l'inscription en ligne (étape « Récapitulatif » de `AssistantInscription`,
    numéro gardé juste après l'inscription et tournoi enregistré aussitôt) et par
    le club (Inscriptions → « Par joueur » → choisir un joueur → carte
    « Convocations par SMS », boutons « Enregistrer le numéro » / « Retirer le
    numéro », enregistrement immédiat). Bloc commun `BlocSaisieSms`, textes dans
    `PresentationSaisieSms` (testée) ;
  - écran « SMS… » fini : tuiles (envoyés / limite, non envoyés, numéros),
    alerte de limite proche ou atteinte, réglages sur deux colonnes, effacement
    manuel avec question explicite et « Annuler », journal lisible (destinataire,
    heure, type, pastille de statut, motif d'échec, 50 derniers), SMS simulés en
    bulles avec longueur et nombre de SMS ; logique dans
    `PresentationConvocationsSms` (testée) ;
  - cœur, ajouts sans changement de comportement : `ServiceConvocationsSms.refusSaisieFacultative`
    (contrôle avant inscription) et `dateEffacementAutomatique` (rappel affiché), testés ;
  - CSS : section « Convocations par SMS (B019) » en fin de `theme.css`
    (`sms-consentement`, `sms-bulle`, `sms-journal-ligne`, `sms-alerte-limite`…).
  - Captures sous Xvfb (non committées) : écran SMS clair et sombre, bloc de saisie.

## À tester à l'écran (ajout interface)

- Inscription en ligne (compte joueur) : étape « Récapitulatif », saisir un numéro
  sans cocher → refus avec motif ; laisser vide → inscription normale ; cocher →
  ligne « Convocations par SMS : numéro enregistré (…) » à la confirmation.
- Club : Inscriptions → « Par joueur » → cliquer un joueur → carte sous la liste.

## Questions pour Quentin

### 2026-11-06 — Envoi automatique : une case en plus de l'annonce « SMS » ?
- Contexte : B037 pose le choix « rien / SMS / appel vocal / les deux ». J'ai ajouté une case « envoi automatique » (décochée par défaut) : le SMS ne part au lancement que si l'annonce comprend le SMS **et** si la case est cochée.
- Article FFTT : sans objet.
- Options : A) garder la double condition (prudent, aucun envoi surprise) ; B) l'annonce « SMS » suffit.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Limite d'envois par défaut
- Contexte : limite par tournoi, 300 SMS par défaut (environ 13,50 € HT chez Brevo), réglable de 0 à 5 000.
- Options : A) 300 ; B) autre valeur ; C) limite aussi par personne.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-06 — Moment de l'effacement des numéros
- Contexte : il n'existe pas d'état « tournoi terminé ». Les numéros sont effacés à l'ouverture du jour J ou de l'écran SMS le lendemain du dernier jour, et le club peut les effacer à la main. Si personne n'ouvre le tournoi, ils restent dans le fichier.
- Options : A) comme ça ; B) effacer aussi au démarrage du logiciel pour tous les tournois ; C) effacer à la clôture d'un tableau final.
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

### 2026-11-08 — Où le club saisit-il un numéro ?
- Contexte : il n'existe pas de formulaire « ajouter un joueur » côté club (seulement l'import). La saisie club est donc dans Inscriptions → « Par joueur » (joueur choisi) et dans l'écran « SMS… » du jour J. L'import de tableur ne lit pas de colonne « portable ».
- Options : A) comme ça ; B) lire aussi une colonne « portable + accord » à l'import.
- Choix provisoire de l'agent : A (un accord explicite ne se déduit pas d'un tableur).
- Réponse de Quentin :

### 2026-11-06 — Lettres accentuées dans les SMS
- Contexte : ê, â, î, ô, û, ë, ï, ç, œ font passer un SMS à 70 caractères (Unicode). Elles sont remplacées (« vous etes attendu(e) », « Jérome ») ; é, è, à, ù restent.
- Options : A) remplacer (un seul SMS, moins cher) ; B) garder l'Unicode (2 à 3 SMS par convocation).
- Choix provisoire de l'agent : A.
- Réponse de Quentin :

## Notes pour l'état du projet

- B019 : convocations par SMS simulées. Classes : `core.ServiceConvocationsSms`,
  `core.TexteConvocation`, `core.TexteSms`, port `core.EnvoiSms`,
  `model.ConvocationsSms`, `ui.infra.EnvoiSmsSimule`, `ui.ecran.EcranConvocationsSms`.
- Format de sauvegarde : champ facultatif `convocationsSms` du tournoi, écrit
  seulement si le club s'en sert (anciens fichiers relus sans changement).
- Démo : inchangée.

## Pour l'agent suivant

- `developpeur-interface` : `EcranConvocationsSms` n'a aucune classe CSS
  propre (cartes `Ui` standard) ; la saisie du numéro à l'inscription
  (formulaire d'inscription, en ligne ou par le club) n'est **pas** faite :
  le numéro se saisit seulement dans l'écran SMS. Le texte d'aide de la question
  d'annonce (`EcranSalle.choixAnnonce`) a été ajusté.
- Les matchs de classement ne sont pas encore modélisés : `ContexteConvocation.classement(…)`
  existe et il est testé, mais `ContexteConvocation.de(…)` ne les reconnaît pas encore.
- Vrai prestataire (Brevo) : implémenter `EnvoiSms` hors du cœur et remplacer
  `EnvoiSmsSimule` dans `ContexteApplication` (clé d'API hors du dépôt).

## Fusion

À faire par l'`integrateur`.

## Chiffres

- Tests : 1046 → 1100 (mesurés par `outils-dev/tests.sh`, Maven Central inaccessible)
- Version de la démo : inchangée
