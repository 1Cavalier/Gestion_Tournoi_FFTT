package com.pingmanager.core.buvette;

import com.pingmanager.model.buvette.ArticleCatalogue;
import com.pingmanager.model.buvette.CatalogueBuvette;
import com.pingmanager.model.buvette.LienContenant;
import com.pingmanager.model.buvette.RayonBuvette;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Catalogue de base de la buvette (B026), lu depuis la ressource
 * {@code catalogue-buvette.csv} du cœur (lecture seule, livrée avec
 * l'application, comme la liste des départements). Pour le compléter, il
 * suffit d'ajouter une ligne au fichier (format décrit dans son en-tête).
 *
 * <p>Coûts et prix <b>indicatifs, à adapter</b> par chaque club.</p>
 */
public final class CatalogueBase {

    /** Nom de la ressource, à côté de cette classe. */
    static final String RESSOURCE = "catalogue-buvette.csv";

    private static volatile CatalogueBuvette charge;

    private CatalogueBase() {
    }

    /** @return le catalogue livré avec l'application (lu une fois, puis gardé). */
    public static CatalogueBuvette catalogue() {
        CatalogueBuvette c = charge;
        if (c == null) {
            try (InputStream in = CatalogueBase.class.getResourceAsStream(RESSOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Ressource " + RESSOURCE + " introuvable");
                }
                c = lire(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            charge = c;
        }
        return c;
    }

    /**
     * Lit un catalogue au format de la ressource.
     *
     * @throws IllegalArgumentException si une ligne est illisible (numéro de ligne dans le message)
     */
    public static CatalogueBuvette lire(Reader source) throws IOException {
        BufferedReader r = new BufferedReader(source);
        List<ArticleCatalogue> res = new ArrayList<>();
        String ligne;
        int numero = 0;
        while ((ligne = r.readLine()) != null) {
            numero++;
            String t = ligne.strip();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            try {
                res.add(article(t));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Catalogue de la buvette, ligne " + numero + " : " + e.getMessage(), e);
            }
        }
        return new CatalogueBuvette(res);
    }

    private static ArticleCatalogue article(String ligne) {
        String[] p = ligne.split(";", -1);
        if (p.length < 9) {
            throw new IllegalArgumentException("9 colonnes attendues");
        }
        RayonBuvette rayon = RayonBuvette.lire(p[1].strip());
        if (rayon == null) {
            throw new IllegalArgumentException("rayon inconnu « " + p[1].strip() + " »");
        }
        List<LienContenant> composition = new ArrayList<>();
        for (String c : p[8].split("\\+")) {
            String e = c.strip();
            if (e.isEmpty()) {
                continue;
            }
            String[] iq = e.split("\\*");
            composition.add(new LienContenant(iq[0].strip(), iq.length > 1 ? Integer.parseInt(iq[1].strip()) : 1));
        }
        return new ArticleCatalogue(p[0].strip(), p[2].strip(), rayon, p[3].strip(), montant(p[4]),
                Integer.parseInt(p[5].strip()), p[6].isBlank() ? 0 : montant(p[6]), Long.parseLong(p[7].strip()),
                composition);
    }

    private static long montant(String euros) {
        OptionalLong c = Centimes.lire(euros);
        if (c.isEmpty()) {
            throw new IllegalArgumentException("montant illisible « " + euros.strip() + " »");
        }
        return c.getAsLong();
    }
}
