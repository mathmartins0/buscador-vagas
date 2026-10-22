package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Busca pública de vagas do LinkedIn (a mesma página que aparece sem login).
 * Não usa sua conta: só lê a lista de vagas e você se candidata pelo navegador.
 * A lista não traz a descrição completa, então a nota usa título, empresa e local.
 */
public class LinkedInFonte implements FonteVagas {

    private static final String URL = "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search"
            + "?f_TPR=r604800&start=0&location=%s&keywords=%s";

    private final String local;

    public LinkedInFonte(String local) {
        this.local = local;
    }

    @Override
    public String nome() {
        return "LinkedIn";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        String url = URL.formatted(
                URLEncoder.encode(local, StandardCharsets.UTF_8),
                URLEncoder.encode(termo, StandardCharsets.UTF_8));
        return converter(Http.get(url));
    }

    List<Vaga> converter(String html) {
        List<Vaga> vagas = new ArrayList<>();
        for (Element card : Jsoup.parse(html).select("div.base-search-card")) {
            String urn = card.attr("data-entity-urn");
            String id = urn.substring(urn.lastIndexOf(':') + 1);
            String link = card.select("a.base-card__full-link").attr("href");
            String titulo = card.select(".base-search-card__title").text();
            String empresa = card.select(".base-search-card__subtitle").text();
            String lugar = card.select(".job-search-card__location").text();
            vagas.add(new Vaga(
                    id.isBlank() ? link : id,
                    nome(),
                    titulo,
                    empresa,
                    lugar,
                    lugar.toLowerCase().contains("remot") ? "remoto" : "",
                    "",
                    link.replaceAll("\\?.*$", ""),
                    titulo,
                    data(card.select("time").attr("datetime"))));
        }
        return vagas;
    }

    private static LocalDate data(String texto) {
        try {
            return texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
