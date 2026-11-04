package com.pingmanager.ui.infra;

import com.pingmanager.core.RegistreLicences;
import com.pingmanager.model.FicheLicence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fausse base FFTT des licences, en mémoire, pour les tests manuels.
 *
 * <p>Tient lieu du référentiel fédéral qu'on ne fait qu'interroger : on l'alimente
 * à la main avec quelques fiches, et le jour où la vraie source FFTT est
 * disponible, on remplace cette implémentation du port {@link RegistreLicences}
 * sans rien changer au cœur.</p>
 */
public final class RegistreLicencesFFTT implements RegistreLicences {

    private final Map<String, FicheLicence> parNumero = new LinkedHashMap<>();

    public RegistreLicencesFFTT ajouter(FicheLicence fiche) {
        parNumero.put(cle(fiche.getNumeroLicence()), fiche);
        return this;
    }

    @Override
    public Optional<FicheLicence> trouverParLicence(String numeroLicence) {
        if (numeroLicence == null || numeroLicence.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parNumero.get(cle(numeroLicence)));
    }

    @Override
    public List<FicheLicence> rechercher(String texte, int max) {
        List<FicheLicence> res = new ArrayList<>();
        String t = normaliser(texte);
        if (t.length() < 2) {
            return res;
        }
        String[] mots = t.split("\\s+");
        for (FicheLicence f : parNumero.values()) {
            String cible = normaliser(f.getNom() + " " + f.getPrenom());
            boolean ok = true;
            for (String m : mots) {
                if (!(cible.contains(m) || f.getNumeroLicence().startsWith(m))) {
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

    private static String normaliser(String s) {
        if (s == null) {
            return "";
        }
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).trim();
    }

    /** @return toutes les fiches connues (pour l'affichage du jeu de démo). */
    public List<FicheLicence> toutes() {
        return new ArrayList<>(parNumero.values());
    }

    public int taille() {
        return parNumero.size();
    }

    private static String cle(String numero) {
        return numero.trim();
    }
}
