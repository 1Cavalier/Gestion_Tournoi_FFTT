#!/bin/bash
# Dossier de travail privé par défaut : deux sessions parallèles ne s'écrasent plus.
SP=${SP:-$(mktemp -d /tmp/pingmanager-dev.XXXXXX)}; mkdir -p $SP
SRC=${SRC:-$(cd "$(dirname "$0")/.." && pwd)}
J=/usr/share/java/junit-platform-console-standalone.jar
FX=/usr/share/java/javafx-base.jar:/usr/share/java/javafx-controls.jar:/usr/share/java/javafx-graphics.jar:/usr/share/java/javafx-web.jar:/usr/share/java/javafx-media.jar
PDF=/usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar
rm -rf $SP/tbuild && mkdir -p $SP/tbuild
javac -nowarn -d $SP/tbuild -cp $J:$FX:$PDF $(find $SRC/src/main/java $SRC/src/test/java -name "*.java") 2>&1 | grep -v "^Picked\|^Note"
cp -r $SRC/src/main/resources/* $SP/tbuild/
java -Duser.home=$SP/home -jar $J -cp $SP/tbuild:$FX:$PDF --scan-classpath --disable-banner --details=summary 2>&1 | grep -v "^Picked" | grep -E "tests (successful|failed|found)|✘|MethodSource|=>" | head -60
