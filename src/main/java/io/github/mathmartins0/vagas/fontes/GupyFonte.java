package io.github.mathmartins0.vagas.fontes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vagas do portal da Gupy (portal.gupy.io). A página de busca já vem com a lista de vagas
 * embutida em JSON (__NEXT_DATA__), então lemos direto de lá.
 */
public class GupyFonte implements FonteVagas {

    private static final String URL = "https://portal.gupy.io/job-search/term=";

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String nome() {
        return "Gupy";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        return converter(Http.get(URL + URLEncoder.encode(termo, StandardCharsets.UTF_8)));
    }

    List<Vaga> converter(String html) throws IOException {
        Element script = Jsoup.parse(html).getElementById("__NEXT_DATA__");
        if (script == null) {
            throw new IOException("página da Gupy sem __NEXT_DATA__ (o site pode ter mudado)");
        }
        JsonNode lista = json.readTree(script.data()).path("props").path("pageProps").path("initialJobList").path("data");
        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode item : lista) {
            vagas.add(new Vaga(
                    item.path("id").asText(),
                    nome(),
                    item.path("name").asText(),
                    item.path("careerPageName").asText(),
                    local(item),
                    modalidade(item),
                    item.path("type").asText(),
                    item.path("jobUrl").asText(),
                    item.path("description").asText(),
                    data(item.path("publishedDate").asText())));
        }
        return vagas;
    }

    private static String local(JsonNode item) {
        String cidade = item.path("city").asText("");
        String estado = item.path("state").asText("");
        return cidade.isBlank() ? estado : cidade + (estado.isBlank() ? "" : "/" + estado);
    }

    private static String modalidade(JsonNode item) {
        String tipo = item.path("workplaceType").asText("");
        if (tipo.equals("remote") || item.path("isRemoteWork").asBoolean(false)) {
            return "remoto";
        }
        return switch (tipo) {
            case "hybrid" -> "hibrido";
            case "on-site" -> "presencial";
            default -> "";
        };
    }

    private static LocalDate data(String texto) {
        try {
            return OffsetDateTime.parse(texto).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
