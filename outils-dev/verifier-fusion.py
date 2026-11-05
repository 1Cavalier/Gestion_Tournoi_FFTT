#!/usr/bin/env python3
"""Vérifie qu'une fusion n'a rien perdu du travail déjà présent sur main.

À lancer sur une branche, après `git merge origin/main` :

    python3 outils-dev/verifier-fusion.py            # compare à origin/main
    python3 outils-dev/verifier-fusion.py <commit>   # compare à un autre commit

Pour chaque fichier modifié par main depuis l'ancêtre commun avec la branche,
toute ligne que main a ajoutée doit encore exister dans la version fusionnée.
Les lignes manquantes sont listées : soit la branche les a remplacées
volontairement (à justifier dans le journal), soit la fusion les a perdues
(à réparer). Code de sortie 1 s'il en manque.

Sans dépendance : bibliothèque standard de Python 3 uniquement.
"""
import subprocess
import sys

IGNORES = (".png", ".jpg", ".jpeg", ".gif", ".ttf", ".pdf", ".xlsx", ".ods", ".zip")
TRIVIALES = {"}", "{", "*/", "/**", "*", "});", ");", "};", "", "*/}", "</ul>", "<ul>"}


def git(*args):
    return subprocess.run(("git",) + args, capture_output=True, text=True, check=True).stdout


def main():
    reference = sys.argv[1] if len(sys.argv) > 1 else "origin/main"
    base = git("merge-base", "HEAD^1" if _est_fusion() else "HEAD", reference).strip()
    fichiers = [f for f in git("diff", "--name-only", base, reference).split()
                if not f.lower().endswith(IGNORES)]
    manque_total = 0
    for f in fichiers:
        diff = git("diff", "-U0", base, reference, "--", f)
        ajouts = [l[1:].strip() for l in diff.splitlines()
                  if l.startswith("+") and not l.startswith("+++")]
        ajouts = [a for a in ajouts if a not in TRIVIALES and len(a) > 3]
        if not ajouts:
            continue
        try:
            with open(f, encoding="utf-8") as fh:
                actuel = {x.strip() for x in fh.read().splitlines()}
        except FileNotFoundError:
            actuel = set()
        manquantes = [a for a in ajouts if a not in actuel]
        if manquantes:
            manque_total += len(manquantes)
            print(f"{f} : {len(manquantes)} ligne(s) de main absente(s) sur {len(ajouts)}")
            for m in manquantes[:8]:
                print(f"    - {m[:120]}")
            if len(manquantes) > 8:
                print(f"    … et {len(manquantes) - 8} autre(s)")
    if manque_total:
        print(f"\n{manque_total} ligne(s) ajoutée(s) par main ont disparu : vérifier chaque cas.")
        sys.exit(1)
    print(f"OK : tout ce que {reference} a ajouté depuis l'ancêtre commun est présent.")


def _est_fusion():
    parents = git("rev-list", "--parents", "-n", "1", "HEAD").split()
    return len(parents) > 2


if __name__ == "__main__":
    main()
