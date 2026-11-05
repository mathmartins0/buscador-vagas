package io.github.mathmartins0.vagas.fontes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Jooble (br.jooble.org), agregador que junta vagas de milhares de sites brasileiros.
 * Precisa de uma chave grátis na variável JOOBLE_CHAVE. A cota de requisições da chave é pequena,
 * então a busca usa só os poucos termos de "jooble.termos" no config, em vez de todos os termos.
 * A API só devolve um trecho da descrição, e a busca do Jooble é "aproximada": para "java pj remoto"
 * ela também devolve vendedor PJ presencial. Por isso o termo buscado não conta para nada; PJ, remoto e
 * área precisam aparecer no título, no local ou no trecho da própria vaga.
 */
public class JoobleFonte implements FonteVagas {

    private static final String URL = "https://br.jooble.org/api/";
    private static final int POR_PAGINA = 50;

    private static final Pattern REMOTO = Pattern.compile(
            "(?i)\\b(remot[oa]|home[ -]office|teletrabalho|remote|anywhere)\\b");

    private final ObjectMapper json = new ObjectMapper();
    private final String chave;
    private final List<String> termos;

    JoobleFonte(String chave, List<String> termos) {
        this.chave = chave;
        this.termos = termos;
    }

    /** null quando JOOBLE_CHAVE não está definida ou não há termos. */
    public static JoobleFonte doAmbiente(List<String> termos) {
        String chave = System.getenv("JOOBLE_CHAVE");
        if (chave == null || chave.isBlank() || termos == null || termos.isEmpty()) {
            return null;
        }
        return new JoobleFonte(chave.trim(), termos);
    }

    @Override
    public String nome() {
        return "Jooble";
    }

    @Override
    public boolean buscaPorTermo() {
        return false;
    }

    @Override
    public List<Vaga> buscar(String ignorado) throws IOException, InterruptedException {
        List<Vaga> vagas = new ArrayList<>();
        for (String termo : termos) {
            String corpo = json.writeValueAsString(Map.of(
                    "keywords", termo, "location", "Brasil", "page", 1, "ResultOnPage", POR_PAGINA));
            try {
                List<Vaga> encontradas = converter(Http.postJson(URL + chave, corpo));
                vagas.addAll(encontradas);
                System.out.printf("  Jooble \"%s\": %d vagas%n", termo, encontradas.size());
            } catch (IOException e) {
                // a mensagem não leva a URL, que contém a chave
                System.err.printf("  Jooble \"%s\": falhou (%s)%n", termo, e.getMessage());
            }
            Thread.sleep(1000);
        }
        return vagas;
    }

    List<Vaga> converter(String corpo) throws IOException {
        List<Vaga> vagas = new ArrayList<>();
        for (JsonNode item : json.readTree(corpo).path("jobs")) {
            String titulo = Jsoup.parse(item.path("title").asText()).text();
            String local = item.path("location").asText("");
            String trecho = Jsoup.parse(item.path("snippet").asText("")).text();
            boolean remoto = REMOTO.matcher(titulo + " " + local + " " + trecho).find();
            vagas.add(new Vaga(
                    item.path("id").asText(),
                    nome(),
                    titulo,
                    item.path("company").asText(""),
                    local,
                    remoto ? "remoto" : "",
                    item.path("type").asText(""),
                    item.path("link").asText(),
                    trecho,
                    data(item.path("updated").asText())));
        }
        return vagas;
    }

    private static LocalDate data(String texto) {
        try {
            return LocalDateTime.parse(texto.length() > 19 ? texto.substring(0, 19) : texto).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
