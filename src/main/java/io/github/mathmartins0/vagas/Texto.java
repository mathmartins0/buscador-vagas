package io.github.mathmartins0.vagas;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Utilitários de texto: minúsculas, sem acento e busca por palavra inteira. */
public final class Texto {

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern TAGS_HTML = Pattern.compile("<[^>]+>");

    private Texto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semHtml = TAGS_HTML.matcher(texto).replaceAll(" ");
        String semAcento = ACENTOS.matcher(Normalizer.normalize(semHtml, Normalizer.Form.NFD)).replaceAll("");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    /** true se algum termo aparece como palavra inteira no texto já normalizado. */
    public static boolean contem(String textoNormalizado, String... termos) {
        for (String termo : termos) {
            String t = Pattern.quote(normalizar(termo));
            if (Pattern.compile("(?<![a-z0-9])" + t + "(?![a-z0-9])").matcher(textoNormalizado).find()) {
                return true;
            }
        }
        return false;
    }
}
