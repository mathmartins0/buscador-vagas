package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * VAGAS.com.br, já filtrado por "100% Home Office". A lista traz só um trecho da descrição,
 * então cada vaga nova é aberta para ler a descrição e o modelo de contratação.
 */
public class VagasComFonte implements FonteVagas {

    private static final String SITE = "https://www.vagas.com.br";
    private static final String URL = SITE + "/vagas-de-%s?m%%5B%%5D=100%%25+Home+Office";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String nome() {
        return "VAGAS.com";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        return converter(Http.get(URL.formatted(slug(termo))), LocalDate.now());
    }

    @Override
    public Vaga detalhar(Vaga vaga) throws IOException, InterruptedException {
        Thread.sleep(800);
        return completar(vaga, Http.get(vaga.url()));
    }

    List<Vaga> converter(String html, LocalDate hoje) {
        List<Vaga> vagas = new ArrayList<>();
        for (Element card : Jsoup.parse(html).select("li.vaga")) {
            Element link = card.selectFirst("a.link-detalhes-vaga");
            if (link == null) {
                continue;
            }
            String nivel = card.select(".nivelVaga").text();
            String local = card.select(".vaga-local").text();
            vagas.add(new Vaga(
                    link.attr("data-id-vaga"),
                    nome(),
                    link.attr("title"),
                    card.select(".emprVaga").text(),
                    local,
                    local.toLowerCase(Locale.ROOT).contains("home office") ? "remoto" : "",
                    nivel,
                    SITE + link.attr("href"),
                    card.select(".detalhes").text(),
                    data(card.select(".data-publicacao").text(), hoje)));
        }
        return vagas;
    }

    /** Troca o trecho pela descrição completa e junta nível + modelo de contratação (ex.: "Pessoa Jurídica"). */
    Vaga completar(Vaga vaga, String html) {
        Document pagina = Jsoup.parse(html);
        String descricao = pagina.select(".job-description__text").text();
        String contratacao = pagina.select(".info-modelo-contratual").text();
        String local = pagina.select(".info-localizacao").text();
        return new Vaga(vaga.id(), vaga.fonte(), vaga.titulo(), vaga.empresa(),
                local.isBlank() ? vaga.local() : local,
                local.toLowerCase(Locale.ROOT).contains("home office") ? "remoto" : vaga.modalidade(),
                String.join(" · ", List.of(vaga.tipoContrato(), contratacao)).replaceAll("^ · | · $", ""),
                vaga.url(),
                descricao.isBlank() ? vaga.descricao() : descricao,
                vaga.publicadaEm());
    }

    static String slug(String termo) {
        String semAcento = Normalizer.normalize(termo, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-");
    }

    private static LocalDate data(String texto, LocalDate hoje) {
        String t = texto.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("hoje")) {
            return hoje;
        }
        if (t.startsWith("ontem")) {
            return hoje.minusDays(1);
        }
        try {
            return LocalDate.parse(t, DATA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
