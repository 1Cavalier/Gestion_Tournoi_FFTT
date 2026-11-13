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
 * Documents déposés par l'organisateur (PDF du règlement, image promotionnelle
 * PNG ou JPEG depuis B044), copiés dans le
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
 * <p>Anciens fichiers de tournoi (chemin absolu) : {@link #migrerReglement} et
 * {@link #migrerImage}
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
        return deposer(source, libelle, null);
    }

    /**
     * Copie l'image {@code source} (PNG ou JPEG, reconnue à son contenu) dans le dossier
     * des documents (reliquat du point 18).
     *
     * @return la référence relative créée, ou le motif du refus (rien n'est copié)
     */
    public ResultatDepotDocument deposerImage(Path source, String libelle) {
        if (source == null || !Files.isRegularFile(source)) {
            return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE,
                    "Fichier introuvable.");
        }
        String extension;
        try {
            extension = extensionImage(source);
        } catch (IOException e) {
            extension = null;
        }
        if (extension == null) {
            return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.PAS_UNE_IMAGE,
                    "Ce fichier n'est pas une image PNG ou JPEG.");
        }
        return deposer(source, libelle, extension);
    }

    /** @param extensionImage {@code null} pour un PDF, sinon l'extension de l'image reconnue */
    private ResultatDepotDocument deposer(Path source, String libelle, String extensionImage) {
        if (source == null || !Files.isRegularFile(source)) {
            return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.FICHIER_INTROUVABLE,
                    "Fichier introuvable.");
        }
        try {
            if (Files.size(source) > TAILLE_MAX_OCTETS) {
                return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.TROP_VOLUMINEUX,
                        "Fichier trop volumineux (plus de " + TAILLE_MAX_OCTETS / (1024 * 1024) + " Mo).");
            }
            if (extensionImage == null && !commeUnPdf(source)) {
                return ResultatDepotDocument.refuse(ResultatDepotDocument.Motif.PAS_UN_PDF,
                        "Ce fichier n'est pas un document PDF.");
            }
            String nom = nomStocke(source, extensionImage == null ? ".pdf" : extensionImage);
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

    /**
     * Reliquat du point 18 : même migration pour l'image promotionnelle d'un ancien
     * fichier (chemin absolu). L'image est copiée si elle existe encore et est bien une
     * image ; sinon la référence reste telle quelle, jamais ouverte (« image introuvable »).
     *
     * @return vrai si le tournoi a changé (à enregistrer)
     */
    public boolean migrerImage(Tournoi tournoi) {
        DocumentReference ref = tournoi.getImagePromotionnelle();
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
            return false;
        }
        ResultatDepotDocument r = deposerImage(ancien, ref.getLibelle());
        if (!r.estDepose()) {
            return false;
        }
        tournoi.setImagePromotionnelle(r.getReference());
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
        String extension = extensionAutorisee(nom);
        return !nom.isEmpty() && extension != null && nom.equals(assainir(nom, extension));
    }

    /** Extensions des documents stockés : le PDF du règlement et les images. */
    private static final java.util.List<String> EXTENSIONS = java.util.List.of(".pdf", ".png", ".jpg");

    private static String extensionAutorisee(String nom) {
        for (String e : EXTENSIONS) {
            if (nom.endsWith(e)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Nom de fichier sûr : dernier segment seulement, lettres sans accents,
     * chiffres, « - », « _ » et « . » isolés ; pas de point initial, pas de
     * « .. », extension {@code .pdf}, longueur bornée.
     */
    static String assainir(String nom) {
        return assainir(nom, ".pdf");
    }

    /** {@link #assainir(String)} avec l'extension imposée ({@code .pdf}, {@code .png}, {@code .jpg}). */
    static String assainir(String nom, String extension) {
        String s = nom == null ? "" : nom;
        int i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        s = s.substring(i + 1);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.{2,}", ".");
        String[] suffixes = ".jpg".equals(extension) ? new String[] {".jpg", ".jpeg"} : new String[] {extension};
        for (String e : suffixes) {
            if (s.toLowerCase(Locale.ROOT).endsWith(e)) {
                s = s.substring(0, s.length() - e.length());
                break;
            }
        }
        s = s.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (s.length() > LONGUEUR_MAX_NOM) {
            s = s.substring(0, LONGUEUR_MAX_NOM).replaceAll("[._-]+$", "");
        }
        return (s.isEmpty() ? (".pdf".equals(extension) ? "reglement" : "image") : s) + extension;
    }

    private static String nomStocke(Path source, String extension) throws IOException {
        String base = assainir(source.getFileName().toString(), extension);
        base = base.substring(0, base.length() - extension.length());
        return base + "-" + empreinte(source) + extension;
    }

    /** Signature PNG ou JPEG du contenu : {@code .png}, {@code .jpg}, sinon {@code null}. */
    private static String extensionImage(Path source) throws IOException {
        byte[] d = new byte[8];
        int lus;
        try (InputStream in = Files.newInputStream(source)) {
            lus = in.readNBytes(d, 0, d.length);
        }
        if (lus >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return ".png";
        }
        if (lus >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        return null;
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
