package com.pingmanager.core;

import com.pingmanager.model.CategorieAge;
import com.pingmanager.model.ConfigurationTableau;
import com.pingmanager.model.HorairesTableau;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Sexe;
import com.pingmanager.model.StatutTournoi;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TarifInscription;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.format.RegistreFormats;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Import d'une <b>liste de joueurs</b> dans les tableaux d'un tournoi, pour un club qui a
 * préparé son tournoi hors de PingManager (règlement, tableaux, inscriptions) et veut
 * l'utiliser pour la gestion en salle.
 *
 * <p>Entrée : un tableau de cellules texte (lu par {@code com.pingmanager.importation}
 * depuis un fichier Excel, LibreOffice, CSV ou JSON). Trois temps :</p>
 * <ol>
 *   <li>{@link #analyser} : repère la ligne d'en-tête et les colonnes (noms tolérants :
 *       « Prénom », « prenom », « First name »…) ; <b>Nom, Prénom, Points et Club sont
 *       obligatoires</b> (tirage au sort et formation des poules en dépendent : points pour
 *       le classement, club pour la séparation des clubs, I.302) ; Licence, Sexe, Tableau et
 *       Date de naissance sont facultatifs. Chaque ligne est contrôlée ;</li>
 *   <li>{@link #planifier} : dit, sans rien modifier, ce qui sera fait par tableau
 *       (existant ou à créer, inscrits, liste d'attente, refus et motifs) ;</li>
 *   <li>{@link #appliquer} : crée les tableaux manquants et inscrit les joueurs.</li>
 * </ol>
 *
 * <p>Les joueurs importés sont inscrits directement ({@link Tableau#inscrire}) : les
 * règles d'inscription en ligne (quotas, tableaux consécutifs) relèvent du club, qui a
 * constitué la liste lui-même. Les conditions du tableau (points, sexe, catégories) et
 * sa capacité restent contrôlées pour les tableaux existants ; un tableau créé à
 * l'import est « toutes séries » et dimensionné sur sa liste.</p>
 */
public final class ImportJoueurs {

    /** Champ reconnu dans l'en-tête. */
    public enum Champ {
        NOM(true, "Nom"), PRENOM(true, "Prénom"), POINTS(true, "Points"), CLUB(true, "Club"),
        LICENCE(false, "Licence"), SEXE(false, "Sexe"), TABLEAU(false, "Tableau"), NAISSANCE(false, "Date de naissance"),
        CATEGORIE(false, "Catégorie");

        private final boolean obligatoire;
        private final String libelle;

        Champ(boolean obligatoire, String libelle) {
            this.obligatoire = obligatoire;
            this.libelle = libelle;
        }

        public boolean estObligatoire() {
            return obligatoire;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Intitulés acceptés (sans accents, minuscules, sans espaces ni ponctuation). */
    private static final Map<String, Champ> SYNONYMES = new LinkedHashMap<>();

    static {
        for (String s : List.of("nom", "noms", "nomdefamille", "nomjoueur", "lastname", "surname", "familyname")) {
            SYNONYMES.put(s, Champ.NOM);
        }
        for (String s : List.of("prenom", "prenoms", "prenomjoueur", "firstname", "givenname")) {
            SYNONYMES.put(s, Champ.PRENOM);
        }
        for (String s : List.of("points", "point", "pts", "nbpoints", "nombredepoints", "pointslicence",
                "pointsofficiels", "pointsclassement", "classementpoints", "pointsmensuels", "pointsfftt")) {
            SYNONYMES.put(s, Champ.POINTS);
        }
        for (String s : List.of("club", "clubs", "nomclub", "nomduclub", "association", "clubjoueur")) {
            SYNONYMES.put(s, Champ.CLUB);
        }
        for (String s : List.of("licence", "license", "licences", "nlicence", "nolicence", "numlicence",
                "numerolicence", "numerodelicence", "ndelicence", "licencefftt", "numero")) {
            SYNONYMES.put(s, Champ.LICENCE);
        }
        for (String s : List.of("sexe", "genre", "sex", "hf", "mf")) {
            SYNONYMES.put(s, Champ.SEXE);
        }
        for (String s : List.of("tableau", "tableaux", "epreuve", "epreuves", "serie", "series")) {
            SYNONYMES.put(s, Champ.TABLEAU);
        }
        for (String s : List.of("datedenaissance", "datenaissance", "naissance", "nele", "nee", "ddn", "birthdate")) {
            SYNONYMES.put(s, Champ.NAISSANCE);
        }
        for (String s : List.of("categorie", "categories", "categoriedage", "categorieage", "cat", "categ")) {
            SYNONYMES.put(s, Champ.CATEGORIE);
        }
    }

    /** Un problème rattaché à une ligne du fichier (numéro tel qu'affiché dans le tableur). */
    public record Probleme(int ligne, String message) {
        @Override
        public String toString() {
            return ligne > 0 ? "Ligne " + ligne + " : " + message : message;
        }
    }

    /** Une ligne valide du fichier. */
    public record LigneJoueur(int ligne, String nom, String prenom, int points, String club, String licence,
                              Sexe sexe, List<String> tableaux, LocalDate naissance, CategorieAge categorie) {
    }

    /** Résultat de la lecture : colonnes repérées, lignes valides, erreurs (lignes écartées), avertissements. */
    public record Analyse(List<String> entetes, Map<Champ, Integer> colonnes, List<Champ> manquantes,
                          List<LigneJoueur> lignes, List<Probleme> erreurs, List<Probleme> avertissements,
                          List<Integer> colonnesTableau) {
        /** @return vrai si les colonnes obligatoires sont toutes présentes. */
        public boolean colonnesCompletes() {
            return manquantes.isEmpty();
        }
    }

    /** Ce qui arrivera à un tableau. */
    public static final class PlanTableau {
        private final String nom;
        private final Tableau existant;
        private final List<Joueur> inscrits = new ArrayList<>();
        private final List<Joueur> attente = new ArrayList<>();
        private final List<Probleme> refus = new ArrayList<>();
        /** Définition de la feuille « Tableaux » (tableau à créer), ou {@code null}. */
        private ImportTableaux.Definition definition;
        /** Tableau provisoire construit depuis la définition, pour contrôler points, sexe et places. */
        private Tableau apercu;

        PlanTableau(String nom, Tableau existant) {
            this.nom = nom;
            this.existant = existant;
        }

        /** @return la définition de la feuille « Tableaux », ou {@code null} (tableau existant ou créé « toutes séries »). */
        public ImportTableaux.Definition definition() {
            return definition;
        }

        public String nom() {
            return nom;
        }

        /** @return le tableau existant, ou {@code null} s'il sera créé. */
        public Tableau existant() {
            return existant;
        }

        public boolean estNouveau() {
            return existant == null;
        }

        public List<Joueur> inscrits() {
            return inscrits;
        }

        public List<Joueur> attente() {
            return attente;
        }

        public List<Probleme> refus() {
            return refus;
        }
    }

    /** Plan complet : par tableau, plus les lignes sans tableau. */
    public record Plan(Map<String, PlanTableau> tableaux, List<Probleme> sansTableau, List<Probleme> remarques) {
        public int nombreAjouts() {
            return tableaux.values().stream().mapToInt(p -> p.inscrits.size() + p.attente.size()).sum();
        }

        public int nombreRefus() {
            return sansTableau.size() + tableaux.values().stream().mapToInt(p -> p.refus.size()).sum();
        }
    }

    /**
     * Options : tableau par défaut (lignes sans colonne ou sans valeur « Tableau ») et
     * création autorisée des tableaux absents.
     */
    public record Options(String tableauParDefaut, boolean creerTableaux) {
    }

    /** Horaires d'un tableau créé à l'import (jour et heure de début, facultatifs). */
    public record HoraireNouveau(LocalDate jour, LocalTime debut) {
    }

    private ImportJoueurs() {
    }

    // ==================================================================
    // 1. Analyse
    // ==================================================================

    /**
     * @param cellules lignes du fichier (cellules texte, éventuellement vides)
     */
    public static Analyse analyser(List<List<String>> cellules) {
        // En-tête : la première des 15 premières lignes qui contient au moins 2 champs obligatoires
        // (les clubs mettent souvent un titre au-dessus du tableau).
        int iEntete = -1;
        Map<Champ, Integer> colonnes = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(15, cellules.size()) && iEntete < 0; i++) {
            Map<Champ, Integer> c = reperer(cellules.get(i));
            long obligatoires = c.keySet().stream().filter(Champ::estObligatoire).count();
            if (obligatoires >= 2) {
                iEntete = i;
                colonnes = c;
            }
        }
        List<Probleme> erreurs = new ArrayList<>();
        List<Probleme> avert = new ArrayList<>();
        List<String> entetes = iEntete < 0 ? (cellules.isEmpty() ? List.of() : cellules.get(0)) : cellules.get(iEntete);
        List<Champ> manquantes = new ArrayList<>();
        for (Champ ch : Champ.values()) {
            if (ch.estObligatoire() && !colonnes.containsKey(ch)) {
                manquantes.add(ch);
            }
        }
        List<LigneJoueur> lignes = new ArrayList<>();
        if (iEntete < 0 || !manquantes.isEmpty()) {
            return new Analyse(entetes, colonnes, manquantes, lignes, erreurs, avert, List.of());
        }
        List<Integer> colsTableau = colonnesTableau(entetes);
        Set<String> vus = new LinkedHashSet<>();
        for (int i = iEntete + 1; i < cellules.size(); i++) {
            List<String> l = cellules.get(i);
            int numero = i + 1;
            if (l.stream().allMatch(c -> c == null || c.isBlank())) {
                continue;
            }
            String nom = val(l, colonnes.get(Champ.NOM));
            if (normaliser(nom).startsWith("exemple")) {
                avert.add(new Probleme(numero, "ligne d'exemple du modèle ignorée"));
                continue;
            }
            String prenom = val(l, colonnes.get(Champ.PRENOM));
            String pointsTxt = val(l, colonnes.get(Champ.POINTS));
            String club = val(l, colonnes.get(Champ.CLUB));
            List<String> manque = new ArrayList<>();
            if (nom.isEmpty()) manque.add("nom");
            if (prenom.isEmpty()) manque.add("prénom");
            if (pointsTxt.isEmpty()) manque.add("points");
            if (club.isEmpty()) manque.add("club");
            if (!manque.isEmpty()) {
                erreurs.add(new Probleme(numero, String.join(", ", manque) + " manquant" + (manque.size() > 1 ? "s" : "")
                        + (nom.isEmpty() ? "" : " (" + nom + " " + prenom + ")")));
                continue;
            }
            Integer points = lirePoints(pointsTxt);
            if (points == null) {
                erreurs.add(new Probleme(numero, "points illisibles « " + pointsTxt + " » (" + nom + " " + prenom + ")"));
                continue;
            }
            if (points < 500 || points > 4000) {
                avert.add(new Probleme(numero, nom + " " + prenom + " : " + points
                        + " points, hors de l'échelle habituelle (500 et plus) — à vérifier"));
            }
            String licence = colonnes.containsKey(Champ.LICENCE) ? val(l, colonnes.get(Champ.LICENCE)).replaceAll("\\.0$", "") : "";
            Sexe sexe = null;
            if (colonnes.containsKey(Champ.SEXE)) {
                String s = val(l, colonnes.get(Champ.SEXE));
                sexe = lireSexe(s);
                if (sexe == null && !s.isEmpty()) {
                    avert.add(new Probleme(numero, "sexe « " + s + " » non reconnu (H/F attendu) : ignoré"));
                }
            }
            List<String> tableaux = new ArrayList<>();
            for (Integer ct : colsTableau) {
                for (String t : val(l, ct).split("[;,/+|]")) {
                    if (!t.isBlank() && !tableaux.contains(t.trim())) {
                        tableaux.add(t.trim());
                    }
                }
            }
            CategorieAge categorie = null;
            if (colonnes.containsKey(Champ.CATEGORIE)) {
                String c = val(l, colonnes.get(Champ.CATEGORIE));
                categorie = lireCategorie(c);
                if (categorie == null && !c.isEmpty()) {
                    avert.add(new Probleme(numero, "catégorie « " + c + " » non reconnue : ignorée"));
                }
            }
            LocalDate naissance = null;
            if (colonnes.containsKey(Champ.NAISSANCE)) {
                String d = val(l, colonnes.get(Champ.NAISSANCE));
                naissance = lireDate(d);
                if (naissance == null && !d.isEmpty()) {
                    avert.add(new Probleme(numero, "date de naissance « " + d + " » illisible : ignorée"));
                }
            }
            String cle = (licence.isEmpty() ? normaliser(nom + "|" + prenom + "|" + club) : licence)
                    + "#" + String.join(";", tableaux).toLowerCase(Locale.ROOT);
            if (!vus.add(cle)) {
                avert.add(new Probleme(numero, nom + " " + prenom + " apparaît deux fois : ligne ignorée"));
                continue;
            }
            lignes.add(new LigneJoueur(numero, nom.toUpperCase(Locale.FRENCH), capitaliser(prenom), points, club,
                    licence.isEmpty() ? null : licence, sexe, tableaux, naissance, categorie));
        }
        if (lignes.isEmpty() && erreurs.isEmpty()) {
            erreurs.add(new Probleme(0, "Aucun joueur sous la ligne d'en-tête."));
        }
        return new Analyse(entetes, colonnes, manquantes, lignes, erreurs, avert, colsTableau);
    }

    /** Toutes les colonnes « Tableau », « Tableau 1 », « Tableau 2 »… de l'en-tête. */
    private static List<Integer> colonnesTableau(List<String> entete) {
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < entete.size(); i++) {
            String k = normaliser(entete.get(i)).replaceAll("[^a-z0-9]", "");
            if (SYNONYMES.get(k) == Champ.TABLEAU || SYNONYMES.get(k.replaceAll("\\d+$", "")) == Champ.TABLEAU) {
                res.add(i);
            }
        }
        return res;
    }

    /**
     * Catégorie d'âge écrite en clair ou en code : « Senior », « S », « Vétéran 45 », « V45 »,
     * « Junior 2 », « J2 », « Cadet », « C1 », « Minime », « M2 », « Benjamin », « B1 »,
     * « Poussin », « P », « U13 »…
     */
    public static CategorieAge lireCategorie(String s) {
        String n = normaliser(s).replaceAll("[^a-z0-9]", "");
        if (n.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher v = java.util.regex.Pattern.compile("^(?:v|veteran|veterans)(\\d{2})$").matcher(n);
        if (v.matches()) {
            int age = Integer.parseInt(v.group(1));
            for (CategorieAge c : CategorieAge.values()) {
                if (c.estVeteran() && c.name().endsWith("_" + age)) {
                    return c;
                }
            }
            return null;
        }
        java.util.regex.Matcher u = java.util.regex.Pattern.compile("^u(\\d{1,2})$").matcher(n);
        if (u.matches()) {
            for (CategorieAge c : CategorieAge.values()) {
                if (("U" + u.group(1)).equals(c.getCodeU())) {
                    return c;
                }
            }
            return null;
        }
        if (n.startsWith("pous") || n.equals("p")) return CategorieAge.POUSSIN;
        if (n.startsWith("benj") || n.matches("b[12]?")) return CategorieAge.BENJAMIN;
        if (n.startsWith("min") || n.matches("m[12]?")) return CategorieAge.MINIME;
        if (n.startsWith("cad") || n.matches("c[12]?")) return CategorieAge.CADET;
        if (n.startsWith("jun") || n.matches("j[1-4]?")) return CategorieAge.JUNIOR;
        if (n.startsWith("sen") || n.equals("s")) return CategorieAge.SENIOR;
        return null;
    }

    private static Map<Champ, Integer> reperer(List<String> ligne) {
        Map<Champ, Integer> res = new LinkedHashMap<>();
        for (int i = 0; i < ligne.size(); i++) {
            Champ c = SYNONYMES.get(normaliser(ligne.get(i)).replaceAll("[^a-z0-9]", ""));
            if (c != null && !res.containsKey(c)) {
                res.put(c, i);
            }
        }
        return res;
    }

    // ==================================================================
    // 2. Plan (sans rien modifier)
    // ==================================================================

    public static Plan planifier(Tournoi tournoi, Analyse a, Options o) {
        return planifier(tournoi, a, o, List.of());
    }

    /**
     * @param definitions tableaux de la feuille « Tableaux » : créés s'ils n'existent pas
     *                    (un tableau existant du même nom est conservé tel quel)
     */
    public static Plan planifier(Tournoi tournoi, Analyse a, Options o, List<ImportTableaux.Definition> definitions) {
        Objects.requireNonNull(tournoi, "tournoi");
        Map<String, PlanTableau> plans = new LinkedHashMap<>();
        List<Probleme> sansTableau = new ArrayList<>();
        List<Probleme> remarques = new ArrayList<>();
        for (ImportTableaux.Definition d : definitions) {
            Tableau deja = trouver(tournoi, d.nom());
            if (deja != null) {
                remarques.add(new Probleme(d.ligne(), "tableau « " + d.nom() + " » déjà présent : il est conservé tel quel"));
                continue;
            }
            PlanTableau p = new PlanTableau(d.nom(), null);
            p.definition = d;
            p.apercu = ImportTableaux.construire(d, d.places() == null ? 0 : d.places(), tournoi);
            plans.put(d.nom(), p);
        }
        // Une instance de joueur par identité (licence réelle ou fabriquée) : le même joueur
        // sur plusieurs tableaux reste le même joueur (pour les poules et l'ordonnancement).
        Map<String, Joueur> parLicence = new LinkedHashMap<>();
        for (Tableau t : tournoi.getTableaux()) {
            t.getInscrits().forEach(j -> parLicence.put(j.getNumeroLicence(), j));
            t.getFileAttente().forEach(j -> parLicence.put(j.getNumeroLicence(), j));
        }
        // Places simulées par tableau existant (inscrits / attente déjà là).
        Map<Tableau, int[]> occupation = new LinkedHashMap<>();
        LocalDate ref = CalendrierTournoi.debut(tournoi);

        for (LigneJoueur l : a.lignes()) {
            List<String> cibles = l.tableaux().isEmpty() && o.tableauParDefaut() != null && !o.tableauParDefaut().isBlank()
                    ? List.of(o.tableauParDefaut().trim()) : l.tableaux();
            if (cibles.isEmpty()) {
                sansTableau.add(new Probleme(l.ligne(), l.nom() + " " + l.prenom()
                        + " : aucun tableau (colonne « Tableau » vide et pas de tableau choisi)"));
                continue;
            }
            for (String nomCible : cibles) {
                Tableau existant = trouver(tournoi, nomCible);
                PlanTableau defini = existant == null ? trouverDefini(plans, nomCible) : null;
                String cle = existant != null ? existant.getNom() : defini != null ? defini.nom : nomCible;
                PlanTableau p = plans.get(cle);
                if (p == null) {
                    if (existant == null && !o.creerTableaux()) {
                        sansTableau.add(new Probleme(l.ligne(), "tableau « " + nomCible + " » inconnu (création désactivée)"));
                        continue;
                    }
                    p = new PlanTableau(cle, existant);
                    plans.put(cle, p);
                }
                Tableau controle = existant != null ? existant : p.apercu;
                Joueur j = joueur(l, parLicence, controle, ref);
                if (p.inscrits.contains(j) || p.attente.contains(j)
                        || (existant != null && (existant.getInscrits().contains(j) || existant.getFileAttente().contains(j)))) {
                    p.refus.add(new Probleme(l.ligne(), j.getNomComplet() + " : déjà dans ce tableau"));
                    continue;
                }
                if (controle == null) {
                    p.inscrits.add(j);
                    continue;
                }
                ConfigurationTableau c = controle.getConfiguration();
                if (!c.joueurEligible(j)) {
                    p.refus.add(new Probleme(l.ligne(), j.getNomComplet() + " : "
                            + MotifsInscription.texte(ResultatInscriptionTournoi.nonEligible(), tournoi, j, controle)));
                    continue;
                }
                if (j.getCategorie() != null && !c.categorieAutorisee(j.getCategorie())) {
                    p.refus.add(new Probleme(l.ligne(), j.getNomComplet() + " : catégorie "
                            + MotifsInscription.categorie(j.getCategorie()) + " non prévue par le tableau"));
                    continue;
                }
                int[] occ = occupation.computeIfAbsent(controle,
                        t -> new int[] {t.getInscrits().size(), t.getFileAttente().size()});
                if (!c.aLimiteDePlaces() || occ[0] < c.getPlacesMax()) {
                    occ[0]++;
                    p.inscrits.add(j);
                } else if (occ[1] < c.getTailleMaxListeAttente()) {
                    occ[1]++;
                    p.attente.add(j);
                } else {
                    p.refus.add(new Probleme(l.ligne(), j.getNomComplet() + " : tableau complet et liste d'attente pleine"));
                }
            }
        }
        return new Plan(plans, sansTableau, remarques);
    }

    /** Tableau défini dans la feuille « Tableaux » de ce nom (nom exact ou lettre avant « – »). */
    private static PlanTableau trouverDefini(Map<String, PlanTableau> plans, String nom) {
        String n = normaliser(nom);
        for (PlanTableau p : plans.values()) {
            if (p.definition == null) {
                continue;
            }
            int i = p.nom.indexOf(" – ");
            if (normaliser(p.nom).equals(n) || (i > 0 && normaliser(p.nom.substring(0, i)).equals(n))) {
                return p;
            }
        }
        return null;
    }

    /** Tableau du tournoi de ce nom : nom exact, sinon lettre avant « – » (« A » → « A – ≤ 799 pts »). */
    public static Tableau trouver(Tournoi t, String nom) {
        String n = normaliser(nom);
        for (Tableau tab : t.getTableaux()) {
            if (normaliser(tab.getNom()).equals(n)) {
                return tab;
            }
        }
        for (Tableau tab : t.getTableaux()) {
            int i = tab.getNom().indexOf(" – ");
            if (i > 0 && normaliser(tab.getNom().substring(0, i)).equals(n)) {
                return tab;
            }
        }
        return null;
    }

    private static Joueur joueur(LigneJoueur l, Map<String, Joueur> parLicence, Tableau cible, LocalDate ref) {
        String licence = l.licence() != null ? l.licence() : licenceFabriquee(l);
        Joueur j = parLicence.get(licence);
        if (j != null) {
            return j;
        }
        Sexe sexe = l.sexe();
        if (sexe == null) {
            sexe = cible != null && cible.getConfiguration().getSexeRequis() == Sexe.FEMME ? Sexe.FEMME : Sexe.HOMME;
        }
        j = new Joueur(licence, l.nom(), l.prenom(), sexe, l.club(), Math.max(0, l.points()));
        CategorieAge cat = CategorieAge.pour(l.naissance(), ref == null ? LocalDate.now() : ref);
        j.setCategorie(cat != null ? cat : l.categorie());
        parLicence.put(licence, j);
        return j;
    }

    /**
     * Identifiant stable d'un joueur sans licence dans le fichier : « IMP- » + empreinte de
     * nom, prénom et club. Le même joueur listé dans plusieurs tableaux garde la même identité.
     */
    public static String licenceFabriquee(LigneJoueur l) {
        String base = normaliser(l.nom() + "|" + l.prenom() + "|" + l.club());
        return "IMP-" + String.format("%08X", base.hashCode());
    }

    // ==================================================================
    // 3. Application
    // ==================================================================

    /** Bilan de l'import appliqué. */
    public record Bilan(int tableauxCrees, int inscrits, int enAttente, int refus) {
    }

    /**
     * Crée les tableaux manquants (format par défaut, toutes séries, places = liste
     * importée, inscription gratuite, horaires facultatifs) et inscrit les joueurs.
     *
     * @param horaires horaires des tableaux créés, par nom (peut être vide)
     */
    public static Bilan appliquer(Tournoi tournoi, Plan plan, Map<String, HoraireNouveau> horaires) {
        if (tournoi.getStatut() == StatutTournoi.ANNULE) {
            throw new IllegalStateException("Tournoi annulé : import impossible.");
        }
        int crees = 0;
        int inscrits = 0;
        int attente = 0;
        for (PlanTableau p : plan.tableaux().values()) {
            Tableau t = p.existant();
            if (t == null && p.definition != null) {
                ImportTableaux.Definition d = p.definition;
                t = ImportTableaux.construire(d, d.places() != null ? d.places() : Math.max(1, p.inscrits().size()), tournoi);
                HoraireNouveau h = horaires == null ? null : horaires.get(p.nom());
                if (h != null && h.jour() != null && (t.getHoraires() == null || t.getHoraires().getDebutMatchs() == null)) {
                    t.setHoraires(new HorairesTableau(null, null,
                            h.jour().atTime(h.debut() == null ? LocalTime.of(9, 0) : h.debut())));
                }
                tournoi.ajouterTableau(t);
                crees++;
            } else if (t == null) {
                t = new Tableau(p.nom(), ConfigurationTableau.depuisFormat(RegistreFormats.formatParDefaut())
                        .toutesSeries().placesMax(Math.max(1, p.inscrits().size())).tailleMaxListeAttente(0)
                        .tarif(TarifInscription.gratuit()).build());
                t.setType("Toutes séries");
                HoraireNouveau h = horaires == null ? null : horaires.get(p.nom());
                if (h != null && h.jour() != null) {
                    t.setHoraires(new HorairesTableau(null, null,
                            h.jour().atTime(h.debut() == null ? LocalTime.of(9, 0) : h.debut())));
                }
                tournoi.ajouterTableau(t);
                crees++;
            }
            for (Joueur j : p.inscrits()) {
                if (t.inscrire(j) == Tableau.ResultatInscription.INSCRIT) {
                    inscrits++;
                }
            }
            for (Joueur j : p.attente()) {
                if (t.inscrire(j) == Tableau.ResultatInscription.EN_FILE_ATTENTE) {
                    attente++;
                }
            }
        }
        return new Bilan(crees, inscrits, attente, plan.nombreRefus());
    }

    // ==================================================================
    // Lecture des valeurs
    // ==================================================================

    private static String val(List<String> l, Integer i) {
        if (i == null || i >= l.size() || l.get(i) == null) {
            return "";
        }
        return l.get(i).trim();
    }

    /** « 1234 », « 1 234 », « 1234.0 » (tableur), « 1234 pts » → 1234. */
    static Integer lirePoints(String s) {
        String t = s.replace(' ', ' ').replace(' ', ' ').replace(" ", "").toLowerCase(Locale.ROOT)
                .replace("points", "").replace("pts", "").replace("pt", "");
        if (t.matches("\\d{1,2}[.,]\\d{3}")) {
            return Integer.valueOf(t.replaceAll("[.,]", "")); // séparateur de milliers : « 1.234 »
        }
        if (t.matches("\\d+([.,]0+)?")) {
            return Integer.valueOf(t.replaceAll("[.,]0+$", ""));
        }
        if (t.matches("\\d+[.,]\\d+")) {
            return (int) Math.round(Double.parseDouble(t.replace(',', '.')));
        }
        return null;
    }

    static Sexe lireSexe(String s) {
        String n = normaliser(s);
        if (n.isEmpty()) {
            return null;
        }
        if (List.of("h", "m", "homme", "hommes", "masculin", "garcon", "messieurs", "monsieur", "male").contains(n)) {
            return Sexe.HOMME;
        }
        if (List.of("f", "d", "femme", "femmes", "feminin", "fille", "dame", "dames", "madame", "female").contains(n)) {
            return Sexe.FEMME;
        }
        return null;
    }

    /** 20/01/2003, 20-01-2003, 2003-01-20, ou numéro de série de tableur (jours depuis le 30/12/1899). */
    static LocalDate lireDate(String s) {
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        for (String f : List.of("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd.MM.yyyy")) {
            try {
                return LocalDate.parse(t.length() > 10 && f.startsWith("yyyy") ? t.substring(0, 10) : t,
                        DateTimeFormatter.ofPattern(f));
            } catch (RuntimeException ignore) {
                // format suivant
            }
        }
        if (t.matches("\\d{4,5}([.,]\\d+)?")) {
            long jours = (long) Double.parseDouble(t.replace(',', '.'));
            return LocalDate.of(1899, 12, 30).plusDays(jours);
        }
        return null;
    }

    static String normaliser(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private static String capitaliser(String prenom) {
        StringBuilder sb = new StringBuilder();
        boolean debut = true;
        for (char c : prenom.trim().toLowerCase(Locale.FRENCH).toCharArray()) {
            sb.append(debut ? Character.toUpperCase(c) : c);
            debut = c == ' ' || c == '-' || c == '\'';
        }
        return sb.toString();
    }
}
