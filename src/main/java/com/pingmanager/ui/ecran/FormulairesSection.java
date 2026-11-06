package com.pingmanager.ui.ecran;

import com.pingmanager.core.ServiceCompletudeTournoi.Niveau;
import com.pingmanager.core.ResultatModificationTournoi;
import com.pingmanager.core.ServiceVerrousHomologation;
import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.ChampHomologation;
import com.pingmanager.model.Club;
import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.EchelonHomologation;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.NiveauTournoi;
import com.pingmanager.model.PhasePoints;
import com.pingmanager.core.reglement.Formats;
import com.pingmanager.model.PolitiqueBalles;
import com.pingmanager.model.Tournoi;
import com.pingmanager.model.TypeTournoi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Formulaires d'édition d'une section d'un tournoi. Chaque formulaire lit les
 * valeurs courantes, et ne les applique au {@link Tournoi} qu'au moment de
 * l'enregistrement (l'annulation ne modifie rien).
 *
 * <p>Présentation : libellés au-dessus des contrôles, champs regroupés par
 * thème, et code couleur expliqué par une légende en tête : un rond devant le
 * libellé pour la dépendance (homologation, publication, optionnel), et le
 * cadre du champ pour son état (vert rempli, orange obligatoire vide, gris
 * facultatif). Champ fautif bordé de rouge. Dates au format jj/mm/aaaa.</p>
 */
final class FormulairesSection {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HH:mm");

    private FormulairesSection() {
    }

    /**
     * Ce que le formulaire doit connaître de l'application : les clubs gérés
     * (base FFTT) et le correspondant proposé par défaut pour un club.
     *
     * @param clubs          clubs administrés par le compte connecté
     * @param correspondant  numéro de club → {nom, prénom, email} par défaut
     */
    record Referentiel(List<Club> clubs, Function<String, String[]> correspondant) {
    }

    /**
     * @param persister enregistre le tournoi sans quitter le formulaire (modifications
     *                  acceptées quand une autre est refusée par un verrou d'homologation)
     */
    static Node construire(int index, String titre, Tournoi t, Referentiel ref,
                           Runnable persister, Runnable onSave, Runnable onCancel) {
        // Seule la section « Général » reste un formulaire simple ; les autres
        // sections ont leur écran dédié (tableaux, sportif, complémentaire).
        return general(titre, t, ref, persister, onSave, onCancel);
    }

    // ------------------------------------------------------------------
    // Section 0 — Général
    // ------------------------------------------------------------------

    private static Node general(String titre, Tournoi t, Referentiel ref,
                                Runnable persister, Runnable onSave, Runnable onCancel) {
        // B035 : dès la demande d'homologation, les champs du dossier ne sont plus modifiables.
        ServiceVerrousHomologation verrous = new ServiceVerrousHomologation();
        boolean nomFige = verrous.estVerrouille(t, ChampHomologation.NOM);
        boolean clubFige = verrous.estVerrouille(t, ChampHomologation.CLUB_ORGANISATEUR);
        boolean adresseFigee = verrous.estVerrouille(t, ChampHomologation.ADRESSE);
        boolean correspondantFige = verrous.estVerrouille(t, ChampHomologation.CORRESPONDANT);
        String motifVerrou = new PresentationPreparationTournoi(t)
                .motifVerrouSection(PresentationPreparationTournoi.SECTION_GENERAL);
        Niveau H = Niveau.OBLIGATOIRE_HOMOLOGATION;
        Niveau P = Niveau.OBLIGATOIRE_PUBLICATION;
        Niveau O = Niveau.OPTIONNEL;
        Niveau R = Niveau.RECOMMANDE;

        // --- Club organisateur : repris de la base FFTT, jamais saisi ---
        Club clubInitial = clubDuTournoi(t, ref.clubs());
        Club[] club = {clubInitial};
        TextField nomClub = Ui.champLecture(clubInitial != null && !clubFige
                ? clubInitial.getNom() : t.getClubOrganisateur());
        TextField affiliation = Ui.champLecture(clubInitial != null && !clubFige
                ? clubInitial.getNumero() : t.getNumeroAffiliationClub());

        // --- Le tournoi ---
        TextField nom = champ(t.getNom());
        String[] proposition = {PropositionsTournoi.nomTournoi(nomClub.getText())};
        if (valeur(nom) == null && !nomFige) {
            nom.setText(proposition[0]);
        }
        Button utiliserNom = Ui.boutonLien("");
        Runnable majProposition = () -> {
            utiliserNom.setText("Utiliser « " + proposition[0] + " »");
            boolean differente = !nomFige && !proposition[0].equals(nom.getText());
            utiliserNom.setVisible(differente);
            utiliserNom.setManaged(differente);
        };
        nom.textProperty().addListener((o, a, n) -> majProposition.run());
        utiliserNom.setOnAction(e -> nom.setText(proposition[0]));
        majProposition.run();

        ComboBox<NiveauTournoi> niveau = combo(NiveauTournoi.values(), niveauInitial(t));
        TextField homolog = champ(t.getNumeroHomologation());
        homolog.setPromptText("délivré après homologation");

        Label echelon = new Label();
        echelon.getStyleClass().add("valeur-calculee");
        Runnable majEchelon = () -> {
            EchelonHomologation e = EchelonHomologation.pour(niveau.getValue(), t.getDotationGlobale());
            echelon.setText(e == null ? "choisissez d'abord la catégorie" : e.getLibelle());
        };
        niveau.valueProperty().addListener((o, a, n) -> majEchelon.run());
        majEchelon.run();
        VBox blocEchelon = new VBox(3,
                Ui.ligne(6, Ui.note("Homologation demandée à :"), echelon),
                Ui.note("Déduit de la catégorie (règlement IX.103) : national, international ou "
                        + "dotation supérieure à 3 000 € → FFTT ; sinon la ligue, qui peut déléguer "
                        + "au comité départemental."));

        // --- Phase de référence des points (classement retenu pour les inscriptions) ---
        ComboBox<PhasePoints> phase = combo(PhasePoints.values(), t.getPhasePoints());
        Label notePhase = Ui.note("");
        notePhase.setWrapText(true);
        Runnable majPhase = () -> {
            String saison = t.getDateDebut() == null ? "de la saison du tournoi" : Formats.saison(t.getDateDebut());
            notePhase.setText(phase.getValue() == null
                    ? "Points de licence pris en compte pour les inscriptions et les tableaux limités en points."
                    : "Le règlement indiquera : points retenus de la " + phase.getValue().getLibelle() + " "
                    + saison + ".");
        };
        phase.valueProperty().addListener((o, x, n) -> majPhase.run());
        majPhase.run();

        // --- Lieu : par défaut l'adresse du club, modifiable ---
        AdresseTournoi a = t.getAdresse();
        boolean sansAdresse = a == null || (vide(a.getVoie()) && vide(a.getCodePostal()) && vide(a.getVille()));
        TextField voie = champ(a == null ? null : a.getVoie());
        TextField cp = champ(a == null ? null : a.getCodePostal());
        TextField ville = champ(a == null ? null : a.getVille());
        cp.setPromptText("ex. 91800");
        Runnable adresseDuClub = () -> {
            if (club[0] != null) {
                voie.setText(club[0].getAdresse1());
                cp.setText(club[0].getCodePostal());
                ville.setText(club[0].getVille());
            }
        };
        if (sansAdresse && !adresseFigee) {
            adresseDuClub.run();
        }
        Button reprendreAdresse = Ui.boutonLien("Reprendre l'adresse du club");
        reprendreAdresse.setOnAction(e -> adresseDuClub.run());

        // --- Dates de l'ensemble du tournoi ---
        TextField debut = champ(fmt(t.getDateDebut()));
        TextField fin = champ(fmt(t.getDateFin()));
        debut.setPromptText("jj/mm/aaaa");
        fin.setPromptText("jj/mm/aaaa");

        // --- Correspondant : par défaut l'administrateur principal ---
        TextField corNom = champ(t.getCorrespondantNom());
        TextField corPrenom = champ(t.getCorrespondantPrenom());
        TextField corEmail = champ(t.getCorrespondantEmail());
        TextField corTel = champ(t.getCorrespondantTelephone());
        corEmail.setPromptText("nom@exemple.fr");
        Runnable adminPrincipal = () -> {
            if (club[0] != null) {
                String[] c = ref.correspondant().apply(club[0].getNumero());
                corNom.setText(c[0]);
                corPrenom.setText(c[1]);
                corEmail.setText(c[2]);
            }
        };
        if (valeur(corNom) == null && valeur(corPrenom) == null && valeur(corEmail) == null
                && !correspondantFige) {
            adminPrincipal.run();
        }
        Button reprendreAdmin = Ui.boutonLien("Reprendre l'administrateur principal");
        reprendreAdmin.setOnAction(e -> adminPrincipal.run());

        // --- Choix du club si le compte en gère plusieurs ---
        Groupe groupeClub = new Groupe("Club organisateur");
        if (ref.clubs().size() > 1) {
            ComboBox<Club> choix = new ComboBox<>();
            choix.getItems().addAll(ref.clubs());
            choix.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Club c) {
                    return c == null ? "" : c.getNom() + " (" + c.getNumero() + ")";
                }

                @Override
                public Club fromString(String s) {
                    return null;
                }
            });
            choix.setValue(clubInitial);
            if (clubFige) {
                Ui.verrouiller(choix, motifVerrou);
            }
            choix.valueProperty().addListener((o, ancien, nouveau) -> {
                club[0] = nouveau;
                nomClub.setText(nouveau == null ? "" : nouveau.getNom());
                affiliation.setText(nouveau == null ? "" : nouveau.getNumero());
                boolean nomParDefaut = nom.getText().equals(proposition[0]);
                proposition[0] = PropositionsTournoi.nomTournoi(nomClub.getText());
                if (nomParDefaut) {
                    nom.setText(proposition[0]);
                }
                majProposition.run();
            });
            groupeClub.large(Ui.champEtiquete("Club", choix));
        }
        groupeClub
                .demi(Ui.champEtiquete("Nom du club", nomClub, H))
                .demi(Ui.champEtiquete("N° d'affiliation", affiliation, H))
                .large(Ui.note("Repris de la base FFTT du club : non modifiable ici."));

        // --- Verrous du dossier d'homologation (B035) : champs grisés, motif en infobulle ---
        if (nomFige) {
            Ui.verrouiller(nom, motifVerrou);
        }
        if (verrous.estVerrouille(t, ChampHomologation.CATEGORIE)) {
            Ui.verrouiller(niveau, motifVerrou);
        }
        if (adresseFigee) {
            Ui.verrouiller(voie, motifVerrou);
            Ui.verrouiller(cp, motifVerrou);
            Ui.verrouiller(ville, motifVerrou);
            Ui.cacher(reprendreAdresse);
        }
        if (verrous.estVerrouille(t, ChampHomologation.DATES)) {
            Ui.verrouiller(debut, motifVerrou);
            Ui.verrouiller(fin, motifVerrou);
        }
        if (correspondantFige) {
            Ui.verrouiller(corNom, motifVerrou);
            Ui.verrouiller(corPrenom, motifVerrou);
            Ui.verrouiller(corEmail, motifVerrou);
            Ui.cacher(reprendreAdmin);
        }

        Node corps = groupes(
                new Groupe("Le tournoi")
                        .large(new VBox(4, Ui.champEtiquete("Nom du tournoi", nom, H), utiliserNom))
                        .demi(Ui.champEtiquete("Niveau du tournoi (catégorie)", niveau, H))
                        .demi(Ui.champEtiquete("Numéro d'homologation", homolog, P))
                        .large(blocEchelon)
                        .demi(Ui.champEtiquete("Phase concernée (points retenus)", phase, R))
                        .large(notePhase),
                groupeClub,
                new Groupe("Lieu")
                        .large(Ui.note("Par défaut l'adresse du club. Modifiez-la si le tournoi "
                                + "se joue dans une autre salle."))
                        .large(Ui.champEtiquete("Adresse (voie)", voie, H))
                        .demi(Ui.champEtiquete("Code postal", cp, H))
                        .demi(Ui.champEtiquete("Ville", ville, H))
                        .large(reprendreAdresse),
                new Groupe("Dates du tournoi")
                        .large(Ui.note("Du premier au dernier jour de l'ensemble du tournoi, toutes "
                                + "journées comprises (pas une seule journée). À renseigner dès "
                                + "maintenant : elles figurent sur la demande d'homologation."))
                        .demi(Ui.champEtiquete("Date de début", debut, H))
                        .demi(Ui.champEtiquete("Date de fin", fin, H)),
                new Groupe("Correspondant")
                        .large(Ui.note("Par défaut l'administrateur principal du club. Vous pouvez "
                                + "indiquer une autre personne."))
                        .demi(Ui.champEtiquete("Prénom", corPrenom, H))
                        .demi(Ui.champEtiquete("Nom", corNom, H))
                        .demi(Ui.champEtiquete("Email", corEmail, H))
                        .demi(Ui.champEtiquete("Téléphone", corTel, O))
                        .large(reprendreAdmin));

        return cadre(titre, EnumSet.of(H, P, R, O), motifVerrou, corps, onCancel, erreur -> {
            LocalDate dd = parseDate(debut, erreur, "date de début");
            LocalDate df = parseDate(fin, erreur, "date de fin");
            if (erreur.length() > 0) {
                return false;
            }
            if (dd != null && df != null && df.isBefore(dd)) {
                Ui.marquerErreur(fin);
                erreur.append("La date de fin ne peut pas précéder la date de début.");
                return false;
            }
            // Champs du dossier d'homologation : par les méthodes gardées du cœur (B035).
            List<ResultatModificationTournoi> resultats = new java.util.ArrayList<>();
            if (valeur(nom) != null) {
                resultats.add(verrous.modifierNom(t, valeur(nom)));
            }
            resultats.add(verrous.modifierCategorie(t, niveau.getValue()));
            if (club[0] != null && !clubFige) {
                resultats.add(verrous.modifierClubOrganisateur(t, club[0].getNom()));
                resultats.add(verrous.modifierNumeroAffiliation(t, club[0].getNumero()));
            }
            resultats.add(verrous.modifierPeriode(t, dd, df));
            AdresseTournoi saisie = adresse(text(voie), text(cp), text(ville));
            resultats.add(verrous.modifierAdresse(t, adresseFigee && saisie != null && saisie.memeLieu(t.getAdresse())
                    ? t.getAdresse() : adresseGeocodee(saisie, t.getAdresse())));
            resultats.add(verrous.modifierCorrespondant(t, text(corNom), text(corPrenom), text(corEmail),
                    text(corTel), t.getCorrespondantAutresInfos()));
            t.setNumeroHomologation(valeur(homolog));
            t.setPhasePoints(phase.getValue());
            for (ResultatModificationTournoi r : resultats) {
                String refus = PresentationPreparationTournoi.messageModification(r);
                if (refus != null) {
                    ajouter(erreur, refus);
                }
            }
            if (erreur.length() > 0) {
                persister.run();   // le reste est enregistré ; le refus reste affiché
                return false;
            }
            onSave.run();
            return true;
        });
    }

    /** Club du tournoi parmi ceux gérés : par n° d'affiliation, puis par nom, sinon le premier. */
    private static Club clubDuTournoi(Tournoi t, List<Club> clubs) {
        for (Club c : clubs) {
            if (c.getNumero().equals(t.getNumeroAffiliationClub())) {
                return c;
            }
        }
        for (Club c : clubs) {
            if (c.getNom().equalsIgnoreCase(String.valueOf(t.getClubOrganisateur()))) {
                return c;
            }
        }
        return clubs.isEmpty() ? null : clubs.get(0);
    }

    /** Catégorie actuelle ; pour un ancien fichier, reprise de l'ex-« type » quand c'est sans ambiguïté. */
    @SuppressWarnings("deprecation")
    private static NiveauTournoi niveauInitial(Tournoi t) {
        if (t.getNiveau() != null) {
            return t.getNiveau();
        }
        if (t.getTypeTournoi() == TypeTournoi.PROMOTIONNEL) {
            return NiveauTournoi.PROMOTIONNEL;
        }
        return t.getTypeTournoi() == TypeTournoi.INTERNATIONAL ? NiveauTournoi.INTERNATIONAL : null;
    }

    /**
     * Garde la position déjà connue si le lieu n'a pas changé ; sinon géocode la nouvelle
     * adresse (Géoplateforme IGN, délais courts). Sans réseau, l'adresse reste sans
     * coordonnées : la carte se rabat sur la préfecture du département.
     */
    static AdresseTournoi adresseGeocodee(AdresseTournoi nouvelle, AdresseTournoi ancienne) {
        if (nouvelle == null) {
            return null;
        }
        if (ancienne != null && ancienne.getCoordonnees() != null && nouvelle.memeLieu(ancienne)) {
            return nouvelle.avecCoordonnees(ancienne.getCoordonnees());
        }
        return com.pingmanager.ui.infra.GeocodeurGeoplateforme.INSTANCE.completer(nouvelle);
    }

    /** Adresse complète si les trois champs sont remplis, partielle sinon, nulle si tout est vide. */
    static AdresseTournoi adresse(String voie, String cp, String ville) {
        if (voie == null && cp == null && ville == null) {
            return null;
        }
        if (voie != null && cp != null && ville != null) {
            return AdresseTournoi.de(voie, cp, ville);
        }
        return AdresseTournoi.partielle(voie, null, null, cp, ville);
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    // ------------------------------------------------------------------
    // Ossature commune
    // ------------------------------------------------------------------

    /** Un applicateur renvoie true s'il a enregistré, false s'il y a une erreur. */
    private interface Applicateur {
        boolean appliquer(StringBuilder erreur);
    }

    private static Node cadre(String titre, Set<Niveau> dependances, String motifVerrou, Node corps,
                              Runnable onCancel, Applicateur appliquer) {
        Label erreur = Ui.message();

        Button enregistrer = Ui.boutonPrimaire("Enregistrer");
        enregistrer.setOnAction(e -> {
            Ui.cacher(erreur);
            StringBuilder sb = new StringBuilder();
            boolean ok = appliquer.appliquer(sb);
            if (!ok) {
                Ui.erreur(erreur, sb.length() > 0 ? sb.toString() : "Vérifiez les champs saisis.");
            }
        });
        Button annuler = Ui.boutonSecondaire("Annuler");
        annuler.setCancelButton(true);
        annuler.setOnAction(e -> onCancel.run());

        VBox enTete = new VBox(6,
                Ui.titre(titre),
                Ui.discret("Rien n'est modifié tant que vous n'avez pas enregistré."));

        if (motifVerrou != null) {
            enTete.getChildren().add(Ui.bandeauVerrou(motifVerrou));
        }
        return Ui.page(720, enTete, Ui.legende(dependances), Ui.carteNue(corps), erreur,
                Ui.actions(enregistrer, annuler));
    }

    /** Empile des groupes de champs ; le premier n'a pas de filet au-dessus. */
    private static Node groupes(Groupe... groupes) {
        VBox v = new VBox(22);
        for (int i = 0; i < groupes.length; i++) {
            if (i == 0) {
                groupes[i].boite.getStyleClass().add("premier");
            }
            v.getChildren().add(groupes[i].boite);
        }
        return v;
    }

    /** Groupe de champs titré, en grille à deux colonnes égales. */
    private static final class Groupe {
        private final GridPane grille = new GridPane();
        private final VBox boite;
        private int rang = 0;
        private int colonne = 0;

        Groupe(String titre) {
            grille.setHgap(16);
            grille.setVgap(14);
            for (int i = 0; i < 2; i++) {
                ColumnConstraints c = new ColumnConstraints();
                c.setPercentWidth(50);
                c.setHgrow(Priority.ALWAYS);
                grille.getColumnConstraints().add(c);
            }
            Label t = new Label(titre);
            t.getStyleClass().add("groupe-titre");
            boite = new VBox(12, t, grille);
            boite.getStyleClass().add("groupe-champs");
        }

        /** Un champ sur toute la largeur. */
        Groupe large(Node champ) {
            saut();
            grille.add(champ, 0, rang, 2, 1);
            rang++;
            return this;
        }

        /** Un champ sur une demi-largeur (deux par ligne). */
        Groupe demi(Node champ) {
            grille.add(champ, colonne, rang);
            if (colonne == 1) {
                colonne = 0;
                rang++;
            } else {
                colonne = 1;
            }
            return this;
        }

        /** Termine la ligne en cours (laisse la demi-colonne droite vide). */
        Groupe saut() {
            if (colonne == 1) {
                colonne = 0;
                rang++;
            }
            return this;
        }
    }

    private static TextField champ(String valeurInitiale) {
        TextField t = Ui.champ(null);
        if (valeurInitiale != null) {
            t.setText(valeurInitiale);
        }
        return t;
    }

    private static <E extends Enum<E>> ComboBox<E> combo(E[] valeurs, E courant) {
        return Ui.combo(valeurs, courant);
    }

    // --- Lecture / parsing ---

    private static String valeur(TextField t) {
        String s = t.getText() == null ? "" : t.getText().trim();
        return s.isEmpty() ? null : s;
    }

    private static String text(TextField t) {
        return valeur(t);
    }

    private static String fmt(LocalDate d) {
        return d == null ? null : d.format(D);
    }

    private static String fmt(LocalDateTime dt) {
        return dt == null ? null : dt.format(DT);
    }

    private static String fmtHeure(LocalTime h) {
        return h == null ? null : h.format(T);
    }

    private static LocalDate parseDate(TextField champ, StringBuilder erreur, String nom) {
        String s = valeur(champ);
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s, D);
        } catch (RuntimeException e) {
            Ui.marquerErreur(champ);
            ajouter(erreur, "La " + nom + " doit être au format jj/mm/aaaa.");
            return null;
        }
    }

    private static LocalDateTime parseDateTime(TextField champ, StringBuilder erreur, String nom) {
        String s = valeur(champ);
        if (s == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, DT);
        } catch (RuntimeException e) {
            Ui.marquerErreur(champ);
            ajouter(erreur, "La " + nom + " doit être au format jj/mm/aaaa HH:mm.");
            return null;
        }
    }

    private static LocalTime parseHeure(TextField champ, StringBuilder erreur, String nom) {
        String s = valeur(champ);
        if (s == null) {
            return null;
        }
        try {
            return LocalTime.parse(s, T);
        } catch (RuntimeException e) {
            Ui.marquerErreur(champ);
            ajouter(erreur, "L'" + nom + " doit être au format HH:mm.");
            return null;
        }
    }

    private static Integer parseEntier(TextField champ, StringBuilder erreur, String nom) {
        String s = valeur(champ);
        if (s == null) {
            return null;
        }
        try {
            int v = Integer.parseInt(s);
            if (v < 0) {
                Ui.marquerErreur(champ);
                ajouter(erreur, "Le " + nom + " ne peut pas être négatif.");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            Ui.marquerErreur(champ);
            ajouter(erreur, "Le " + nom + " doit être un nombre entier.");
            return null;
        }
    }

    private static void ajouter(StringBuilder erreur, String message) {
        if (erreur.length() > 0) {
            erreur.append(" ");
        }
        erreur.append(message);
    }
}
