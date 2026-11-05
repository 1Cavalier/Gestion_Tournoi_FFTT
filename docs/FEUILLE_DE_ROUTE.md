# Feuille de route jusqu'à la version 1 (5 novembre 2026)

Le détail de chaque tâche est dans `docs/backlog/B….md`. Les agents les
prennent dans l'ordre des priorités (skill `piloter-projet`) ; deux tâches qui
touchent les mêmes fichiers ne tournent jamais en même temps.

## Vague 1 — Finir l'essentiel (P1)

| Tâche | Quoi |
|---|---|
| B001 | Correctif technique (switch sur énumération) |
| B010 | Règlement en lecture seule après publication, réduit à un onglet |
| B011 | Jour J plus simple : un écran unique avec matchs proposés, tables et avancement des tableaux |
| B012 | Vrai rendu v2 de la préparation du tournoi |
| B022 | Recherche approfondie sur l'échange avec la FFTT (joueurs, clubs, résultats) |
| B004 | Sécurité, lot 1 — **attend tes réponses** dans `QUESTIONS.md` |

## Vague 2 — Le jour J complet et le rendu partout (P2)

- Rendu v2 : B013 (onglet Tournois, inscriptions), B005 (jour J, après B011), B014 (finitions et cohérence).
- Salle : B015 (couleur par tableau), B016 (feuilles de match, de poule, tableau final imprimables), B024 (plusieurs salles et plan des tables, après B011).
- Appels : B017 (annonces vocales), B018 (prononciation phonétique dans le profil, après B017), B019 (convocations par SMS, en simulation), B020 (choix du service SMS).
- Public : B021 (suivi sur téléphone par le Wi-Fi de la salle).
- Veille du tournoi : B006 (tableau de bord J-1), B007 (listes de pointage), B008 (mise à jour des points).
- Grands volumes : B025 (simulation de gros tournois, stratégies d'ordonnancement et prévisions).
- FFTT : B023 (ports et échanges par fichier, après B022).
- Qualité : B002, B003, B009.

## Vague 3 — Après

- B026 : buvette — **attend ton cahier des charges**.
- Lots 2 à 5 de sécurité (`RAPPORT_SECURITE.md`), paiement HelloAsso, version web.

## Tes décisions qui débloquent du travail

1. Sécurité (`QUESTIONS.md`) : données de démo et supervision, preuve de licence, service d'emails → débloque B004.
2. Service SMS (après B020) → envoi réel des convocations.
3. QR code pour le suivi sur téléphone : bibliothèque (nouvelle dépendance) ou encodeur maison.
4. Buvette : cahier des charges → débloque B026.
5. FFTT : envoyer le courriel de `docs/RECHERCHE_FFTT.md` (section 5) → accès à l'API.
