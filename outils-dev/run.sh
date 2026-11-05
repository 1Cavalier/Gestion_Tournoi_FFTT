#!/bin/bash
# usage: run.sh <outdir> [sombre] [brunoy|onglet|import|choix|v2]
# FX, PDF et ECRAN (taille de l'écran Xvfb) peuvent être fournis par variables d'environnement.
SP=${SP:-/tmp/pingmanager-dev}; mkdir -p $SP
FX=${FX:-/usr/share/java/javafx-base.jar:/usr/share/java/javafx-controls.jar:/usr/share/java/javafx-graphics.jar:/usr/share/java/javafx-web.jar:/usr/share/java/javafx-media.jar}
PDF=${PDF:-/usr/share/java/pdfbox2.jar:/usr/share/java/fontbox2.jar:/usr/share/java/commons-logging.jar}
SRC=${SRC:-$(cd "$(dirname "$0")/.." && pwd)}
rm -rf $SP/build && mkdir -p $SP/build "$1"
javac -nowarn -d $SP/build -cp $FX:$PDF $(find $SRC/src/main/java -name "*.java") $(find $SRC/outils-dev/harness -name "*.java") 2>&1 | grep -v "^Picked\|^Note" 
[ -d $SRC/src/main/resources ] && cp -r $SRC/src/main/resources/* $SP/build/
xvfb-run -a -s "-screen 0 ${ECRAN:-1280x900x24}" java -Djava.library.path=/usr/lib/x86_64-linux-gnu/jni -Dprism.order=sw -Duser.home=$SP/home \
  --module-path $FX --add-modules javafx.controls,javafx.web -cp $SP/build:$PDF com.pingmanager.ui.ecran.Capture "$@" 2>&1 | grep -v "^Picked\|PingManager —" | grep -v "^\s*at javafx\|^\s*at java\.\|^\s*at com.sun" | head -20
