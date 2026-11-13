package com.pingmanager.core;

import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Export de l'agenda au format iCalendar ({@code .ics}, RFC 5545), écrit à la main
 * sans dépendance : importable dans Google Agenda, Outlook ou Apple Calendrier.
 *
 * <ul>
 *   <li>lignes terminées par CRLF, y compris la dernière (RFC 5545, 3.1) ;</li>
 *   <li>lignes de plus de 75 octets UTF-8 pliées (CRLF + espace), sans couper
 *       un caractère multi-octets ;</li>
 *   <li>textes échappés : {@code \\}, {@code \;}, {@code \,} et {@code \n} (3.3.11) ;</li>
 *   <li>{@code UID} stable (identifiant de la date) et {@code DTSTAMP} en UTC
 *       (heure fournie par le port {@link Horloge}) ;</li>
 *   <li>date sans heure : journée entière ({@code VALUE=DATE}, fin exclusive au
 *       lendemain du dernier jour) ; date avec heure : heure locale « flottante »,
 *       interprétée dans le fuseau de l'agenda qui importe.</li>
 * </ul>
 *
 * <p>Le résultat est un texte ; l'écriture du fichier (en UTF-8) revient à l'appelant.</p>
 */
public final class ExportIcs {

    /** Fin de ligne imposée par la RFC 5545. */
    public static final String CRLF = "\r\n";

    /** Longueur maximale d'une ligne, en octets, hors CRLF. */
    public static final int OCTETS_PAR_LIGNE = 75;

    /** Domaine ajouté aux identifiants pour former un UID globalement unique. */
    static final String DOMAINE_UID = "pingmanager";

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HEURE_LOCALE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter HEURE_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final Horloge horloge;

    public ExportIcs(Horloge horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    /** Nom de fichier conseillé pour l'export. */
    public static String nomFichier() {
        return "agenda-pingmanager.ics";
    }

    /** @return le calendrier complet (toutes les dates de l'agenda). */
    public String exporter(AgendaCompte agenda) {
        return exporter(agenda, e -> true);
    }

    /** @return le calendrier des seules dates de tournoi (choix « seulement les tournois » de l'Agenda). */
    public String exporterTournois(AgendaCompte agenda) {
        return exporter(agenda, EvenementAgenda::estAutomatique);
    }

    /** @return le calendrier des dates de l'agenda retenues par {@code filtre}. */
    public String exporter(AgendaCompte agenda, Predicate<EvenementAgenda> filtre) {
        Objects.requireNonNull(agenda, "agenda");
        Objects.requireNonNull(filtre, "filtre");
        Instant maintenant = horloge.maintenant();
        StringBuilder sb = new StringBuilder();
        ligne(sb, "BEGIN:VCALENDAR");
        ligne(sb, "VERSION:2.0");
        ligne(sb, "PRODID:-//PingManager//Agenda//FR");
        ligne(sb, "CALSCALE:GREGORIAN");
        ligne(sb, "METHOD:PUBLISH");
        ligne(sb, "X-WR-CALNAME:" + echapper("Agenda PingManager"));
        for (EvenementAgenda e : agenda.evenements()) {
            if (!filtre.test(e)) {
                continue;
            }
            evenement(sb, e, maintenant);
        }
        ligne(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static void evenement(StringBuilder sb, EvenementAgenda e, Instant maintenant) {
        ligne(sb, "BEGIN:VEVENT");
        ligne(sb, "UID:" + echapper(e.getId() + "@" + DOMAINE_UID));
        ligne(sb, "DTSTAMP:" + HEURE_UTC.format(maintenant));
        if (e.getHeure() == null) {
            ligne(sb, "DTSTART;VALUE=DATE:" + JOUR.format(e.getDate()));
            ligne(sb, "DTEND;VALUE=DATE:" + JOUR.format(e.getDateFin().plusDays(1)));
        } else {
            ligne(sb, "DTSTART:" + HEURE_LOCALE.format(e.getDate().atTime(e.getHeure())));
            if (e.estSurPlusieursJours()) {
                ligne(sb, "DTEND:" + HEURE_LOCALE.format(e.getDateFin().plusDays(1).atStartOfDay()));
            }
        }
        ligne(sb, "SUMMARY:" + echapper(e.getTitre()));
        if (e.getLieu() != null) {
            ligne(sb, "LOCATION:" + echapper(e.getLieu()));
        }
        if (e.getNote() != null) {
            ligne(sb, "DESCRIPTION:" + echapper(e.getNote()));
        }
        ligne(sb, "CATEGORIES:" + categorie(e));
        ligne(sb, "TRANSP:" + (e.getHeure() == null ? "TRANSPARENT" : "OPAQUE"));
        ligne(sb, "END:VEVENT");
    }

    private static String categorie(EvenementAgenda e) {
        if (e.estAutomatique()) {
            return "Tournoi";
        }
        if (e.getOrigine() == OrigineEvenement.FEDERAL) {
            return "Fédéral";
        }
        return "Personnel";
    }

    /**
     * Échappe une valeur de type TEXT (RFC 5545, 3.3.11) : barre oblique inverse,
     * point-virgule, virgule et retours à la ligne ({@code \n}).
     */
    public static String echapper(String texte) {
        if (texte == null) {
            return "";
        }
        String t = texte.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(t.length() + 8);
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == ';') {
                sb.append("\\;");
            } else if (c == ',') {
                sb.append("\\,");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c < 0x20 && c != '\t') {
                // caractère de contrôle interdit dans une valeur TEXT : ignoré
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Plie une ligne de contenu à {@value #OCTETS_PAR_LIGNE} octets UTF-8 (RFC 5545,
     * 3.1) : chaque suite commence par une espace, qui compte dans la limite. Un
     * caractère (y compris une paire de substitution) n'est jamais coupé.
     *
     * @return la ligne pliée, sans CRLF final
     */
    public static String plier(String ligne) {
        StringBuilder sb = new StringBuilder(ligne.length() + 8);
        int octets = 0;
        int i = 0;
        while (i < ligne.length()) {
            int cp = ligne.codePointAt(i);
            int n = Character.charCount(cp);
            int taille = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (octets + taille > OCTETS_PAR_LIGNE) {
                sb.append(CRLF).append(' ');
                octets = 1;
            }
            sb.appendCodePoint(cp);
            octets += taille;
            i += n;
        }
        return sb.toString();
    }

    private static void ligne(StringBuilder sb, String contenu) {
        sb.append(plier(contenu)).append(CRLF);
    }
}
