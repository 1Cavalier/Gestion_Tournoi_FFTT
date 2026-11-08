package com.pingmanager.ui.infra;

import com.pingmanager.core.RegistreClubs;
import com.pingmanager.core.RegistreLicences;
import com.pingmanager.importation.AnomalieImport;
import com.pingmanager.importation.LecteurClubsCsv;
import com.pingmanager.importation.LecteurLicencesCsv;
import com.pingmanager.importation.LecteurTableur;
import com.pingmanager.importation.RegistreClubsFichier;
import com.pingmanager.importation.RegistreLicencesFichier;
import com.pingmanager.importation.ResultatImportClubs;
import com.pingmanager.importation.ResultatImportLicences;
import com.pingmanager.model.Club;
import com.pingmanager.model.FicheLicence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Licenciés et clubs importés d'un fichier fourni par le club (B023),
 * <b>superposés</b> à la base existante : une fiche importée l'emporte sur la
 * fiche de même numéro, le reste vient de la base.
 *
 * <p>Choix prudent et réversible : l'import ne vit que le temps de la session
 * (rien n'est enregistré sur disque), en attendant la décision de Quentin sur
 * la conservation de ces données (voir le journal B023).</p>
 */
public final class RegistresImportes {

    private volatile RegistreLicencesFichier licences;
    private volatile RegistreClubsFichier clubs;

    /** @return un registre qui consulte d'abord les licences importées, puis {@code base}. */
    public RegistreLicences licences(RegistreLicences base) {
        return new RegistreLicences() {
            @Override
            public Optional<FicheLicence> trouverParLicence(String numero) {
                RegistreLicencesFichier f = licences;
                Optional<FicheLicence> r = f == null ? Optional.empty() : f.trouverParLicence(numero);
                return r.isPresent() ? r : base.trouverParLicence(numero);
            }

            @Override
            public List<FicheLicence> rechercher(String texte, int max) {
                Map<String, FicheLicence> res = new LinkedHashMap<>();
                RegistreLicencesFichier f = licences;
                if (f != null) {
                    f.rechercher(texte, max).forEach(x -> res.put(x.getNumeroLicence(), x));
                }
                for (FicheLicence x : base.rechercher(texte, max)) {
                    if (res.size() >= max) {
                        break;
                    }
                    res.putIfAbsent(x.getNumeroLicence(), x);
                }
                return new ArrayList<>(res.values());
            }

            @Override
            public List<FicheLicence> listerParClub(String numeroClub) {
                RegistreLicencesFichier f = licences;
                List<FicheLicence> r = f == null ? List.of() : f.listerParClub(numeroClub);
                return r.isEmpty() ? base.listerParClub(numeroClub) : r;
            }
        };
    }

    /** @return un registre qui consulte d'abord les clubs importés, puis {@code base}. */
    public RegistreClubs clubs(RegistreClubs base) {
        return new RegistreClubs() {
            @Override
            public Optional<Club> trouverParNumero(String numero) {
                RegistreClubsFichier f = clubs;
                Optional<Club> r = f == null ? Optional.empty() : f.trouverParNumero(numero);
                return r.isPresent() ? r : base.trouverParNumero(numero);
            }

            @Override
            public List<Club> rechercher(String texte, int max) {
                Map<String, Club> res = new LinkedHashMap<>();
                RegistreClubsFichier f = clubs;
                if (f != null) {
                    f.rechercher(texte, max).forEach(x -> res.put(x.getNumero(), x));
                }
                for (Club x : base.rechercher(texte, max)) {
                    if (res.size() >= max) {
                        break;
                    }
                    res.putIfAbsent(x.getNumero(), x);
                }
                return new ArrayList<>(res.values());
            }
        };
    }

    /**
     * Importe {@code licencies.csv} (remplace un import précédent).
     *
     * @return un résumé destiné à l'utilisateur
     */
    public String importerLicencies(Path fichier) throws LecteurTableur.ImportException {
        ResultatImportLicences r = LecteurLicencesCsv.lire(fichier);
        licences = new RegistreLicencesFichier(r);
        String releve = r.releveLe() == null ? "" : " Données relevées le "
                + r.releveLe().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";
        return r.fiches().size() + " licencié(s) importé(s), " + r.lignesRefusees() + " ligne(s) refusée(s)."
                + releve + detail(r.anomalies());
    }

    /**
     * Importe {@code clubs.csv} (remplace un import précédent).
     *
     * @return un résumé destiné à l'utilisateur
     */
    public String importerClubs(Path fichier) throws LecteurTableur.ImportException {
        ResultatImportClubs r = LecteurClubsCsv.lire(fichier);
        clubs = new RegistreClubsFichier(r);
        return r.clubs().size() + " club(s) importé(s), " + r.lignesRefusees() + " ligne(s) refusée(s)."
                + detail(r.anomalies());
    }

    /** @return le nombre de licences importées (0 sans import). */
    public int nombreLicences() {
        RegistreLicencesFichier f = licences;
        return f == null ? 0 : f.taille();
    }

    /** @return le nombre de clubs importés (0 sans import). */
    public int nombreClubs() {
        RegistreClubsFichier f = clubs;
        return f == null ? 0 : f.taille();
    }

    private static String detail(List<AnomalieImport> anomalies) {
        if (anomalies.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (AnomalieImport a : anomalies) {
            if (n++ == 5) {
                sb.append("\n… et ").append(anomalies.size() - 5).append(" autre(s).");
                break;
            }
            sb.append('\n').append(a);
        }
        return sb.toString();
    }
}
