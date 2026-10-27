package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tira as vagas do HTML de um e-mail de alerta (Indeed, Glassdoor, Programathor, Jobbol...).
 * Cada link que aponta para uma vaga do site vira uma vaga; o bloco em volta do link
 * (empresa, local, trecho) vira a descrição.
 */
public class LeitorAlertaEmail {

    /** Site → padrão de caminho/consulta que identifica o link de uma vaga. */
    private static final Map<String, Pattern> LINKS_DE_VAGA = Map.of(
            // os e-mails usam links de rastreamento de clique (engage.indeed.com/f/..., .../trc/...)
            "indeed", Pattern.compile("(rc/clk|viewjob|pagead/clk|[?&]jk=|engage\\.indeed\\.com/f/)"),
            "glassdoor", Pattern.compile("(joblisting|job-listing|/partner/|[?&]jl=)", Pattern.CASE_INSENSITIVE),
            "programathor", Pattern.compile("(/jobs/\\d+|/trc/)"),
            "jobbol", Pattern.compile("/vaga"));
    private static final Pattern LINK_GENERICO = Pattern.compile("(vaga|job)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXTO_DE_NAVEGACAO = Pattern.compile(
            "(ver (todas|mais)|veja (todas|mais)|descadastr|cancelar|unsubscribe|configura|gerenciar|prefer|"
                    + "baixe|download|app|privacidade|termos|ajuda|candidatar-se|candidate-se|aplicar|apply)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_RELATIVA = Pattern.compile(
            "(ha \\d+\\+? (dias|dia|horas|hora|semanas|semana)|\\d+ (dias|dia) atras|hoje|ontem|"
                    + "publicad[ao] [^·|]*|nova|novo|new|\\d+[dh] ago)");
    private static final int BLOCO_MAXIMO = 700;

    public List<Vaga> ler(String remetente, String assunto, String html, LocalDate recebidoEm) {
        String site = site(remetente);
        Pattern padrao = LINKS_DE_VAGA.getOrDefault(site, LINK_GENERICO);
        // O alerta é criado pela própria pessoa: se a busca do alerta tem "PJ", o site já filtrou por PJ.
        boolean buscaPj = Pattern.compile("(?<![a-z0-9])pj(?![a-z0-9])").matcher(normalizar(assunto)).find();

        Map<String, Vaga> vagas = new LinkedHashMap<>();
        for (Element link : Jsoup.parse(html).select("a[href]")) {
            String href = link.attr("href");
            String titulo = link.text().trim();
            if (!padrao.matcher(href).find() || titulo.length() < 6 || titulo.length() > 150
                    || TEXTO_DE_NAVEGACAO.matcher(titulo).find() || !mesmoSite(href, site)) {
                continue;
            }
            String bloco = bloco(link);
            // título + começo do bloco (empresa/local): o fim costuma ter "há 2 dias", que muda a cada e-mail
            String resto = DATA_RELATIVA.matcher(normalizar(bloco.replace(titulo, ""))).replaceAll("")
                    .replaceAll("[^a-z0-9]+", " ").trim();
            String id = hash(normalizar(titulo) + "|" + resto.substring(0, Math.min(60, resto.length())));
            vagas.putIfAbsent(id, new Vaga(
                    id,
                    "Alerta " + nomeDoSite(site),
                    titulo,
                    "",
                    "",
                    "",
                    buscaPj ? "PJ (busca do alerta: " + assunto + ")" : "",
                    href,
                    bloco,
                    recebidoEm));
        }
        return new ArrayList<>(vagas.values());
    }

    /** Diagnóstico: host + primeiro pedaço do caminho de cada link, com contagem, e quantos têm texto. */
    static String formatoDosLinks(String html) {
        Map<String, Integer> formatos = new java.util.TreeMap<>();
        int comTexto = 0;
        for (Element link : Jsoup.parse(html).select("a[href]")) {
            try {
                URI uri = URI.create(link.attr("href").trim().replace(" ", "%20"));
                String caminho = uri.getPath() == null ? "" : uri.getPath().replaceAll("^(/[^/]*(/[^/]*)?).*", "$1");
                formatos.merge(uri.getHost() + caminho.replaceAll("\\d{3,}", "N"), 1, Integer::sum);
            } catch (IllegalArgumentException e) {
                formatos.merge("(inválido)", 1, Integer::sum);
            }
            if (link.text().trim().length() >= 6) {
                comTexto++;
            }
        }
        return comTexto + " com texto; " + formatos;
    }

    /** Sobe a partir do link até o maior bloco que ainda é só desta vaga. */
    private static String bloco(Element link) {
        Element atual = link;
        String texto = link.text();
        while (atual.parent() != null) {
            Element pai = atual.parent();
            String textoPai = pai.text();
            if (textoPai.length() > BLOCO_MAXIMO || pai.select("a[href]").size() > 4) {
                break;
            }
            atual = pai;
            texto = textoPai;
        }
        return texto;
    }

    static String site(String remetente) {
        String r = remetente.toLowerCase(Locale.ROOT);
        for (String site : LINKS_DE_VAGA.keySet()) {
            if (r.contains(site)) {
                return site;
            }
        }
        int arroba = r.lastIndexOf('@');
        return arroba >= 0 ? r.substring(arroba + 1).replaceAll("[>\\s].*$", "") : r;
    }

    private static boolean mesmoSite(String href, String site) {
        if (!LINKS_DE_VAGA.containsKey(site)) {
            return true;
        }
        try {
            String host = URI.create(href.trim().replace(" ", "%20")).getHost();
            // links de rastreamento de e-mail (ex.: click.mail...) também são aceitos se o caminho bater
            return host == null || host.contains(site) || host.contains("click") || host.contains("track");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String nomeDoSite(String site) {
        return switch (site) {
            case "indeed" -> "Indeed";
            case "glassdoor" -> "Glassdoor";
            case "programathor" -> "Programathor";
            case "jobbol" -> "Jobbol";
            default -> site;
        };
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String hash(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
