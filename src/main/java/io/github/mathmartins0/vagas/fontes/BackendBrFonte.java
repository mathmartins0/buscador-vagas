package io.github.mathmartins0.vagas.fontes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathmartins0.vagas.Vaga;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vagas do repositório backend-br/vagas no GitHub, onde cada vaga é uma issue
 * com rótulos padronizados (Java, PJ, Remoto, Pleno, Híbrido...).
 */
public class BackendBrFonte implements FonteVagas {

    private static final String URL =
            "https://api.github.com/repos/backend-br/vagas/issues?state=open&per_page=100&labels=Java";

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String nome() {
        return "backend-br";
    }

    @Override
    public boolean buscaPorTermo() {
        return false;
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        Map<String, String> cabecalhos = new HashMap<>(Map.of("Accept", "application/vnd.github+json"));
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) {
            cabecalhos.put("Authorization", "Bearer " + token);
        }
        return converter(Http.get(URL, cabecalhos));
    }

    List<Vaga> converter(String corpo) throws IOException {
        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode issue : json.readTree(corpo)) {
            if (issue.has("pull_request")) {
                continue;
            }
            List<String> rotulos = new ArrayList<>();
            issue.path("labels").forEach(r -> rotulos.add(r.path("name").asText()));
            vagas.add(new Vaga(
                    issue.path("number").asText(),
                    nome(),
                    issue.path("title").asText(),
                    "",
                    "",
                    modalidade(rotulos),
                    String.join(", ", rotulos),
                    issue.path("html_url").asText(),
                    issue.path("body").asText(""),
                    data(issue.path("created_at").asText())));
        }
        return vagas;
    }

    private static String modalidade(List<String> rotulos) {
        if (rotulos.contains("Híbrido") || rotulos.contains("Presencial")) {
            return "hibrido";
        }
        return rotulos.contains("Remoto") ? "remoto" : "";
    }

    private static LocalDate data(String texto) {
        try {
            return OffsetDateTime.parse(texto).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
