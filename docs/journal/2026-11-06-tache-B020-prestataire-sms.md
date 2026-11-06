# Choisir un service d'envoi de SMS (B020) — 2026-11-06

- Branche : `tache/B020-prestataire-sms`
- Agent : `chercheur-fftt`
- Pull Request : #55
- Statut : fusionné

## Pour Quentin

Comparatif de 5 services d'envoi de SMS dans `docs/RECHERCHE_SMS.md`. Recommandation : Brevo (environ 0,045 EUR HT le SMS, serveurs en UE), repli OVHcloud. Aucun code modifié. Le nom « PINGTOURNOI » est déconseillé comme expéditeur (nom d'un logiciel concurrent, et les règles AF2M exigent un expéditeur identifiant l'émetteur) : utiliser le nom du club ou `PINGMANAGER`.

## À tester à l'écran

Sans objet (recherche, aucun changement d'écran).

## Questions pour Quentin

### 2026-11-06 — Compte SMS du club ou de PingManager
- Contexte : qui crée le compte chez le prestataire et porte l'expéditeur.
- Article FFTT : sans objet
- Options : A) compte par club B) compte PingManager
- Choix provisoire de l'agent : A (option prudente) ; clé d'API dans `~/.pingmanager/sms.properties`, hors dépôt.
- Réponse de Quentin :

### 2026-11-06 — Confirmer le prix Brevo
- Contexte : prix Brevo et Octopush issus de sources secondaires.
- Options : A) confirmer sur le site du prestataire avant B019
- Choix provisoire de l'agent : A
- Réponse de Quentin :

## Notes pour l'état du projet

Recherche SMS faite (`docs/RECHERCHE_SMS.md`) ; esquisse d'adaptateur `java.net.http` en section 6 pour B019. Format de sauvegarde et démo inchangés.

## Fusion

OK, rien à signaler (verifier-fusion.py).

## Chiffres

- Tests : 1042 réussis, 0 échec (outils-dev/tests.sh ; Maven Central inaccessible, GitHub Actions fait foi)
- Version de la démo : inchangée
