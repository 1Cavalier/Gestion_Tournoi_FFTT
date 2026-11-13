package com.pingmanager.core;

import com.pingmanager.model.AffectationJugeArbitre;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.Poule;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.TableauElimination;
import com.pingmanager.model.Tour;
import com.pingmanager.model.Tournoi;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service pur : prépare les résultats d'un tournoi pour un
 * {@link ExportResultatsFederaux} (B023, voir le README, section 8).
 *
 * <p>IX.107 : les résultats d'un tournoi homologué sont transmis dans les huit
 * jours « sous un format informatique qui sera donné lors de l'homologation ».
 * Ce format n'étant pas public, le service ne produit qu'un objet neutre
 * ({@link ResultatsTournoi}) ; le format de fichier est l'affaire du port.</p>
 *
 * <p>Règles retenues (provisoires, voir le journal de la tâche B023) :</p>
 * <ul>
 *   <li>un tableau est <b>terminé</b> quand sa phase finale est montée et que la
 *       finale (et la petite finale le cas échéant) est jouée ; seuls les tableaux
 *       terminés sont exportés, les autres donnent un avertissement ;</li>
 *   <li>les parties « exempt » et celles dont un joueur reste « à déterminer » ne
 *       sont pas exportées ; une partie entre deux joueurs sans résultat donne un
 *       avertissement et n'est pas exportée ;</li>
 *   <li>classement : 1er et 2e par la finale, 3e (et 4e) par la petite finale,
 *       sinon les deux demi-finalistes battus 3es ex aequo (comme
 *       {@link PodiumPhaseFinale}) ; les perdants d'un tour plus tôt sont classés
 *       ex aequo au rang « nombre de parties du tour + 1 » (5e pour les quarts,
 *       9e pour les huitièmes…). Les joueurs éliminés en poule ne sont pas classés ;</li>
 *   <li>numéro d'homologation absent : avertissement, pas blocage.</li>
 * </ul>
 */
public final class ServiceResultatsFederaux {

    private final Horloge horloge;
    private final ZoneId fuseau;

    /**
     * @param horloge horodatage de l'export
     * @param fuseau  fuseau des dates et heures de parties
     */
    public ServiceResultatsFederaux(Horloge horloge, ZoneId fuseau) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.fuseau = Objects.requireNonNull(fuseau, "fuseau");
    }

    /** Service horodaté par l'horloge système. */
    public ServiceResultatsFederaux() {
        this(Horloge.systeme(), ZoneId.systemDefault());
    }

    /** @return true si le tableau a une phase finale entièrement jouée. */
    public static boolean estTermine(Tableau tableau) {
        TableauElimination elim = tableau.getPhaseFinale();
        return elim != null && ServicePhaseFinale.estTerminee(elim);
    }

    /** Prépare l'export de <b>tous les tableaux terminés</b> du tournoi. */
    public ResultatPreparationExport construire(Tournoi tournoi) {
        Objects.requireNonNull(tournoi, "tournoi");
        List<String> avertissements = new ArrayList<>();
        List<Tableau> termines = new ArrayList<>();
        for (Tableau t : tournoi.getTableaux()) {
            if (estTermine(t)) {
                termines.add(t);
            } else {
                avertissements.add("Tableau « " + t.getNom() + " » non terminé : non exporté.");
            }
        }
        if (termines.isEmpty()) {
            return ResultatPreparationExport.refuse(MotifRefusExport.AUCUN_TABLEAU_TERMINE, avertissements);
        }
        return assembler(tournoi, termines, avertissements);
    }

    /** Prépare l'export d'<b>un seul tableau</b>, qui doit être terminé. */
    public ResultatPreparationExport construire(Tournoi tournoi, Tableau tableau) {
        Objects.requireNonNull(tournoi, "tournoi");
        Objects.requireNonNull(tableau, "tableau");
        List<String> avertissements = new ArrayList<>();
        if (!tournoi.getTableaux().contains(tableau)) {
            return ResultatPreparationExport.refuse(MotifRefusExport.TABLEAU_INCONNU, avertissements);
        }
        if (!estTermine(tableau)) {
            return ResultatPreparationExport.refuse(MotifRefusExport.TABLEAU_NON_TERMINE, avertissements);
        }
        return assembler(tournoi, List.of(tableau), avertissements);
    }

    private ResultatPreparationExport assembler(Tournoi tournoi, List<Tableau> tableaux, List<String> avertissements) {
        String homologation = tournoi.getNumeroHomologation();
        if (homologation == null || homologation.isBlank()) {
            avertissements.add("Numéro d'homologation absent : à compléter avant l'envoi (IX.107).");
        }
        List<PartieExportee> parties = new ArrayList<>();
        List<ClassementExporte> classements = new ArrayList<>();
        for (Tableau t : tableaux) {
            parties.addAll(parties(t, avertissements));
            classements.addAll(classement(t));
        }
        String jaNom = "";
        String jaLicence = "";
        AffectationJugeArbitre ja = tournoi.getJugeArbitrePrincipal().orElse(null);
        if (ja != null) {
            jaNom = (ja.getPrenom() + " " + ja.getNom()).trim();
            jaLicence = ja.getNumeroLicence();
        }
        ResultatsTournoi r = new ResultatsTournoi(homologation, tournoi.getNom(),
                tournoi.getJours().first(), tournoi.getJours().last(), jaNom, jaLicence,
                parties, classements, horloge.maintenant());
        return ResultatPreparationExport.exportable(r, avertissements);
    }

    private List<PartieExportee> parties(Tableau t, List<String> avertissements) {
        List<PartieExportee> res = new ArrayList<>();
        int numero = 0;
        for (Poule p : t.getPoules()) {
            for (Match m : p.getMatchs()) {
                numero = ajouter(res, t, PartieExportee.Phase.POULE, p.getNom(), m, numero, avertissements);
            }
        }
        TableauElimination elim = t.getPhaseFinale();
        if (elim != null) {
            for (Tour tour : elim.getTours()) {
                for (Match m : tour.getMatchs()) {
                    numero = ajouter(res, t, PartieExportee.Phase.TABLEAU, tour.getNom(), m, numero, avertissements);
                }
            }
        }
        return res;
    }

    private int ajouter(List<PartieExportee> res, Tableau t, PartieExportee.Phase phase, String etape, Match m,
                        int numero, List<String> avertissements) {
        Joueur a = m.getJoueurA();
        Joueur b = m.getJoueurB();
        if (a == null || b == null || m.estBye() || a.estADeterminer() || b.estADeterminer()) {
            return numero; // exempt ou place vide : pas une partie
        }
        if (m.getStatut() != StatutMatch.TERMINE || m.getVainqueur() == null) {
            avertissements.add("« " + t.getNom() + " », " + etape + " : partie " + a.getNomComplet() + " – "
                    + b.getNomComplet() + " sans résultat, non exportée.");
            return numero;
        }
        LocalDateTime instant = m.getInstantFin() != null ? m.getInstantFin() : m.getInstantLancement();
        Joueur fautif = m.getJoueurFautif();
        res.add(new PartieExportee.Builder(t.getNom(), phase)
                .etape(etape)
                .numeroPartie(numero + 1)
                .moment(instant == null ? null : instant.toLocalDate(), instant == null ? null : instant.toLocalTime().withSecond(0).withNano(0))
                .table(m.getNumeroTable())
                .joueurs(JoueurExporte.de(a), JoueurExporte.de(b))
                .manches(m.getManches())
                .resultat(m.getVainqueur().getNumeroLicence(), m.getIssue().name(),
                        fautif == null ? "" : fautif.getNumeroLicence())
                .construire());
        return numero + 1;
    }

    private static List<ClassementExporte> classement(Tableau t) {
        List<ClassementExporte> res = new ArrayList<>();
        TableauElimination elim = t.getPhaseFinale();
        PodiumPhaseFinale podium = ServicePhaseFinale.podium(elim);
        String nom = t.getNom();
        classer(res, nom, 1, podium.premier());
        classer(res, nom, 2, podium.deuxieme());
        for (Joueur j : podium.troisiemes()) {
            classer(res, nom, 3, j);
        }
        classer(res, nom, 4, podium.quatrieme());
        List<Tour> principaux = ServicePhaseFinale.toursPrincipaux(elim);
        // Du tour le plus proche de la finale au premier tour, en sautant finale et demies.
        for (int i = principaux.size() - 3; i >= 0; i--) {
            Tour tour = principaux.get(i);
            int rang = tour.getMatchs().size() + 1;
            for (Match m : tour.getMatchs()) {
                if (m.getStatut() == StatutMatch.TERMINE && !m.estBye()) {
                    classer(res, nom, rang, m.getPerdant());
                }
            }
        }
        return res;
    }

    private static void classer(List<ClassementExporte> res, String tableau, int rang, Joueur j) {
        if (j != null && !j.estADeterminer()) {
            res.add(new ClassementExporte(tableau, rang, JoueurExporte.de(j)));
        }
    }
}
