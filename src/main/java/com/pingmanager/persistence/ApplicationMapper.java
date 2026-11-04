package com.pingmanager.persistence;

import com.pingmanager.model.AccesClub;
import com.pingmanager.model.Club;
import com.pingmanager.model.CompteOrganisateur;
import com.pingmanager.model.CompteUtilisateur;
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
        return o;
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
        return c;
    }
}
