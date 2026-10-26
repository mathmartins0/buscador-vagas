package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * InfoJobs, já filtrado na própria busca por "Prestador de Serviços (PJ)" e "Home office".
 * O cartão da vaga traz modalidade (ex.: "Híbrido") e um trecho da descrição.
 */
public class InfoJobsFonte implements FonteVagas {

    private static final String SITE = "https://www.infojobs.com.br";
    private static final String URL = SITE + "/vagas-de-emprego-%s-trabalho-home-office.aspx?tipocontrato=17";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Override
    public String nome() {
        return "InfoJobs";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        return converter(Http.get(URL.formatted(URLEncoder.encode(termo.trim(), StandardCharsets.UTF_8))));
    }

    List<Vaga> converter(String html) {
        List<Vaga> vagas = new ArrayList<>();
        for (Element card : Jsoup.parse(html).select("div.js_vacancyLoad[data-id]")) {
            Element empresa = card.selectFirst("div.text-body > a");
            vagas.add(new Vaga(
                    card.attr("data-id"),
                    nome(),
                    card.select(".js_vacancyTitle").text(),
                    empresa == null ? "" : empresa.ownText(),
                    card.select("div.mb-8").text(),
                    "remoto",
                    "PJ (filtro do InfoJobs: Prestador de Serviços)",
                    SITE + card.attr("data-href"),
                    card.text(),
                    data(card.select(".js_date").attr("data-value"))));
        }
        return vagas;
    }

    private static LocalDate data(String texto) {
        try {
            return texto.isBlank() ? null : LocalDate.parse(texto, DATA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
