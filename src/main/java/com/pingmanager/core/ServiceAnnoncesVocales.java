package com.pingmanager.core;

import com.pingmanager.core.SyntheseVocale.ParametresVoix;
import com.pingmanager.core.SyntheseVocale.ResultatSynthese;
import com.pingmanager.model.Joueur;
import com.pingmanager.model.Match;
import com.pingmanager.model.ModeAnnonce;
import com.pingmanager.model.ReglagesAnnoncesVocales;
import com.pingmanager.model.StatutMatch;
import com.pingmanager.model.Tournoi;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Annonces vocales des rencontres dans la salle (B017) : texte, file d'attente,
 * diffusion par la synthèse vocale, rappel si les joueurs tardent.
 *
 * <ul>
 *   <li><b>Déclenchement</b> : au lancement d'un match si l'annonce choisie
 *       comprend l'appel vocal (« Appel vocal » ou « SMS et appel vocal », B037)
 *       et si le club a activé l'annonce automatique ; ou à la demande
 *       (bouton « Annoncer »), quel que soit le mode.</li>
 *   <li><b>File</b> : une seule annonce dite à la fois, dans l'ordre d'arrivée ;
 *       redemander l'annonce d'une rencontre déjà en attente ne la double pas.</li>
 *   <li><b>Rappel</b> : si le club l'a réglé, la même annonce précédée de
 *       « Rappel : » repart N minutes après avoir été dite, tant que le match
 *       est toujours sur la table et que personne n'a indiqué « Joueurs à la
 *       table ». Un seul rappel par annonce (choix prudent, voir le journal B017).</li>
 *   <li><b>Repli silencieux</b> : si la voix est indisponible ou échoue,
 *       l'annonce est rendue « affichée » avec le motif ; elle ne se perd pas.</li>
 * </ul>
 *
 * <p>Le temps vient du port {@link Horloge} ; la voix du port
 * {@link SyntheseVocale} ; la prononciation des noms du point d'accroche
 * {@link ResolveurPrononciation} (B018). {@link #diffuserSuivante()} bloque le
 * temps de la parole : l'interface l'appelle depuis un fil de fond, les autres
 * méthodes depuis le fil de l'interface. La classe est sûre entre fils.</p>
 */
public final class ServiceAnnoncesVocales {

    private final Horloge horloge;
    private final SyntheseVocale synthese;
    private volatile ResolveurPrononciation resolveur = ResolveurPrononciation.TEXTE_NORMAL;

    /** Verrou de la parole : une seule annonce dite à la fois. */
    private final Object parole = new Object();
    private final Deque<Demande> file = new ArrayDeque<>();
    private final List<Rappel> rappels = new ArrayList<>();
    private AnnonceVocale enCours;
    /** La rencontre en cours de diction a été oubliée pendant la parole : pas de rappel. */
    private boolean enCoursOubliee;

    /** Annonce à dire, avec les réglages du tournoi au moment de la demande. */
    private record Demande(AnnonceVocale annonce, ParametresVoix voix, int diffusions, int rappelMinutes) {
    }

    private record Rappel(Demande demande, Instant echeance) {
    }

    public ServiceAnnoncesVocales(Horloge horloge, SyntheseVocale synthese) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
        this.synthese = Objects.requireNonNull(synthese, "synthese");
    }

    /** Branche la prononciation personnalisée des noms (B018) ; null = texte normal. */
    public void definirResolveur(ResolveurPrononciation r) {
        this.resolveur = r == null ? ResolveurPrononciation.TEXTE_NORMAL : r;
    }

    /** La voix peut-elle parler sur cet ordinateur ? */
    public boolean voixDisponible() {
        try {
            return synthese.estDisponible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Voix installées (vide si inconnu). */
    public List<String> voixInstallees() {
        try {
            return List.copyOf(synthese.voix());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Demandes
    // ------------------------------------------------------------------

    /**
     * Faut-il annoncer d'elle-même une rencontre au lancement ? Oui si l'annonce
     * choisie comprend l'appel vocal et si le club a activé l'annonce
     * automatique. Jamais pour « Rien » ni « SMS ».
     */
    public static boolean annonceAutomatique(Tournoi tournoi, ModeAnnonce mode) {
        return mode != null && mode.avecAppel() && tournoi.getAnnoncesVocales().isAnnonceAutomatique();
    }

    /** Après l'envoi d'un match sur une table : met l'annonce en file si la règle le demande. */
    public ResultatDemandeAnnonce apresLancement(Tournoi tournoi, Match match, int table, ModeAnnonce mode) {
        if (!annonceAutomatique(tournoi, mode)) {
            return ResultatDemandeAnnonce.refusee("annonce vocale automatique non demandée");
        }
        return annoncer(tournoi, match, table);
    }

    /** Annonce à la demande (bouton « Annoncer ») d'une rencontre sur une table. */
    public ResultatDemandeAnnonce annoncer(Tournoi tournoi, Match match, int table) {
        Objects.requireNonNull(tournoi, "tournoi");
        if (match == null) {
            return ResultatDemandeAnnonce.refusee("Aucune rencontre à annoncer.");
        }
        if (table < 1) {
            return ResultatDemandeAnnonce.refusee("La rencontre n'est sur aucune table.");
        }
        if (match.getStatut() == StatutMatch.TERMINE) {
            return ResultatDemandeAnnonce.refusee("Cette rencontre est terminée : rien à annoncer.");
        }
        if (inconnu(match.getJoueurA()) || inconnu(match.getJoueurB())) {
            return ResultatDemandeAnnonce.refusee("Les deux joueurs ne sont pas encore connus.");
        }
        ReglagesAnnoncesVocales r = tournoi.getAnnoncesVocales();
        ContexteConvocation contexte = r.isAvecTableauEtTour()
                ? ContexteConvocation.de(tournoi, match).orElse(null) : null;
        AnnonceVocale annonce = TexteAnnonceVocale.composer(match, table, contexte);
        Demande d = new Demande(annonce, new ParametresVoix(r.getVoix(), r.getDebit()), r.getDiffusions(),
                r.getRappelMinutes());
        synchronized (this) {
            retirerDeLaFile(match);
            rappels.removeIf(x -> memeRencontre(x.demande().annonce().match(), match));
            file.addLast(d);
            return ResultatDemandeAnnonce.acceptee(annonce, file.size());
        }
    }

    /**
     * Même rencontre ? Le même objet, ou les deux mêmes joueurs (le tournoi peut
     * être relu depuis le fichier entre deux pages : les objets changent, les
     * licences restent).
     */
    public static boolean memeRencontre(Match a, Match b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || inconnu(a.getJoueurA()) || inconnu(a.getJoueurB())
                || inconnu(b.getJoueurA()) || inconnu(b.getJoueurB())) {
            return false;
        }
        // L'ordre des joueurs ne change pas la rencontre (A contre B = B contre A).
        return (a.getJoueurA().equals(b.getJoueurA()) && a.getJoueurB().equals(b.getJoueurB()))
                || (a.getJoueurA().equals(b.getJoueurB()) && a.getJoueurB().equals(b.getJoueurA()));
    }

    private static boolean inconnu(Joueur j) {
        return j == null || j.estADeterminer();
    }

    /**
     * Les joueurs sont à la table (ou le match a quitté la table : score saisi,
     * envoi annulé) : plus de rappel ni d'annonce en attente pour ce match.
     *
     * @return vrai si quelque chose a été retiré
     */
    public synchronized boolean oublier(Match match) {
        if (enCours != null && memeRencontre(enCours.match(), match)) {
            enCoursOubliee = true;
        }
        boolean retire = retirerDeLaFile(match);
        return rappels.removeIf(x -> memeRencontre(x.demande().annonce().match(), match)) || retire;
    }

    private boolean retirerDeLaFile(Match match) {
        return file.removeIf(x -> memeRencontre(x.annonce().match(), match));
    }

    /**
     * Met en file les rappels arrivés à échéance dont la table attend toujours
     * ses joueurs ; les autres rappels échus sont abandonnés.
     *
     * @param tableToujoursVide le match est-il toujours sur cette table, sans les joueurs ?
     * @return nombre de rappels mis en file
     */
    public synchronized int programmerRappelsDus(BiPredicate<Match, Integer> tableToujoursVide) {
        Instant maintenant = horloge.maintenant();
        int ajoutes = 0;
        Iterator<Rappel> it = rappels.iterator();
        while (it.hasNext()) {
            Rappel r = it.next();
            if (r.echeance().isAfter(maintenant)) {
                continue;
            }
            it.remove();
            AnnonceVocale a = r.demande().annonce();
            if (tableToujoursVide.test(a.match(), a.table())) {
                retirerDeLaFile(a.match());
                Demande d = r.demande();
                file.addLast(new Demande(a.enRappel(), d.voix(), d.diffusions(), 0));
                ajoutes++;
            }
        }
        return ajoutes;
    }

    // ------------------------------------------------------------------
    // Diffusion
    // ------------------------------------------------------------------

    /**
     * Dit la prochaine annonce de la file (bloquant le temps de la parole), ou
     * l'affiche seulement si la voix est indisponible. Deux appels simultanés
     * ne parlent jamais en même temps.
     *
     * @return l'issue, ou vide si la file est vide
     */
    public Optional<ResultatAnnonceVocale> diffuserSuivante() {
        synchronized (parole) {
            Demande d;
            synchronized (this) {
                d = file.pollFirst();
                if (d == null) {
                    return Optional.empty();
                }
                enCours = d.annonce();
                enCoursOubliee = false;
            }
            ResultatSynthese rs;
            try {
                if (!synthese.estDisponible()) {
                    rs = SyntheseVocale.AUCUNE.dire("", d.voix());
                } else {
                    rs = synthese.dire(GenerateurSsml.generer(d.annonce(), d.diffusions(), resolveur), d.voix());
                }
                if (rs == null) {
                    rs = ResultatSynthese.echec("réponse vide de la synthèse vocale");
                }
            } catch (RuntimeException e) {
                rs = ResultatSynthese.echec("erreur de la synthèse vocale (" + e.getMessage() + ")");
            }
            synchronized (this) {
                enCours = null;
                if (d.rappelMinutes() > 0 && !d.annonce().rappel() && !enCoursOubliee) {
                    rappels.add(new Rappel(d, horloge.maintenant().plus(Duration.ofMinutes(d.rappelMinutes()))));
                }
            }
            return Optional.of(new ResultatAnnonceVocale(d.annonce(), rs.dite(), rs.dite() ? "" : rs.motif()));
        }
    }

    // ------------------------------------------------------------------
    // État (affichage)
    // ------------------------------------------------------------------

    /** Annonces en attente, dans l'ordre où elles seront dites. */
    public synchronized List<AnnonceVocale> enAttente() {
        List<AnnonceVocale> l = new ArrayList<>();
        file.forEach(d -> l.add(d.annonce()));
        return l;
    }

    /** Annonce en train d'être dite, s'il y en a une. */
    public synchronized Optional<AnnonceVocale> enCours() {
        return Optional.ofNullable(enCours);
    }

    /** Échéance du rappel programmé pour ce match, s'il y en a un. */
    public synchronized Optional<Instant> rappelPrevu(Match match) {
        for (Rappel r : rappels) {
            if (memeRencontre(r.demande().annonce().match(), match)) {
                return Optional.of(r.echeance());
            }
        }
        return Optional.empty();
    }

    /** Reste-t-il quelque chose à dire ? */
    public synchronized boolean aDiffuser() {
        return !file.isEmpty();
    }
}
