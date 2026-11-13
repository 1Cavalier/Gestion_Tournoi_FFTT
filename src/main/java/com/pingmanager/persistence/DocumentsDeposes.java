package com.pingmanager.persistence;

import com.pingmanager.model.DocumentReference;
import com.pingmanager.model.Tournoi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Documents déposés par l'organisateur (PDF du règlement), copiés dans le
 * dossier de données de l'application ({@value #DOSSIER}/) au lieu d'être
 * référencés par un chemin absolu libre (README section 5, point 18).
 *
 * <p>Le tournoi ne garde qu'un emplacement <b>relatif</b>
 * {@code documents/<nom-assaini>-<empreinte>.pdf}. À la lecture, seul un
 * emplacement de cette forme exacte est résolu, et toujours à l'intérieur du
 * dossier : pas de « .. », pas de séparateur, pas de chemin absolu. Le nom
 * contient une empreinte du contenu : déposer deux fois le même fichier ne
 * crée qu'une copie.</p>
 *
 * <p>Anciens fichiers de tournoi (chemin absolu) : {@link #migrerReglement}
 * copie le PDF s'il existe encore ; sinon la référence est laissée telle quelle
 * (jamais ouverte) et le règlement apparaît « fichier introuvable ».</p>
 */
public final class DocumentsDeposes {

    /** Sous-dossier du dossier de données, et préfixe des emplacements stockés. */
    public static final String DOSSIER = "documents";

    /** Taille maximale d'un PDF déposé (50 Mo). */
    public static final long TAILLE_MAX_OCTETS = 50L * 1024 * 1024;

    private static final int LONGUEUR_MAX_NOM = 80;

    private final Path dossier;

    /** @param dossierDonnees dossier de données de l'application */
    public DocumentsDeposes(Path dossierDonnees) {
        this.dossier = dossierDonnees.resolve(DOSSIER).toAbsolutePath().normalize();
    }

    /**
     * Copie le PDF {@code source} dans le dossier des documents.
     *
     * @return la référence relative créée, ou le motif du refus (rien n'est copié)
     */
    public ResultatDepotDocument deposer(Path source, String libelle) {
        if (source == null || !Files.isRegularFile(source)) {
            return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE,
                    "Fichier introuvable.");
        }
        try {
            if (Files.size(source) > TAILLE_MAX_OCTETS) {
                return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.TROP_VOLUMINEUX,
                        "Fichier trop volumineux (plus de " + TAILLE_MAX_OCTETS / (1024 * 1024) + " Mo).");
            }
            if (!commeUnPdf(source)) {
                return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.PAS_UN_PDF,
                        "Ce fichier n'est pas un document PDF.");
            }
            String nom = nomStocke(source);
            Files.createDirectories(dossier);
            Path cible = dossier.resolve(nom);
            if (!Files.isRegularFile(cible)) {
                Path temp = Files.createTempFile(dossier, ".depot-", ".tmp");
                try {
                    Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
                    deplacer(temp, cible);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
            return ResultatDepotDocument.depose(new DocumentReference(libelle, DOSSIER + "/" + nom));
        } catch (IOException | RuntimeException e) {
            return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.ECHEC_COPIE,
                    "Copie du fichier impossible : " + e.getMessage());
        }
    }

    /**
     * Fichier stocké d'une référence, s'il existe. Seuls les emplacements
     * relatifs créés par {@link #deposer} sont résolus : un chemin absolu (ancien
     * fichier non migré) ou un emplacement forgé renvoie {@code Optional.empty()}.
     */
    public Optional<Path> fichier(DocumentReference reference) {
        if (reference == null || !emplacementStocke(reference.getEmplacement())) {
            return Optional.empty();
        }
        Path p = dossier.resolve(reference.getEmplacement().substring(DOSSIER.length() + 1)).normalize();
        if (!p.getParent().equals(dossier) || !Files.isRegularFile(p)) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    /**
     * Ancien fichier de tournoi : si le règlement déposé est un chemin absolu
     * vers un PDF qui existe encore, le copie dans le dossier des documents et
     * remplace la référence (même pour un tournoi publié : le contenu est
     * identique). Sinon ne change rien.
     *
     * @return vrai si le tournoi a changé (à enregistrer)
     */
    public boolean migrerReglement(Tournoi tournoi) {
        DocumentReference ref = tournoi.getReglementDocument();
        if (ref == null || emplacementStocke(ref.getEmplacement())) {
            return false;
        }
        Path ancien;
        try {
            ancien = Path.of(ref.getEmplacement());
        } catch (RuntimeException e) {
            return false;
        }
        if (!ancien.isAbsolute() || !Files.isRegularFile(ancien)) {
            return false; // « fichier introuvable » à l'affichage, référence conservée
        }
        ResultatDepotDocument r = deposer(ancien, ref.getLibelle());
        if (!r.estDepose()) {
            return false;
        }
        tournoi.relocaliserReglementDocument(r.getReference());
        return true;
    }

    /** Nom affichable d'une référence (dernier segment), sans jamais ouvrir le fichier. */
    public static String nomAffiche(DocumentReference reference) {
        if (reference == null) {
            return "";
        }
        String e = reference.getEmplacement();
        int i = Math.max(e.lastIndexOf('/'), e.lastIndexOf('\\'));
        return i < 0 ? e : e.substring(i + 1);
    }

    /** Emplacement de la forme exacte {@code documents/<nom assaini>}. */
    static boolean emplacementStocke(String emplacement) {
        if (emplacement == null || !emplacement.startsWith(DOSSIER + "/")) {
            return false;
        }
        String nom = emplacement.substring(DOSSIER.length() + 1);
        return !nom.isEmpty() && nom.equals(assainir(nom));
    }

    /**
     * Nom de fichier sûr : dernier segment seulement, lettres sans accents,
     * chiffres, « - », « _ » et « . » isolés ; pas de point initial, pas de
     * « .. », extension {@code .pdf}, longueur bornée.
     */
    static String assainir(String nom) {
        String s = nom == null ? "" : nom;
        int i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        s = s.substring(i + 1);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.{2,}", ".");
        if (s.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            s = s.substring(0, s.length() - 4);
        }
        s = s.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (s.length() > LONGUEUR_MAX_NOM) {
            s = s.substring(0, LONGUEUR_MAX_NOM).replaceAll("[._-]+$", "");
        }
        return (s.isEmpty() ? "reglement" : s) + ".pdf";
    }

    private static String nomStocke(Path source) throws IOException {
        String base = assainir(source.getFileName().toString());
        base = base.substring(0, base.length() - 4);
        return base + "-" + empreinte(source) + ".pdf";
    }

    /** 12 premiers caractères hexadécimaux du SHA-256 du contenu. */
    private static String empreinte(Path source) throws IOException {
        try (InputStream in = Files.newInputStream(source)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] tampon = new byte[64 * 1024];
            int n;
            while ((n = in.read(tampon)) > 0) {
                md.update(tampon, 0, n);
            }
            return HexFormat.of().formatHex(md.digest()).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Signature « %PDF- » dans le premier kilo-octet (tolérance de la norme PDF). */
    private static boolean commeUnPdf(Path source) throws IOException {
        byte[] debut = new byte[1024];
        int lus;
        try (InputStream in = Files.newInputStream(source)) {
            lus = in.readNBytes(debut, 0, debut.length);
        }
        String s = new String(debut, 0, lus, java.nio.charset.StandardCharsets.ISO_8859_1);
        return s.contains("%PDF-");
    }

    private static void deplacer(Path temp, Path cible) throws IOException {
        try {
            Files.move(temp, cible, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, cible, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
