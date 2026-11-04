# Charte graphique PingManager — brief de départ

> Point de départ de la phase « design / charte » (rédigé le 13 octobre 2026).
> Application **bureau JavaFX**, package `com.pingmanager.ui`. Objectif : interface
> **pro, lisible, agréable** à utiliser, en particulier les écrans de saisie
> (création / complétion de tournoi). Thèmes **clair + sombre**.
>
> Depuis : `theme.css` a été écrit selon ce brief (variables sans préfixe
> `--`, par exemple `-primaire`) et la police Inter est embarquée. La référence
> à jour pour les agents est la skill `.claude/skills/charte-graphique`.

---

## 1. Palette

Couleurs sources fournies : `#55D5E0` (cyan), `#335F8A` (bleu moyen), `#2F4558`
(bleu ardoise foncé), `#F6B12D` (ambre). **Le bleu domine** ; l'ambre est un
accent **ponctuel** (attention / mise en avant), jamais une couleur de fond large.

Deux couleurs fonctionnelles sont ajoutées (hors palette de marque) car la saisie
en a besoin : un **vert succès** et un **rouge erreur**, choisis pour s'harmoniser
avec les bleus.

### Tokens sémantiques (à utiliser dans le code, jamais le hex brut)

| Token | Rôle | Clair | Sombre |
|---|---|---|---|
| `--fond-app` | fond général | `#F5F8FB` | `#17242E` |
| `--surface` | cartes, champs | `#FFFFFF` | `#22333F` |
| `--surface-2` | survol / zones secondaires | `#EEF3F8` | `#2B3E4B` |
| `--bordure` | traits, contours de champ | `#D4DEE7` | `#37505F` |
| `--texte` | texte principal | `#2F4558` | `#E8EFF4` |
| `--texte-doux` | texte secondaire / aide | `#5C6E7D` | `#A7B9C5` |
| `--primaire` | boutons, liens, focus | `#335F8A` | `#4C93C9` |
| `--primaire-fort` | survol/appui du primaire | `#294E73` | `#63A8D8` |
| `--primaire-doux` | fonds teintés, sélection | `#E3EDF5` | `#2C4A5F` |
| `--accent` | cyan, détails & sélection | `#55D5E0` | `#55D5E0` |
| `--entete-fond` | barre d'en-tête | `#2F4558` | `#111C24` |
| `--entete-texte` | texte sur en-tête | `#F5F8FB` | `#E8EFF4` |
| `--attention` | ambre — obligatoire manquant | `#F6B12D` | `#F6B12D` |
| `--succes` | renseigné / complet | `#2E8B6F` | `#49B894` |
| `--erreur` | erreur de saisie | `#C0453B` | `#E0756B` |

> Règle d'usage : fonds et structure en bleus/gris ; **primaire** pour les actions ;
> **accent cyan** pour la sélection et de petits détails ; **ambre** réservé aux
> éléments « obligatoire manquant » ; vert/rouge seulement pour succès/erreur.

---

## 2. Typographie

- Police : Inter (embarquée), repli `Segoe UI`, `system-ui`, sans-serif.
- Échelle : Titre page 20 · Section 15 (gras) · Corps 13–14 · Petit/aide 11–12.
- Graisses : normal 400, semi-gras 600 pour titres et libellés de champ.
- Interligne confortable sur les blocs d'aide ; libellés de champ au-dessus du contrôle.

---

## 3. Espacement, rayons, élévation

- Échelle d'espacement : **4 / 8 / 12 / 16 / 24 / 32**.
- Rayons : cartes **10**, champs & boutons **8**, badges **6**.
- Élévation : pas d'ombres lourdes ; cartes = surface + bordure fine (1px `--bordure`).
- Densité des formulaires : **confortable** (vgap 10–12, champs hauteur ~32–36).

---

## 4. Inventaire des composants à styliser (classes CSS proposées)

- Structure : `.app`, `.entete`, `.nav`, `.nav-groupe-titre`, `.nav-item`, `.nav-item-actif`.
- Contenu : `.carte`, `.carte-titre`, `.titre`, `.sous-titre`, `.texte`, `.discret`, `.badge`.
- Saisie : `.champ` (TextField), `.combo` (ComboBox), `.case` (CheckBox), `.zone-texte` (TextArea).
- Boutons : `.bouton-primaire`, `.bouton-secondaire`, `.bouton-lien`.
- États complétude : `.etat-ok` (vert ✓), `.etat-attention` (ambre, obligatoire manquant),
  `.etat-recommande` (bleu/doux), `.etat-optionnel` (gris).
- États d'interaction : `:focused` (anneau `--primaire`), `.en-erreur` (bordure `--erreur`),
  `:disabled` (opacité réduite, pas d'ombre).

---

## 5. Approche technique

1. **Un seul `theme.css`** dans `src/main/resources/com/pingmanager/ui/theme.css`,
   appliqué à la scène.
2. **Tokens via couleurs nommées JavaFX** : variables définies sur `.root.theme-clair`
   et `.root.theme-sombre`, puis n'utiliser que ces variables dans les règles.
   Bascule clair/sombre = changer la classe du root (`ui.ecran.Theme`).
3. **Pas de styles *inline*** (`-fx-...` dans le Java) : des **classes de style**.
   `Ui.java` est le point central qui pose les `styleClass`.
4. Bascule de thème : bouton clair/sombre dans l'en-tête.
5. JavaFX ne se lance pas visuellement en session cloud → **validation visuelle
   par captures** côté machine de Quentin, par petits lots d'écrans.
