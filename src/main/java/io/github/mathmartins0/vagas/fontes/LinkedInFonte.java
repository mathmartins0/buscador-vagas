package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
 * Não usa sua conta: só lê as vagas e você se candidata pelo navegador.
 */
public class LinkedInFonte implements FonteVagas {

    private static final String BUSCA = "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search"
            + "?f_TPR=r604800&geoId=%s&keywords=%s&start=%d";
    private static final String DETALHE = "https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/";
    private static final int PAGINAS = 3;
    private static final int POR_PAGINA = 10;

    private final String geoId;
    private final boolean apenasRemoto;

    /**
     * @param geoId        código de região do LinkedIn (106057199 = Brasil)
     * @param apenasRemoto usa o filtro "Remoto" da própria busca do LinkedIn
     */
    public LinkedInFonte(String geoId, boolean apenasRemoto) {
        this.geoId = geoId;
        this.apenasRemoto = apenasRemoto;
    }

    @Override
    public String nome() {
        return "LinkedIn";
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException, InterruptedException {
        List<Vaga> vagas = new ArrayList<>();
        for (int pagina = 0; pagina < PAGINAS; pagina++) {
            String url = BUSCA.formatted(geoId, URLEncoder.encode(termo, StandardCharsets.UTF_8), pagina * POR_PAGINA)
                    + (apenasRemoto ? "&f_WT=2" : "");
            List<Vaga> encontradas = converter(Http.get(url));
            vagas.addAll(encontradas);
            if (encontradas.size() < POR_PAGINA) {
                break;
            }
            Thread.sleep(1000);
        }
        return vagas;
    }

    @Override
    public Vaga detalhar(Vaga vaga) throws IOException, InterruptedException {
        Thread.sleep(800);
        return completar(vaga, Http.get(DETALHE + vaga.id()));
    }

    List<Vaga> converter(String html) {
        List<Vaga> vagas = new ArrayList<>();
        for (Element card : Jsoup.parse(html).select("div.base-search-card")) {
            String urn = card.attr("data-entity-urn");
            String id = urn.substring(urn.lastIndexOf(':') + 1);
            String link = card.select("a.base-card__full-link").attr("href");
            String titulo = card.select(".base-search-card__title").text();
            String lugar = card.select(".job-search-card__location").text();
            vagas.add(new Vaga(
                    id.isBlank() ? link : id,
                    nome(),
                    titulo,
                    card.select(".base-search-card__subtitle").text(),
                    lugar,
                    apenasRemoto ? "remoto" : modalidade(lugar),
                    "",
                    link.replaceAll("\\?.*$", ""),
                    "",
                    data(card.select("time").attr("datetime"))));
        }
        return vagas;
    }

    /** Preenche descrição e tipo de emprego (ex.: "Contrato", "Tempo integral") a partir da página da vaga. */
    Vaga completar(Vaga vaga, String html) {
        Document pagina = Jsoup.parse(html);
        String descricao = pagina.select(".show-more-less-html__markup").text();
        String criterios = String.join(" · ", pagina.select(".description__job-criteria-text").eachText());
        return new Vaga(vaga.id(), vaga.fonte(), vaga.titulo(), vaga.empresa(), vaga.local(),
                vaga.modalidade().isBlank() ? modalidade(descricao) : vaga.modalidade(),
                criterios, vaga.url(), descricao, vaga.publicadaEm());
    }

    private static String modalidade(String texto) {
        String t = texto.toLowerCase();
        if (t.contains("remot")) {
            return "remoto";
        }
        return t.contains("híbrid") || t.contains("hibrid") ? "hibrido" : "";
    }

    private static LocalDate data(String texto) {
        try {
            return texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
