package com.pingmanager.importation;

import com.pingmanager.core.RegistreLicences;
import com.pingmanager.model.FicheLicence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link RegistreLicences} alimenté par un fichier de licenciés fourni par le
 * club ({@link LecteurLicencesCsv}, format provisoire B023). Lecture seule,
 * en mémoire : rien n'est écrit sur disque.
 */
public final class RegistreLicencesFichier implements RegistreLicences {

    private final Map<String, FicheLicence> parNumero = new LinkedHashMap<>();
    private final Map<String, SituationCertificat> certificats;
    private final LocalDate releveLe;

    public RegistreLicencesFichier(ResultatImportLicences import_) {
        for (FicheLicence f : import_.fiches()) {
            parNumero.put(f.getNumeroLicence(), f);
        }
        this.certificats = import_.certificats();
        this.releveLe = import_.releveLe();
    }

    @Override
    public Optional<FicheLicence> trouverParLicence(String numeroLicence) {
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parNumero.get(numeroLicence.trim()));
    }

    @Override
    public List<FicheLicence> rechercher(String texte, int max) {
        List<FicheLicence> res = new ArrayList<>();
        String t = CsvFederal.normaliser(texte);
        if (t.length() < 2 || max <= 0) {
            return res;
        }
        String[] mots = t.split(" ");
        for (FicheLicence f : parNumero.values()) {
            String cible = CsvFederal.normaliser(f.getNom() + " " + f.getPrenom());
            boolean ok = true;
            for (String m : mots) {
                if (!(cible.contains(m) || CsvFederal.normaliser(f.getNumeroLicence()).startsWith(m))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                res.add(f);
                if (res.size() >= max) {
                    break;
                }
            }
        }
        return res;
    }

    @Override
    public List<FicheLicence> listerParClub(String numeroClub) {
        List<FicheLicence> res = new ArrayList<>();
        if (numeroClub == null || numeroClub.isBlank()) {
            return res;
        }
        String n = numeroClub.trim();
        for (FicheLicence f : parNumero.values()) {
            if (n.equals(f.getNumeroClub())) {
                res.add(f);
            }
        }
        return res;
    }

    /** @return la situation déclarée du certificat médical (INCONNU si la licence est absente). */
    public SituationCertificat situationCertificat(String numeroLicence) {
        return numeroLicence == null ? SituationCertificat.INCONNU
                : certificats.getOrDefault(numeroLicence.trim(), SituationCertificat.INCONNU);
    }

    /** @return la date de relevé la plus ancienne du fichier, ou null. */
    public LocalDate getReleveLe() {
        return releveLe;
    }

    public int taille() {
        return parNumero.size();
    }
}
