package com.pingmanager.importation;

import com.pingmanager.core.ClassementExporte;
import com.pingmanager.core.ExportResultatsFederaux;
import com.pingmanager.core.FichierExporte;
import com.pingmanager.core.JoueurExporte;
import com.pingmanager.core.PartieExportee;
import com.pingmanager.core.ResultatsTournoi;
import com.pingmanager.model.Manche;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Export des résultats en CSV <b>provisoire</b> (B023) : {@code parties.csv} et
 * {@code classements.csv}, en attendant le format fédéral « donné lors de
 * l'homologation » (IX.107). Format détaillé dans
 * {@code docs/FORMAT_ECHANGES_FFTT.md}.
 *
 * <p>Point-virgule, UTF-8 avec BOM, CRLF ; dates jj/mm/aaaa, heures hh:mm ;
 * cellules échappées et formules neutralisées ({@link CsvFederal#cellule}).
 * Ne contient que ce que le tournoi contient déjà : ni date de naissance, ni
 * certificat. Aucune écriture disque : l'interface enregistre les fichiers.</p>
 */
public final class ExportResultatsCsv implements ExportResultatsFederaux {

    public static final String NOM_FORMAT = "CSV provisoire PingManager v1";

    static final List<String> ENTETES_PARTIES = List.of("N° homologation", "Tableau", "Phase", "Étape", "N° partie",
            "Date", "Heure", "Table", "Licence A", "Nom A", "Prénom A", "Club A", "Points A",
            "Licence B", "Nom B", "Prénom B", "Club B", "Points B", "Score", "Manches A", "Manches B",
            "Vainqueur", "Issue", "Fautif");
    static final List<String> ENTETES_CLASSEMENTS = List.of("N° homologation", "Tableau", "Rang", "Licence", "Nom",
            "Prénom", "Club", "Points");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MIME = "text/csv";

    @Override
    public String nomFormat() {
        return NOM_FORMAT;
    }

    @Override
    public List<FichierExporte> produire(ResultatsTournoi r) {
        Objects.requireNonNull(r, "resultats");
        String h = r.numeroHomologation();
        List<List<String>> parties = new ArrayList<>();
        for (PartieExportee p : r.parties()) {
            List<String> l = new ArrayList<>(List.of(h, p.getTableau(), p.getPhase().name(), p.getEtape(),
                    String.valueOf(p.getNumeroPartie()),
                    p.getDate() == null ? "" : DATE.format(p.getDate()),
                    p.getHeure() == null ? "" : HEURE.format(p.getHeure()),
                    p.getTable() == null ? "" : String.valueOf(p.getTable())));
            joueur(l, p.getJoueurA());
            joueur(l, p.getJoueurB());
            l.add(score(p.getManches()));
            l.add(String.valueOf(p.manchesA()));
            l.add(String.valueOf(p.manchesB()));
            l.add(p.getLicenceVainqueur());
            l.add(p.getIssue());
            l.add(p.getLicenceFautif());
            parties.add(l);
        }
        List<List<String>> classements = new ArrayList<>();
        for (ClassementExporte c : r.classements()) {
            JoueurExporte j = c.joueur();
            classements.add(List.of(h, c.tableau(), String.valueOf(c.rang()), j.licence(), j.nom(), j.prenom(),
                    j.club(), String.valueOf(j.points())));
        }
        return List.of(
                new FichierExporte("parties.csv", CsvFederal.ecrire(ENTETES_PARTIES, parties), MIME),
                new FichierExporte("classements.csv", CsvFederal.ecrire(ENTETES_CLASSEMENTS, classements), MIME));
    }

    private static void joueur(List<String> l, JoueurExporte j) {
        l.add(j.licence());
        l.add(j.nom());
        l.add(j.prenom());
        l.add(j.club());
        l.add(String.valueOf(j.points()));
    }

    /** Manches séparées par une espace, du point de vue de A : « 11-7 9-11 11-5 ». */
    static String score(List<Manche> manches) {
        StringBuilder sb = new StringBuilder();
        for (Manche m : manches) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(m.pointsJoueurA()).append('-').append(m.pointsJoueurB());
        }
        return sb.toString();
    }
}
