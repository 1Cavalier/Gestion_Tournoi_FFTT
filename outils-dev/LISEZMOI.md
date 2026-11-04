# Outils de développement (session cloud, sans Maven)

Maven Central est inaccessible dans la session : on compile avec `javac` et les jars de `/usr/share/java`.

- `./tests.sh` : compile `src/main` + `src/test` et lance tous les tests JUnit (jar `junit-platform-console-standalone.jar`).
- `./run.sh <dossier> [sombre] [brunoy]` : compile l'appli + le harnais `harness/…/Capture.java` et capture les écrans en PNG sous Xvfb.
- Variables : `SP` (dossier de travail, défaut `/tmp/pingmanager-dev`), `SRC` (racine du projet, défaut le dossier parent).
- La carte (`WebView`) est prise en charge : `javafx-web.jar` (et `javafx-media.jar`) sont dans `FX`, `javafx.web` dans `--add-modules`.
- Si JavaFX, PDFBox 2 ou JUnit 5 manquent : `apt-get install -y openjfx libopenjfx-jni libpdfbox2-java junit5 xvfb` (apt passe, Maven Central non).
- `./run.sh <dossier> onglet` : captures de l'onglet Tournois (carte, filtres, fiche, inscription, Mes tournois) et de la vue Inscriptions du club.

Ces fichiers ne font pas partie de l'application (non compilés par Maven).
