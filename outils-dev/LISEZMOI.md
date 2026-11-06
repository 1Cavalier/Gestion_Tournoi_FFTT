# Outils de développement (session cloud, sans Maven)

Maven Central est inaccessible dans la session : on compile avec `javac` et les jars de `/usr/share/java`.

- `./tests.sh` : compile `src/main` + `src/test` et lance tous les tests JUnit (jar `junit-platform-console-standalone.jar`).
- `./run.sh <dossier> [sombre] [brunoy]` : compile l'appli + le harnais `harness/…/Capture.java` et capture les écrans en PNG sous Xvfb.
- Variables : `SP` (dossier de travail, défaut `/tmp/pingmanager-dev`), `SRC` (racine du projet, défaut le dossier parent).
- La carte (`WebView`) est prise en charge : `javafx-web.jar` (et `javafx-media.jar`) sont dans `FX`, `javafx.web` dans `--add-modules`.
- Si JavaFX, PDFBox 2 ou JUnit 5 manquent : `apt-get install -y openjfx libopenjfx-jni libpdfbox2-java junit5 xvfb` (apt passe, Maven Central non).
- `./run.sh <dossier> onglet` : captures de l'onglet Tournois (carte, filtres, fiche, inscription, Mes tournois) et de la vue Inscriptions du club.

Ces fichiers ne font pas partie de l'application (non compilés par Maven).
- `./run.sh <dossier> v2 [sombre]` : écrans de référence de la charte v2 à 1440 px (fichiers `<écran>-clair.png` / `<écran>-sombre.png`).
- Jars du dépôt Maven local (après `mvn -B test`) : `FX`, `PDF` et `ECRAN` se passent par variables d'environnement, par exemple
  `FX=$(ls ~/.m2/repository/org/openjfx/javafx-{base,controls,graphics,web,media}/21.0.5/*-linux.jar | paste -sd:)`,
  `PDF=…/pdfbox-2.0.29.jar:…/fontbox-2.0.29.jar:…/commons-logging-1.2.jar`, `ECRAN=1600x1000x24` (écran Xvfb plus large que la fenêtre de 1440).
- `./run.sh <dossier> profil [sombre]` : Mon profil (B029) à 1440 px, compte relié au club puis joueur non relié (case de confirmation cochée).
