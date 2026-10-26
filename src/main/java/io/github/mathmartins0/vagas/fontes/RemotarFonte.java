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

/**
 * Remotar (remotar.com.br), site só de vagas remotas no Brasil. A API pública traz
 * tags como "PJ", "CLT", "Pleno", "Sênior", "Vaga híbrida" e "Vaga internacional".
 */
public class RemotarFonte implements FonteVagas {

    private static final String URL = "https://api.remotar.com.br/jobs?search=%s&page=%d";
    private static final int PAGINAS = 3;

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public String nome() {
        return "Remotar";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        List<Vaga> vagas = new ArrayList<>();
        for (int pagina = 1; pagina <= PAGINAS; pagina++) {
            String corpo = Http.get(URL.formatted(URLEncoder.encode(termo, StandardCharsets.UTF_8), pagina));
            vagas.addAll(converter(corpo));
            if (pagina >= json.readTree(corpo).path("meta").path("last_page").asInt(1)) {
                break;
            }
            Thread.sleep(800);
        }
        return vagas;
    }

    List<Vaga> converter(String corpo) throws IOException {
        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode item : json.readTree(corpo).path("data")) {
            List<String> tags = new ArrayList<>();
            item.path("jobTags").forEach(t -> tags.add(t.path("tag").path("name").asText().replaceAll("^[^\\p{L}\\p{N}]+", "")));
            String id = item.path("id").asText();
            String link = item.path("externalLink").asText("");
            vagas.add(new Vaga(
                    id,
                    nome(),
                    item.path("title").asText(),
                    item.path("company").path("name").asText(""),
                    "",
                    item.path("type").asText().equals("remote") && !tags.contains("Vaga híbrida") ? "remoto" : "hibrido",
                    String.join(", ", tags),
                    link.isBlank() ? "https://remotar.com.br/job/" + id : link,
                    item.path("description").asText(""),
                    data(item.path("createdAt").asText())));
        }
        return vagas;
    }

    private static LocalDate data(String texto) {
        try {
            return OffsetDateTime.parse(texto).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
