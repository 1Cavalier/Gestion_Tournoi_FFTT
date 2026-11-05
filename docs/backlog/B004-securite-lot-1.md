---
id: B004
titre: Sécurité, lot 1 : plus de compte ni de donnée réelle livrés
type: idee
priorite: P1
taille: M
statut: bloquee
depend-de: []
fichiers: [ui/infra/DonneesDemo, ui/ContexteApplication, core/ServiceAuthentification, ui/ecran/EcranConnexion]
branche:
pr:
---

## Ce qu'il faut faire

Appliquer le lot 1 de `RAPPORT_SECURITE.md` (S-01, S-02, S-03, S-12, S-16) : mode démo explicite, aucune identité réelle ni compte privilégié hors démo, vérification de l'email, politique de mot de passe. Inverser les tests `faille_S01` à `faille_S03`.

## Critères de fin

- [ ] aucun compte créé hors mode démo
- [ ] identités de démo fictives partout (code, tests, docs)
- [ ] `mvn -B test` à 0 échec

## Contraintes / décisions de Quentin

**Décision de Quentin attendue** (`QUESTIONS.md` : données de démo et supervision, preuve de licence, service d'envoi d'emails). Passer `a-faire` une fois répondu.

## Passations

## Résultat
