package com.pingmanager.core;

import com.pingmanager.model.AdresseTournoi;
import com.pingmanager.model.InfosReglement;
import com.pingmanager.model.Tableau;
import com.pingmanager.model.Tournoi;

import java.util.ArrayList;
import java.util.List;

/**
 * Vue de complétude enrichie d'un tournoi, pour l'écran de gestion.
 *
 * <p>Classe de lecture : elle <b>n'altère rien</b> et ne change pas les règles de
 * transition (homologation / publication) portées par {@link ServiceCycleTournoi}.
 * Elle produit une grille lisible — quatre sections, chaque champ avec son niveau
 * d'exigence et son état (renseigné ou manquant) — en s'appuyant sur les champs
 * du modèle et, pour les deux compteurs de blocage, sur le service de cycle.</p>
 *
 * <p>Niveaux : obligatoire pour l'homologation, obligatoire pour la publication,
 * recommandé (non bloquant), optionnel.</p>
 */
public final class ServiceCompletudeTournoi {

    public enum Niveau {
        OBLIGATOIRE_HOMOLOGATION("Obligatoire (homologation)"),
        OBLIGATOIRE_PUBLICATION("Obligatoire (publication)"),
        RECOMMANDE("Recommandé"),
        OPTIONNEL("Optionnel");

        private final String libelle;

        Niveau(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Un champ évalué : son libellé, son niveau d'exigence, et s'il est renseigné. */
    public record Item(String libelle, Niveau niveau, boolean renseigne) {
    }

    /** Une section d'affichage avec ses items. */
    public record Section(String titre, List<Item> items) {
    }

    private final ServiceCycleTournoi cycle = new ServiceCycleTournoi();

    /** @return les quatre sections, dans l'ordre d'affichage. */
    public List<Section> sections(Tournoi t) {
        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Général", general(t)));
        sections.add(new Section("Tableaux et inscriptions", tableaux(t)));
        sections.add(new Section("Informations sportives et techniques", sportif(t)));
        sections.add(new Section("Complémentaire", complementaire(t)));
        return sections;
    }

    /** @return le nombre d'éléments obligatoires encore manquants pour l'homologation. */
    public int bloquantsHomologation(Tournoi t) {
        return cycle.elementsManquantsPourHomologation(t).size();
    }

    /** @return le nombre d'éléments obligatoires encore manquants pour la publication. */
    public int bloquantsPublication(Tournoi t) {
        return cycle.elementsManquantsPourPublication(t).size();
    }

    // ------------------------------------------------------------------
    // Sections
    // ------------------------------------------------------------------

    private List<Item> general(Tournoi t) {
        List<Item> l = new ArrayList<>();
        l.add(item("Nom du tournoi", Niveau.OBLIGATOIRE_HOMOLOGATION, nonVide(t.getNom())));
        // Catégorie IX.102 (remplace l'ancien « type ») ; elle fixe l'échelon
        // d'homologation (IX.103), qui n'est donc plus un champ à remplir.
        l.add(item("Niveau du tournoi (catégorie)", Niveau.OBLIGATOIRE_HOMOLOGATION, t.getNiveau() != null));
        l.add(item("Numéro d'homologation", Niveau.OBLIGATOIRE_PUBLICATION, nonVide(t.getNumeroHomologation())));
        l.add(item("Club organisateur", Niveau.OBLIGATOIRE_HOMOLOGATION, nonVide(t.getClubOrganisateur())));
        l.add(item("N° d'affiliation du club", Niveau.OBLIGATOIRE_HOMOLOGATION, nonVide(t.getNumeroAffiliationClub())));
        l.add(item("Lieu (voie, code postal, ville)", Niveau.OBLIGATOIRE_HOMOLOGATION, adresseComplete(t)));
        l.add(item("Date de début", Niveau.OBLIGATOIRE_HOMOLOGATION, t.getDateDebut() != null));
        l.add(item("Date de fin", Niveau.OBLIGATOIRE_HOMOLOGATION, t.getDateFin() != null));
        l.add(item("Correspondant (nom, prénom, email)", Niveau.OBLIGATOIRE_HOMOLOGATION, correspondantComplet(t)));
        l.add(item("Correspondant : téléphone", Niveau.OPTIONNEL, nonVide(t.getCorrespondantTelephone())));
        l.add(item("Phase de référence des points", Niveau.RECOMMANDE, t.getPhasePoints() != null));
        return l;
    }

    private List<Item> tableaux(Tournoi t) {
        List<Item> l = new ArrayList<>();
        boolean auMoinsUn = !t.getTableaux().isEmpty();
        l.add(item("Au moins un tableau", Niveau.OBLIGATOIRE_HOMOLOGATION, auMoinsUn));
        l.add(item("Au moins un tableau réservé aux dames (IX.104)",
                Niveau.OBLIGATOIRE_HOMOLOGATION, auMoinsUnTableauDames(t)));
        l.add(item("Chaque tableau : format et capacité",
                Niveau.OBLIGATOIRE_PUBLICATION, auMoinsUn && tousFormatEtCapacite(t)));
        l.add(item("Tarif d'engagement par tableau",
                Niveau.OBLIGATOIRE_PUBLICATION, auMoinsUn)); // un tarif existe toujours (gratuit par défaut)
        l.add(item("Récompenses annoncées", Niveau.RECOMMANDE, auMoinsUneRecompense(t)));
        l.add(item("Ouverture des inscriptions",
                Niveau.OBLIGATOIRE_PUBLICATION, infos(t) != null && infos(t).getOuvertureInscriptions() != null));
        l.add(item("Clôture des inscriptions",
                Niveau.OBLIGATOIRE_PUBLICATION, infos(t) != null && infos(t).getClotureInscriptions() != null));
        l.add(item("Chaque tableau : jour et heure de début",
                Niveau.OBLIGATOIRE_HOMOLOGATION, auMoinsUn && tousDates(t)));
        l.add(item("Remboursement en cas de forfait (3 cas)", Niveau.RECOMMANDE, t.getPolitiqueForfaits().estComplete()));
        l.add(item("Chaque tableau : heures de pointage",
                Niveau.RECOMMANDE, auMoinsUn && tousPointage(t)));
        return l;
    }

    private List<Item> sportif(Tournoi t) {
        List<Item> l = new ArrayList<>();
        boolean auMoinsUnJa = !t.getJugesArbitres().isEmpty();
        l.add(item("Juge-arbitre désigné", Niveau.OBLIGATOIRE_HOMOLOGATION, auMoinsUnJa));
        l.add(item("Grade du juge-arbitre conforme à la catégorie (IX.104)",
                Niveau.OBLIGATOIRE_HOMOLOGATION, auMoinsUnJa && ServiceCycleTournoi.gradeJugeArbitreConforme(t)));
        l.add(item("Arbitres", Niveau.OPTIONNEL, !t.getArbitres().isEmpty()));
        l.add(item("Nombre de tables", Niveau.OBLIGATOIRE_PUBLICATION, t.getNombreDeTables() > 0));
        l.add(item("Tables : marque et couleur", Niveau.OPTIONNEL, !t.getLotsTables().isEmpty()));
        l.add(item("Provenance des balles",
                Niveau.OBLIGATOIRE_PUBLICATION, infos(t) != null && infos(t).getPolitiqueBalles() != null));
        l.add(item("Balles : couleur et marque", Niveau.RECOMMANDE,
                infos(t) != null && infos(t).getCouleurBalles() != null && nonVide(infos(t).getMarqueBalles())));
        l.add(item("Aire de jeu (dimensions, éclairage)",
                Niveau.OPTIONNEL, infos(t) != null && infos(t).getAireDeJeu() != null));
        return l;
    }

    private List<Item> complementaire(Tournoi t) {
        List<Item> l = new ArrayList<>();
        l.add(item("Règlement du tournoi (déposé ou généré)",
                Niveau.OBLIGATOIRE_PUBLICATION, t.aUnReglement()));
        l.add(item("Condition : licence promotionnelle acceptée",
                Niveau.RECOMMANDE, true)); // valeur toujours définie (oui/non)
        l.add(item("Condition : joueurs étrangers autorisés",
                Niveau.RECOMMANDE, true));
        l.add(item("Paiement en ligne (lien)", Niveau.RECOMMANDE, t.getPaiement().estPret()));
        l.add(item("Image promotionnelle", Niveau.OPTIONNEL, t.getImagePromotionnelle() != null));
        l.add(item("Site web", Niveau.OPTIONNEL, nonVide(t.getSiteWeb())));
        return l;
    }

    // ------------------------------------------------------------------
    // Prédicats
    // ------------------------------------------------------------------

    private static InfosReglement infos(Tournoi t) {
        return t.getInfosReglement();
    }

    private static boolean adresseComplete(Tournoi t) {
        AdresseTournoi a = t.getAdresse();
        return a != null && a.estComplete();
    }

    private static boolean correspondantComplet(Tournoi t) {
        return nonVide(t.getCorrespondantNom())
                && nonVide(t.getCorrespondantPrenom())
                && nonVide(t.getCorrespondantEmail());
    }

    private static boolean auMoinsUnTableauDames(Tournoi t) {
        for (Tableau tab : t.getTableaux()) {
            if (tab.getConfiguration() != null
                    && tab.getConfiguration().getSexeRequis() == com.pingmanager.model.Sexe.FEMME) {
                return true;
            }
        }
        return false;
    }

    private static boolean tousFormatEtCapacite(Tournoi t) {
        for (Tableau tab : t.getTableaux()) {
            var c = tab.getConfiguration();
            if (c == null || c.getFormat() == null || !c.aLimiteDePlaces()) {
                return false;
            }
        }
        return true;
    }

    private static boolean tousDates(Tournoi t) {
        return t.getTableaux().stream()
                .allMatch(tab -> tab.getHoraires() != null && tab.getHoraires().getDebutMatchs() != null);
    }

    private static boolean tousPointage(Tournoi t) {
        return t.getTableaux().stream().allMatch(tab -> tab.getHoraires() != null
                && tab.getHoraires().getDebutPointage() != null && tab.getHoraires().getFinPointage() != null);
    }

    private static boolean auMoinsUneRecompense(Tournoi t) {
        for (Tableau tab : t.getTableaux()) {
            if (tab.getConfiguration() != null
                    && tab.getConfiguration().getRecompenses() != null
                    && !tab.getConfiguration().getRecompenses().estVide()) {
                return true;
            }
        }
        return false;
    }

    private static boolean nonVide(String s) {
        return s != null && !s.isBlank();
    }

    private static Item item(String libelle, Niveau niveau, boolean renseigne) {
        return new Item(libelle, niveau, renseigne);
    }
}
