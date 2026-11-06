# Recherche : choisir un service d'envoi de SMS (tâche B020)

> Veille du **6 novembre 2026**, faite par l'agent `chercheur-fftt` (hors FFTT).
> Objet : choisir le prestataire qui portera les convocations par SMS de la
> tâche B019 (port `EnvoiSms`). Seules des pages publiques ont été lues ; aucun
> compte n'a été créé. Chaque chiffre renvoie à une source numérotée `[n]`
> (section 7). **[O]** = page officielle du prestataire, **[S]** = source
> secondaire (comparateur, blog), **[?]** = non trouvé. Notation combinée :
> `[O1]` = source n° 1, officielle ; `[S5]` = source n° 5, secondaire.
>
> Limite : plusieurs pages officielles de tarifs (Octopush, Brevo) ne sont pas
> joignables depuis la session. Leurs prix viennent donc de sources
> secondaires datées. Il faut les vérifier sur le site avant de payer.

---

## 1. Résumé

1. Pour un club, le bon modèle est le **crédit prépayé sans abonnement** : on
   achète quelques centaines de SMS avant un tournoi.
2. **Recommandation : Brevo**. Environ **0,045 € HT par SMS** vers la France
   [S5], crédits **sans date d'expiration**, achat possible avec l'offre
   gratuite [S5]. API JSON simple (une requête POST, une clé dans un en-tête)
   [O6]. Serveurs dans l'UE [O7].
3. **Solution de repli : OVHcloud**. 0,060 € HT par SMS par pack de 100
   [O1]. Société française. Envoi possible par une simple URL (`http2sms`)
   [O3], mais avec le mot de passe dans l'URL et un filtrage par adresse IP.
4. **Attention au nom d'expéditeur « PINGTOURNOI »** : c'est le nom d'un
   **logiciel concurrent** (ping-tournoi.fr, voir `docs/RECHERCHE_FFTT.md`,
   L3). Depuis 2025-2026, les opérateurs français bloquent ou remplacent les
   expéditeurs qui n'identifient pas clairement l'émetteur, et protègent les
   marques [S11][S12]. Il est conseillé de prendre **le nom du club** (par
   exemple `TTBRUNOY`) ou `PINGMANAGER` (11 caractères, la limite).
5. Les convocations sont des **SMS transactionnels** (non commerciaux) : pas
   de mention STOP obligatoire et pas de plage horaire interdite [O2]. Le
   consentement RGPD et l'effacement du numéro restent à faire dans B019.

---

## 2. Tableau comparatif (France métropolitaine, prix HT)

| Critère | **Brevo** | **OVHcloud SMS** | **SMSFactor** | **Octopush** | **Twilio** |
|---|---|---|---|---|---|
| Prix par SMS, petit volume | ≈ 0,045 € (4,5 crédits à 0,01 €) [S5] | 0,060 € (pack de 100) · 0,058 € (pack de 1 000) [O1] | 0,066 € (pack de 1 000, le plus petit) [O4] | ≈ 0,045 € **avec abonnement** de 9 €/mois [S9]. Prix officiel non trouvé [?] | 0,0798 $ par segment, plus d'éventuels frais d'opérateur non chiffrés [O13] |
| Coût de 1 000 SMS | ≈ 45 € [S5] | 58 € [O1] | 66 € [O4] | Inconnu sans abonnement [?] | ≈ 80 $ + frais [O13] |
| Plus petit achat | lot de 100 crédits (≈ 22 SMS) [S5] | 100 SMS pour 6 € [O1] | 1 000 SMS pour 66 € [O4] | [?] | à l'usage [O13] |
| Facturation | crédits prépayés, **sans expiration**, possible avec l'offre gratuite [S5] | packs prépayés, recharge automatique possible (prélèvement SEPA) [O1][O15]. Validité des crédits non trouvée [?] | packs prépayés **valables 1 an**, ou mensuel à partir de 3 000 SMS/mois [O4] | abonnements mensuels [S9]. La page officielle annonce aussi un plan à 65 €/mois [O8] (offres contradictoires) | à l'usage [O13] |
| API | `POST https://api.brevo.com/v3/transactionalSMS/send`, JSON, en-tête `api-key` [O6] | `GET https://www.ovh.com/cgi-bin/sms/http2sms.cgi?account=…&login=…&password=…&from=…&to=…&message=…` [O3]. L'API complète OVH n'a pas été étudiée | API documentée [S10]. Détail non lu [?] | `POST https://api.octopush.com/v1/public/sms-campaign/send`, JSON, en-têtes `api-login` et `api-key` [O14] | API REST connue. Détail non lu pour cette étude |
| Expéditeur alphanumérique | jusqu'à 11 caractères [O6]. Accents et caractères interdits convertis ou retirés automatiquement [O16] | 11 caractères maximum, lettres et chiffres, **validé par OVHcloud sous 72 h** sur justificatif [O2] | non mentionné sur la page des tarifs [?] | 3 à 11 caractères, si le compte l'autorise [O14]. Pas d'expéditeur personnalisé en « low cost » [S9] | gratuit [O13]. **Lettre d'autorisation (LOA) AF2M obligatoire** pour la France [O17] |
| Hébergement des données | « serveurs situés dans l'UE » [O7] | non trouvé pour le service SMS [?] | « data centers européens » [O18] | « hébergées en France », ISO 27001 [O8] | non vérifié (société américaine) [?] |
| RGPD | conformité affirmée [O7] | non étudié [?] | DPO désigné, registre des sous-traitants, messages conservés 6 mois par défaut [O18] | conformité affirmée [O8] | non étudié [?] |
| STOP pour les SMS non commerciaux | `type: transactional`. Le message passe en « marketing » s'il contient un code STOP [O6] | paramètre `noStop=1` [O3] | [?] | champ `purpose: alert` [O14] | — |
| Accusés de réception | webhook `webUrl` [O6] | identifiants de SMS renvoyés [O3] | [?] | [?] | oui (non étudié) |

Non retenus : **CM.com** (la page des tarifs n'affiche pas le prix pour la
France [O19]) et **smsmode** (prix pour la France non affiché, « contactez-nous »
[O20]).

---

## 3. Règles françaises utiles pour PingManager

- **Expéditeur (OADC)** : depuis le 10 mars 2025, la charte de l'AF2M
  (association des opérateurs et agrégateurs) impose : 11 caractères au plus,
  lettres et chiffres, pas uniquement des chiffres, pas de termes génériques
  (ALERT, RDV…). Les noms d'institutions et de banques sont interdits. Les
  marques demandent l'autorisation du titulaire [S11]. Depuis le 1er mars 2026,
  le contrôle est plus strict. L'expéditeur doit « clairement identifier »
  l'émetteur, sinon il est rejeté ou remplacé, et des pénalités sont
  possibles [S12]. → `PINGTOURNOI` = marque d'un tiers (risque de blocage et de
  confusion). Mieux vaut le **nom du club**, ce qui identifie aussi l'émetteur
  réel aux yeux du joueur.
- **Transactionnel ou commercial** : un SMS commercial demande un
  consentement préalable, une mention de désinscription, et ne part pas entre
  20 h et 8 h, ni le dimanche, ni un jour férié. Un SMS transactionnel n'est
  soumis ni à la mention STOP ni aux plages horaires [O2]. Une convocation
  « table 4 contre … » est transactionnelle. **Elle ne doit contenir aucune
  publicité** (par exemple pour la buvette du club), sinon elle devient
  commerciale.
- **Numéros mobiles français comme expéditeur** : ils sont réservés aux
  échanges de personne à personne. Les envois automatisés passent par un
  expéditeur alphanumérique ou un numéro court [O21]. Un expéditeur
  alphanumérique ne peut pas recevoir de réponse [O21].
- **RGPD** (à traiter dans B019) : le club est responsable du traitement, le
  prestataire est sous-traitant. Il faut un consentement explicite à la
  saisie du numéro, un usage limité aux convocations, et l'effacement à la fin
  du tournoi (déjà prévu dans B019). Conserver côté prestataire le moins
  possible : par exemple, réduire la conservation de l'historique quand
  l'outil le permet (SMSFactor : réglable [O18]).
- **Longueur** : un SMS fait 160 caractères (alphabet GSM 7 bits) ou 70 en
  Unicode [O3]. Au-delà, il est découpé et facturé en plusieurs SMS
  [O6][O13]. Certains caractères français (ê, â, î, ô, û, ë…) ne sont pas dans
  l'alphabet GSM 7 bits de base et font passer le message en Unicode. À
  vérifier par les tests de textes de B019 (remplacer ces lettres ou viser
  70 caractères).

---

## 4. Recommandation argumentée

**Brevo pour commencer, OVHcloud en repli.**

- **Coût adapté à un club** : pas d'abonnement et des crédits qui
  n'expirent pas [S5]. Un tournoi de 150 joueurs avec environ 5 SMS par joueur
  (convocations et arbitrage) représente à peu près 750 SMS, soit environ
  34 € HT. Les crédits restants servent au tournoi suivant. SMSFactor oblige à
  acheter 1 000 SMS valables un an [O4]. Octopush semble lié à un abonnement
  mensuel [S9][O8].
- **Intégration la plus propre** : une seule requête `POST` JSON, la clé dans
  un en-tête (pas dans l'URL) et une réponse `201` avec un `messageId` [O6].
  C'est facile avec `java.net.http`, sans aucune dépendance.
- **Données dans l'UE** [O7].
- **Points faibles** : le prix vient d'une source secondaire du 24 octobre
  2026 [S5], à confirmer sur brevo.com. Brevo est d'abord une plateforme
  d'e-mailing, et une validation du compte avant le premier envoi de SMS est
  possible (non trouvé [?]). La règle « le message passe en marketing s'il
  contient un code STOP » [O6] impose de ne jamais mettre de STOP dans les
  convocations.
- **Pourquoi OVHcloud en repli** : prix officiel clair, packs de 100 SMS
  [O1] et opérateur français. Mais `http2sms` met le mot de passe dans l'URL
  et filtre par IP (code 401 « pas d'IP autorisée » [O3]), ce qui gêne un
  ordinateur portable qui change de réseau dans un gymnase. L'API complète
  d'OVH demande une signature des requêtes (non étudiée ici).
- **Twilio écarté** : le plus cher [O13], LOA AF2M à fournir [O17] et
  hébergement UE non vérifié.

**Question produit pour Quentin** (à trancher avant B019) : le compte SMS
est-il **celui du club** (chaque club crée son compte, saisit sa clé et
paie ses SMS : le plus simple juridiquement, l'expéditeur est le nom du club)
ou **celui de PingManager** (Quentin revend des SMS : facturation à gérer,
PingManager devient sous-traitant RGPD de chaque club, et un seul expéditeur
`PINGMANAGER` doit être validé) ? Option prudente et réversible : **le compte
du club**.

---

## 5. Ce que Quentin doit faire

1. Vérifier le prix et les conditions des SMS sur brevo.com (page des tarifs
   et « crédits SMS »). Si le prix diffère beaucoup, regarder OVHcloud.
2. Créer un compte Brevo (offre gratuite) au nom du club pilote. Acheter le
   plus petit lot de crédits SMS pour les essais.
3. Déclarer l'expéditeur : **nom du club** en lettres et chiffres, 11
   caractères au plus (pas `PINGTOURNOI`). Envoyer un SMS d'essai vers son
   propre portable pour vérifier qu'il n'est pas remplacé.
4. Créer une **clé d'API** dédiée (paramètres du compte, clés API). La copier
   **hors du dépôt**, dans `~/.pingmanager/sms.properties` (dossier déjà
   utilisé par l'application, voir `ServiceFichierTournoi`) :
   ```
   prestataire=brevo
   cle=xkeysib-…
   expediteur=TTBRUNOY
   ```
   Ne jamais la coller dans le code, un test, une capture, un journal ni une
   Pull Request. En cas de fuite, la révoquer depuis Brevo.
5. Signer ou accepter l'accord de sous-traitance (DPA) proposé par le
   prestataire au nom du club, s'il en propose un.
6. Répondre à la question « compte du club ou compte PingManager »
   (section 4).

---

## 6. Branchement du futur port `EnvoiSms` (tâche B019)

Même modèle que le port existant `core.EnvoiEmail` : le cœur ne connaît que
l'interface. L'implémentation HTTP vit **hors du cœur**, dans un package
d'infrastructure à valider avec `gardien-architecture` (par exemple
`com.pingmanager.envoi`, au même niveau que `importation`). **Aucune
dépendance Maven** : `java.net.http.HttpClient` (JDK) et un JSON écrit à la
main (trois champs).

```java
// core : le port (B019)
public interface EnvoiSms {
    ResultatEnvoiSms envoyer(String numeroInternational, String texte);
}
```

Esquisse de l'adaptateur Brevo (hors du cœur, d'après [O6]) :

```java
HttpRequest requete = HttpRequest.newBuilder(
        URI.create("https://api.brevo.com/v3/transactionalSMS/send"))
    .header("api-key", cle)                 // lue dans ~/.pingmanager/sms.properties
    .header("content-type", "application/json")
    .header("accept", "application/json")
    .timeout(Duration.ofSeconds(10))
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"sender\":\"" + json(expediteur) + "\","
      + "\"recipient\":\"" + numero + "\","      // ex. 33612345678, sans « + »
      + "\"content\":\"" + json(texte) + "\","
      + "\"type\":\"transactional\"}"))
    .build();
HttpResponse<String> reponse = client.send(requete, HttpResponse.BodyHandlers.ofString());
// 201 → envoyé (messageId dans le corps) ; 400/401/402… → ResultatEnvoiSms en échec avec motif
```

Points d'attention pour B019 :
- `json(...)` doit échapper `"` et `\` et les retours à la ligne. Le numéro
  est normalisé au format international (`06…` → `336…`).
- Appel **hors du fil JavaFX** (l'écran ne doit pas geler). Délai maximal de
  10 s. Un échec ne bloque jamais le lancement d'un match, il est inscrit au
  journal des envois.
- L'implémentation **simulée** (journal à l'écran) reste celle par défaut
  quand `~/.pingmanager/sms.properties` est absent. Les tests JUnit
  n'appellent jamais le vrai service.
- Un adaptateur OVH (`http2sms`, simple `GET` avec paramètres encodés par
  `URLEncoder`, `noStop=1`, `contentType=application/json` [O3]) peut être
  ajouté plus tard derrière le même port, choisi par `prestataire=ovh`.
- La clé n'apparaît jamais dans les messages d'erreur ni dans les journaux.

---

## 7. Sources (consultées le 6 novembre 2026)

1. [O] OVHcloud, « SMS Tarifs » : https://www.ovhcloud.com/fr/sms/prices/
2. [O] OVHcloud, FAQ SMS (expéditeur, transactionnel, plages horaires) : https://docs.ovhcloud.com/en/guides/web-cloud/messaging/sms/faq-sms
3. [O] OVHcloud, « Envoyer des SMS depuis une URL - http2sms » : https://docs.ovhcloud.com/fr/guides/web-cloud/messaging/sms/send-sms-http2sms
4. [O] SMSFactor, tarifs : https://smsfactor.com/tarifs
5. [S] Kanal, « Brevo prix : tarifs 2026, forfaits, SMS et WhatsApp », prix relevés le 24/10/2026 : https://getkanal.com/fr/blog/brevo-prix
6. [O] Brevo, documentation « Transactional SMS endpoints » : https://developers.brevo.com/docs/transactional-sms-endpoints
7. [O] Brevo, « Sécurité et confidentialité des données » : https://www.brevo.com/fr/securite/
8. [O] Octopush, page d'offre (plan Basique 65 €/mois, données hébergées en France) : https://octopush.com/?p=22291
9. [S] Tool-Advisor, « Notre avis sur Octopush en 2026 », mis à jour le 19/08/2026 : https://tool-advisor.fr/sms-pro/comparatif/octopush/
10. [S] Tool-Advisor, « Notre avis sur SMSFactor en 2026 », mis à jour le 20/08/2026 : https://tool-advisor.fr/sms-pro/comparatif/smsfactor/
11. [S] Efidem, « French Sender ID rules… from early 2025 », 15/05/2025 : https://www.efidem.com/en/blog/2025/05/15/marketing-sms-sender-id-security/
12. [S] LINK Mobility, « France updates SMS Sender ID requirements from March 2026 », 27/02/2026 : https://www.linkmobility.com/news/france-updates-sms-sender-id-requirements-from-march-2026
13. [O] Twilio, tarifs SMS France (à jour d'octobre 2026) : https://twilio.com/sms/pricing/fr
14. [O] Octopush, documentation de l'API (en-têtes, endpoint) : https://dev.octopush.com/?p=8155 et paramètres : https://scriptagc.wasmer.app/https_dev_octopush_com/en/sms-gateway-api-documentation/parameters (miroir de dev.octopush.com)
15. [O] OVHcloud, « Gérer les crédits SMS et activer la recharge automatique » : https://docs.ovhcloud.com/fr/guides/web-cloud/messaging/sms/activer-la-recharge-automatique-du-credit-sms
16. [O] Brevo, note de version « Sender ID compliance », 17/07/2026 : https://www.brevo.com/fr/releases/sender-id-compliance/
17. [O] Twilio, « How to Register an Alphanumeric Sender ID in France » (LOA AF2M) : https://support.twilio.com/hc/en-us/articles/36973882933787-How-to-Register-an-Alphanumeric-Sender-ID-in-France
18. [O] SMSFactor, « RGPD : nos engagements » : https://smsfactor.com/rgpd-nos-engagements
19. [O] CM.com, tarifs SMS : https://www.cm.com/fr-fr/tarifs/sms/
20. [O] smsmode, « SMS rates by country » : https://www.smsmode.com/en/tarifs-text-messaging-par-pays/
21. [O] Twilio, « Limitations on sending SMS to French mobile numbers » : https://support.twilio.com/hc/en-us/articles/223133827-Limitations-on-sending-SMS-to-French-mobile-numbers
