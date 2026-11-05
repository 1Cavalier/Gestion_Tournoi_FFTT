# Consignes à lancer (nuit du 5 au 6 novembre 2026)

Une consigne = une nouvelle session sur claude.ai/code (bouton « Créer »,
dépôt `Gestion_Tournoi_FFTT`). Les quatre « couloirs » touchent des fichiers
différents et peuvent tourner en même temps. **Ordre conseillé si tu en lances
moins** : A, puis C, puis B, puis D. Avec l'abonnement Pro, deux ou trois
sessions longues en parallèle atteignent vite la limite : une session arrêtée
se relance en lui écrivant « continue ».

---

## A — Jour J (B037, B038, B039)

```
Avance le projet en suivant la skill piloter-projet, mais uniquement sur le couloir « Jour J » : prends dans l'ordre B037 (jour J v2 : priorités en couleur, joueurs en attente, salle, lancer, terminer, annonce), puis B038 (saisie du score en grille et cartons), puis B039 (page du tournoi le jour J avec onglets, pointage sans croix « payé »). Référence : docs/CAHIER_DES_CHARGES_V0.md, section 7 et 8. Consigne contraire au budget : enchaîne ces trois tâches tant que tu le peux, chacune jusqu'à sa fusion. Ne prends aucune autre tâche.
```

## B — Tournois côté joueur (B032, B030, B031)

```
Avance le projet en suivant la skill piloter-projet, mais uniquement sur le couloir « Tournois côté joueur » : prends dans l'ordre B032 (agenda personnel et export .ics), puis B030 (carte des tournois : vue carte avec « Voir plus », vue agenda par jour, statuts visibles), puis B031 (mes tournois : tournoi en cours, inscriptions et agenda, tournois passés). Référence : docs/CAHIER_DES_CHARGES_V0.md, sections 1 et 3. Consigne contraire au budget : enchaîne ces trois tâches tant que tu le peux, chacune jusqu'à sa fusion. Ne prends aucune autre tâche.
```

## C — Gestion du club et cycle du tournoi (B034, B033, B035, B036)

```
Avance le projet en suivant la skill piloter-projet, mais uniquement sur le couloir « Gestion du club » : prends dans l'ordre B034 (nouveau tournoi : texte explicatif et calendrier), B033 (gestion des tournois : tournoi actuel, frise verticale, gros bouton de création), B035 (homologation : verrous, refus, deuxième version de l'écran du tournoi), puis B036 (remplissage et finances). Référence : docs/CAHIER_DES_CHARGES_V0.md, sections 4, 5 et 6. Consigne contraire au budget : enchaîne ces tâches tant que tu le peux, chacune jusqu'à sa fusion. Ne prends aucune autre tâche.
```

## D — Accueil et profil (B028, B029, puis B012)

```
Avance le projet en suivant la skill piloter-projet, mais uniquement sur le couloir « Accueil et profil » : prends dans l'ordre B028 (accueil : point d'entrée qui explique chaque section, section séparée « Gestion de votre club »), puis B029 (mon profil : bloc personnel et licence, bloc club avec avertissement et tableau des droits), puis, s'il reste du temps, B012 (rendu v2 des formulaires de préparation). Référence : docs/CAHIER_DES_CHARGES_V0.md, sections 1 et 2. Consigne contraire au budget : enchaîne ces tâches tant que tu le peux, chacune jusqu'à sa fusion. Ne prends aucune autre tâche.
```

---

## Au réveil

`outils-dev\mettre-a-jour.cmd` (récupère, résume chaque tâche, lance
l'application), puis une session : « Utilise l'agent recapitulateur et fais le
point. » (une session planifiée le fait aussi à 7 h 40).

## Après la v0 (planifié)

- Plan de salle (B024), stratégies et prévisions (B025), feuilles de match
  (B016), annonces vocales (B017, B018), SMS (B019, B020), suivi sur téléphone
  (B021), FFTT (B022, B023), conformité (B009, B040), puis finitions (B005,
  B014).
- Qualité : audit d'architecture, tests manquants, optimisation, nouvel audit
  de sécurité — sessions planifiées le week-end (voir les tâches planifiées).
