---
id: B020
titre: Choisir un service d'envoi de SMS
type: recherche
priorite: P2
taille: S
statut: fait
depend-de: []
fichiers: [docs/RECHERCHE_SMS.md]
branche: tache/B020-prestataire-sms
pr:
---

## Ce qu'il faut faire

Comparer 3 à 5 services d'envoi de SMS utilisables depuis la France (prix par message, API HTTP simple, expéditeur personnalisé « PINGTOURNOI », conformité RGPD, hébergement des données, facturation à l'usage pour un club). Recommandation argumentée et liste de ce que Quentin doit faire (création de compte, clé d'API à garder hors du dépôt). Agent : `chercheur-fftt` (consigne : hors FFTT, comparatif de prestataires). Livrable `docs/RECHERCHE_SMS.md`.

## Critères de fin

- [x] comparatif sourcé et daté
- [x] recommandation
- [x] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

Aucune pour l'instant.

## Passations

- 2026-11-06 `chercheur-fftt` : `docs/RECHERCHE_SMS.md` écrit (5 prestataires comparés, 21 sources datées). Recommandation : Brevo (≈ 0,045 € HT/SMS, crédits sans expiration, API JSON à clé d'en-tête, serveurs UE), repli OVHcloud (0,060 €/SMS, http2sms).
- Alerte : « PINGTOURNOI » est le nom d'un logiciel concurrent (ping-tournoi.fr) et les règles AF2M de 2025-2026 exigent un expéditeur qui identifie l'émetteur : utiliser le nom du club ou `PINGMANAGER`. Prix Brevo et Octopush issus de sources secondaires, à confirmer.
- Questions pour Quentin : compte SMS du club ou de PingManager (option prudente : le club) ; clé à placer dans `~/.pingmanager/sms.properties`. Esquisse de l'adaptateur `java.net.http` pour B019 en section 6. Aucun code modifié, pas de test lancé.

## Résultat
