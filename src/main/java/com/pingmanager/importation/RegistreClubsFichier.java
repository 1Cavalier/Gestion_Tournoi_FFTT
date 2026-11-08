package com.pingmanager.importation;

import com.pingmanager.core.RegistreClubs;
import com.pingmanager.model.Club;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link RegistreClubs} alimenté par un fichier de clubs ({@link LecteurClubsCsv},
 * format provisoire B023). Lecture seule, en mémoire.
 */
public final class RegistreClubsFichier implements RegistreClubs {

    private final Map<String, Club> parNumero = new LinkedHashMap<>();

    public RegistreClubsFichier(ResultatImportClubs import_) {
        for (Club c : import_.clubs()) {
            parNumero.put(c.getNumero(), c);
        }
    }

    @Override
    public Optional<Club> trouverParNumero(String numeroClub) {
        if (numeroClub == null || numeroClub.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parNumero.get(numeroClub.trim()));
    }

    @Override
    public List<Club> rechercher(String texte, int max) {
        List<Club> res = new ArrayList<>();
        String t = CsvFederal.normaliser(texte);
        if (t.length() < 2 || max <= 0) {
            return res;
        }
        String[] mots = t.split(" ");
        for (Club c : parNumero.values()) {
            String cible = CsvFederal.normaliser(c.getNom() + " " + c.getVille());
            boolean ok = true;
            for (String m : mots) {
                if (!(cible.contains(m) || c.getNumero().startsWith(m))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                res.add(c);
                if (res.size() >= max) {
                    break;
                }
            }
        }
        return res;
    }

    /** @return les clubs du fichier, dans l'ordre. */
    public List<Club> clubs() {
        return List.copyOf(parNumero.values());
    }

    public int taille() {
        return parNumero.size();
    }
}
