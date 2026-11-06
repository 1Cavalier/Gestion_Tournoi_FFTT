package com.pingmanager.core;

import java.util.Objects;

/**
 * Statut d'un tournoi dans la <b>carte des tournois</b> (carte et agenda), pour le
 * licencié connecté. Quatre cas, distingués à l'écran par une pastille, une couleur et
 * un texte (cahier des charges v0, section 3) :
 * <ol>
 *   <li>{@link Statut#EN_COURS} : le tournoi se joue aujourd'hui ;</li>
 *   <li>{@link Statut#A_VENIR} : on peut y participer ;</li>
 *   <li>{@link Statut#NON_ACCESSIBLE} : on ne peut pas y participer (IX.101 / IX.102),
 *       avec le motif d'{@link EligibiliteTournoi} ;</li>
 *   <li>{@link Statut#COMPLET_OU_HORS_NIVEAU} : accessible en principe, mais aucun
 *       tableau n'est ouvert à son niveau, ou tous ceux qui le sont sont complets.</li>
 * </ol>
 *
 * <p>Ordre de priorité : en cours, puis non accessible, puis aucun tableau à son
 * niveau, puis tableaux complets, sinon à venir. La fiche du tournoi s'ouvre dans
 * tous les cas : ce statut n'empêche que l'inscription.</p>
 *
 * <p>Pas de {@code switch} sur les énumérations (voir CLAUDE.md).</p>
 */
public final class StatutCarteTournoi {

    /** Les quatre statuts de la carte des tournois. */
    public enum Statut { EN_COURS, A_VENIR, NON_ACCESSIBLE, COMPLET_OU_HORS_NIVEAU }

    /** Statut et motif en clair ({@code null} quand rien n'empêche de s'inscrire). */
    public record Verdict(Statut statut, String motif) {
        public Verdict {
            Objects.requireNonNull(statut, "statut");
        }

        /** @return vrai si le joueur ne peut pas s'inscrire (deux derniers statuts). */
        public boolean estBloquant() {
            return statut == Statut.NON_ACCESSIBLE || statut == Statut.COMPLET_OU_HORS_NIVEAU;
        }
    }

    /** Motif quand aucun tableau n'est ouvert aux points ou à la catégorie du joueur. */
    public static final String MOTIF_HORS_NIVEAU = "Aucun tableau à votre niveau (points ou catégorie).";
    /** Motif quand tous les tableaux ouverts au joueur sont complets. */
    public static final String MOTIF_COMPLET = "Tableaux à votre niveau complets (liste d'attente éventuelle).";

    private StatutCarteTournoi() {
    }

    /** @return le statut d'une entrée du catalogue. */
    public static Verdict de(CatalogueTournois.Entree e) {
        Objects.requireNonNull(e, "entree");
        if (e.moment() == CalendrierTournoi.Moment.EN_COURS) {
            return new Verdict(Statut.EN_COURS, null);
        }
        if (e.acces() != null && e.acces().estRefuse()) {
            return new Verdict(Statut.NON_ACCESSIBLE, e.acces().motif());
        }
        if (!e.tableauAMesPoints()) {
            return new Verdict(Statut.COMPLET_OU_HORS_NIVEAU, MOTIF_HORS_NIVEAU);
        }
        if (e.tableauxAMonNiveauComplets()) {
            return new Verdict(Statut.COMPLET_OU_HORS_NIVEAU, MOTIF_COMPLET);
        }
        return new Verdict(Statut.A_VENIR, null);
    }
}
