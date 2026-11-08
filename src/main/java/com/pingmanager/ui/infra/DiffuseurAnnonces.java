package com.pingmanager.ui.infra;

import com.pingmanager.core.ResultatAnnonceVocale;
import com.pingmanager.core.ServiceAnnoncesVocales;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Fait parler la file d'annonces (B017) sur un fil de fond unique, pour ne
 * jamais figer l'écran pendant que la voix parle. Une seule instance par
 * application : la file et les rappels survivent aux changements de page.
 */
public final class DiffuseurAnnonces {

    private final ServiceAnnoncesVocales service;
    private final ExecutorService fil = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pingmanager-annonces");
        t.setDaemon(true);
        return t;
    });
    private volatile Consumer<ResultatAnnonceVocale> ecouteur = r -> { };
    private volatile List<String> voixConnues;

    public DiffuseurAnnonces(ServiceAnnoncesVocales service) {
        this.service = service;
    }

    public ServiceAnnoncesVocales service() {
        return service;
    }

    /**
     * Reçoit chaque issue (appelé depuis le fil de fond : à l'écran de repasser
     * sur le fil de l'interface).
     */
    public void surDiffusion(Consumer<ResultatAnnonceVocale> ecouteur) {
        this.ecouteur = ecouteur == null ? r -> { } : ecouteur;
    }

    /**
     * Voix installées, lues une fois en fond (PowerShell peut prendre quelques
     * secondes) : vide tant que la liste n'est pas connue.
     */
    public List<String> voixConnues() {
        List<String> v = voixConnues;
        if (v == null) {
            voixConnues = List.of();
            fil.execute(() -> voixConnues = service.voixInstallees());
            return List.of();
        }
        return v;
    }

    /** Dit, l'une après l'autre, toutes les annonces en attente. */
    public void diffuser() {
        if (!service.aDiffuser()) {
            return;
        }
        fil.execute(() -> {
            Optional<ResultatAnnonceVocale> r;
            while ((r = service.diffuserSuivante()).isPresent()) {
                try {
                    ecouteur.accept(r.get());
                } catch (RuntimeException ignore) {
                    // Un écran fermé ne doit pas arrêter la file.
                }
            }
        });
    }
}
