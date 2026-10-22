package io.github.mathmartins0.vagas.fontes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathmartins0.vagas.Vaga;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Vagas da Gupy, pela mesma API pública que o portal de vagas deles usa. */
public class GupyFonte implements FonteVagas {

    private static final String URL = "https://portal.api.gupy.io/api/job?limit=100&offset=0&name=";

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String nome() {
        return "Gupy";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        return converter(Http.get(URL + URLEncoder.encode(termo, StandardCharsets.UTF_8)));
    }

    List<Vaga> converter(String corpo) throws IOException {
        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode item : json.readTree(corpo).path("data")) {
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
