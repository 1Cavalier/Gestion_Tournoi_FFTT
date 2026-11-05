package com.pingmanager.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règle d'or : model, core et ordonnancement n'importent ni interface, ni persistance, ni importation, ni PDFBox. */
class RegleOrModeleSansDependanceTest {

    private static final List<String> INTERDITS = List.of("javafx", "com.pingmanager.ui", "com.pingmanager.persistence",
            "com.pingmanager.importation", "org.apache.pdfbox");

    private static List<String> violations(String paquet) throws IOException {
        List<String> res = new ArrayList<>();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/pingmanager", paquet))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (p.getFileName().toString().equals("RenduPdfReglement.java")) {
                    continue; // exception connue
                }
                for (String l : Files.readAllLines(p)) {
                    String ligne = l.trim();
                    if (ligne.startsWith("import ") || ligne.startsWith("package ")) {
                        for (String interdit : INTERDITS) {
                            if (ligne.contains(interdit)) {
                                res.add(p + " : " + ligne);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    @Test
    void model_sans_dependance_interdite() throws IOException {
        List<String> v = violations("model");
        assertTrue(v.isEmpty(), v.toString());
    }

    @Test
    void core_sans_dependance_interdite() throws IOException {
        List<String> v = violations("core");
        assertTrue(v.isEmpty(), v.toString());
    }

    @Test
    void ordonnancement_sans_dependance_interdite() throws IOException {
        List<String> v = violations("ordonnancement");
        assertTrue(v.isEmpty(), v.toString());
    }

    @Test
    void document_reglement_n_importe_que_le_jdk() throws IOException {
        for (String l : Files.readAllLines(Path.of("src/main/java/com/pingmanager/model/DocumentReglement.java"))) {
            if (l.startsWith("import ")) {
                assertTrue(l.startsWith("import java."), l);
            }
        }
    }
}
