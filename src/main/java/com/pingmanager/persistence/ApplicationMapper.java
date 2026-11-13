package com.pingmanager.persistence;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.AgendaCompte;
import com.pingmanager.model.EvenementAgenda;
import com.pingmanager.model.OrigineEvenement;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;
import com.pingmanager.model.CompteUtilisateur;
import com.pingmanager.model.RoleCompte;
import com.pingmanager.model.NiveauAcces;
import com.pingmanager.persistence.json.Json;

/**
 * Traduit les entités d'<b>application</b> — clubs, comptes organisateurs et
 * accès — entre le modèle et le JSON.
 *
 * <p>Ces données vivent entre les tournois (référentiel des clubs, qui peut
 * administrer quoi) et relèvent donc d'un stockage distinct du document de
 * tournoi. Ce sont des classes de données pures, reconstruites par leurs
 * constructeurs et setters publics, sans garde de cycle de vie à contourner.</p>
 *
 * <p>Le mot de passe n'apparaît jamais en clair : le compte ne porte qu'une
 * <b>empreinte</b> (hash) produite ailleurs, qu'on se contente de transporter.</p>
 */
final class ApplicationMapper {

    private ApplicationMapper() {
    }

    // ------------------------------------------------------------------
    // Club
    // ------------------------------------------------------------------

    static Json ecrireClub(Club c) {
        Json o = Json.objet();
        o.set("numero", c.getNumero());
        o.set("nom", c.getNom());
        o.set("departement", c.getDepartement());
        o.set("ville", c.getVille());
        o.set("emailOfficiel", c.getEmailOfficiel());
        if (c.getAdresse1() != null) {
            o.set("adresse1", c.getAdresse1());
        }
        if (c.getAdresse2() != null) {
            o.set("adresse2", c.getAdresse2());
        }
        if (c.getCodePostal() != null) {
            o.set("codePostal", c.getCodePostal());
        }
        if (c.getResponsableNom() != null) {
            o.set("responsableNom", c.getResponsableNom());
        }
        if (c.getResponsablePrenom() != null) {
            o.set("responsablePrenom", c.getResponsablePrenom());
        }
        return o;
    }

    static Club lireClub(Json o) {
        Club c = new Club(
                o.get("numero").asString(),
                o.get("nom").asString(),
                o.get("departement").asString(),
                o.get("ville").asString(),
                o.get("emailOfficiel").asString());
        c.setAdresse1(o.getString("adresse1", null));
        c.setAdresse2(o.getString("adresse2", null));
        c.setCodePostal(o.getString("codePostal", null));
        c.setResponsableNom(o.getString("responsableNom", null));
        c.setResponsablePrenom(o.getString("responsablePrenom", null));
        return c;
    }

    // ------------------------------------------------------------------
    // CompteOrganisateur
    // ------------------------------------------------------------------

    static Json ecrireCompte(CompteOrganisateur c) {
        Json o = Json.objet();
        o.set("email", c.getEmail());
        o.set("nom", c.getNom());
        o.set("prenom", c.getPrenom());
        o.set("empreinteMotDePasse", c.getEmpreinteMotDePasse());
        o.set("numeroClub", c.getNumeroClub());
        o.set("emailVerifie", c.isEmailVerifie());
        return o;
    }

    static CompteOrganisateur lireCompte(Json o) {
        CompteOrganisateur c = new CompteOrganisateur(
                o.get("email").asString(),
                o.get("nom").asString(),
                o.get("prenom").asString(),
                o.get("empreinteMotDePasse").asString(),
                o.get("numeroClub").asString());
        if (o.getBoolean("emailVerifie", false)) {
            c.marquerEmailVerifie();
        }
        return c;
    }

    // ------------------------------------------------------------------
    // AccesClub
    // ------------------------------------------------------------------

    static Json ecrireAcces(AccesClub a) {
        Json o = Json.objet();
        o.set("numeroClub", a.getNumeroClub());
        o.set("email", a.getEmail());
        if (a.getNom() != null) {
            o.set("nom", a.getNom());
        }
        if (a.getPrenom() != null) {
            o.set("prenom", a.getPrenom());
        }
        o.set("niveau", a.getNiveau().name());
        return o;
    }

    static AccesClub lireAcces(Json o) {
        NiveauAcces niveau = lireNiveau(o.getString("niveau", null));
        return new AccesClub(
                o.get("numeroClub").asString(),
                o.get("email").asString(),
                o.getString("nom", null),
                o.getString("prenom", null),
                niveau);
    }

    private static NiveauAcces lireNiveau(String valeur) {
        if (valeur == null) {
            return NiveauAcces.ADMIN_NIVEAU_1; // ancien fichier : niveau par défaut
        }
        try {
            return NiveauAcces.valueOf(valeur);
        } catch (IllegalArgumentException e) {
            return NiveauAcces.ADMIN_NIVEAU_1;
        }
    }

    // ------------------------------------------------------------------
    // CompteUtilisateur
    // ------------------------------------------------------------------

    static Json ecrireCompteUtilisateur(CompteUtilisateur c) {
        Json o = Json.objet();
        o.set("email", c.getEmail());
        o.set("nom", c.getNom());
        o.set("prenom", c.getPrenom());
        o.set("empreinteMotDePasse", c.getEmpreinteMotDePasse());
        if (c.getLicenceLiee() != null) {
            o.set("licenceLiee", c.getLicenceLiee());
        }
        o.set("role", c.getRole().name());
        o.set("emailVerifie", c.isEmailVerifie());
        return o;
    }

    /**
     * Email de l'ancien compte de supervision : jusqu'à la v19 de la démo, le rôle était
     * déduit de cette adresse. Ne sert plus qu'à relire, une dernière fois, un ancien
     * fichier dont les comptes n'ont pas de champ « role ».
     */
    static final String EMAIL_SUPERVISION_ANCIENS_FICHIERS = "admin@pingmanager.fr";

    /**
     * Rôle relu : celui du champ « role » ; à défaut (ancien fichier), celui que le compte
     * avait (déduit de l'email, comme avant) ; valeur inconnue : {@link RoleCompte#MEMBRE}
     * (le moins de droits).
     */
    static RoleCompte lireRole(Json o, String email) {
        String role = o.getString("role", null);
        if (role == null || role.isBlank()) {
            return EMAIL_SUPERVISION_ANCIENS_FICHIERS.equalsIgnoreCase(email)
                    ? RoleCompte.SUPERVISION : RoleCompte.MEMBRE;
        }
        for (RoleCompte r : RoleCompte.values()) {
            if (r.name().equals(role)) {
                return r;
            }
        }
        return RoleCompte.MEMBRE;
    }

    /** Absent : vrai (ancien compte actif) ; valeur d'un autre type : faux (le moins de droits). */
    static boolean lireEmailVerifie(Json o) {
        try {
            return o.getBoolean("emailVerifie", true);
        } catch (com.pingmanager.persistence.json.JsonException typeInattendu) {
            return false;
        }
    }

    static CompteUtilisateur lireCompteUtilisateur(Json o) {
        CompteUtilisateur c = new CompteUtilisateur(
                o.get("email").asString(),
                o.get("nom").asString(),
                o.get("prenom").asString(),
                o.get("empreinteMotDePasse").asString());
        String licence = o.getString("licenceLiee", null);
        if (licence != null && !licence.isBlank()) {
            c.lierLicence(licence);
        }
        c.setRole(lireRole(o, c.getEmail()));
        // Point 13 : un compte d'un fichier antérieur (sans le champ) était déjà actif ;
        // il reste actif, considéré vérifié. Un compte jamais confirmé porte « false ».
        if (lireEmailVerifie(o)) {
            c.marquerEmailVerifie();
        }
        return c;
    }

    // ------------------------------------------------------------------
    // AgendaCompte (champ « agendas », facultatif : anciens fichiers relus sans)
    // ------------------------------------------------------------------

    static Json ecrireAgenda(AgendaCompte a) {
        Json o = Json.objet();
        o.set("email", a.getEmail());
        Json evts = Json.tableau();
        for (EvenementAgenda e : a.evenements()) {
            Json j = Json.objet();
            j.set("id", e.getId());
            j.set("titre", e.getTitre());
            j.set("date", e.getDate().toString());
            if (e.estSurPlusieursJours()) {
                j.set("dateFin", e.getDateFin().toString());
            }
            if (e.getHeure() != null) {
                j.set("heure", e.getHeure().toString());
            }
            if (e.getLieu() != null) {
                j.set("lieu", e.getLieu());
            }
            if (e.getNote() != null) {
                j.set("note", e.getNote());
            }
            j.set("origine", e.getOrigine().name());
            if (e.getCleTournoi() != null) {
                j.set("cleTournoi", e.getCleTournoi());
            }
            evts.add(j);
        }
        o.set("evenements", evts);
        return o;
    }

    static AgendaCompte lireAgenda(Json o) {
        AgendaCompte a = new AgendaCompte(o.get("email").asString());
        if (o.has("evenements")) {
            for (Json j : o.get("evenements").elements()) {
                String fin = j.getString("dateFin", null);
                String heure = j.getString("heure", null);
                String cle = j.getString("cleTournoi", null);
                OrigineEvenement origine = lireOrigine(j.getString("origine", null), cle);
                a.enregistrer(EvenementAgenda.builder()
                        .id(j.get("id").asString())
                        .titre(j.get("titre").asString())
                        .date(java.time.LocalDate.parse(j.get("date").asString()))
                        .dateFin(fin == null ? null : java.time.LocalDate.parse(fin))
                        .heure(heure == null ? null : java.time.LocalTime.parse(heure))
                        .lieu(j.getString("lieu", null))
                        .note(j.getString("note", null))
                        .origine(origine)
                        .cleTournoi(cle)
                        .construire());
            }
        }
        return a;
    }

    /** Origine inconnue ou date de tournoi sans clé : relue comme date personnelle. */
    private static OrigineEvenement lireOrigine(String valeur, String cle) {
        if ("TOURNOI".equals(valeur) && cle != null && !cle.isBlank()) {
            return OrigineEvenement.TOURNOI;
        }
        if ("FEDERAL".equals(valeur)) {
            return OrigineEvenement.FEDERAL; // B045 : repère du calendrier fédéral
        }
        return OrigineEvenement.PERSONNEL;
    }
}
