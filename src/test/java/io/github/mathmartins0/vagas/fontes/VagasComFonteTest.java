package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VagasComFonteTest {

    private final VagasComFonte fonte = new VagasComFonte();

    @Test
    void converteCartoesDaBusca() {
        String html = """
                <ul><li class="vaga odd "><h2 class="cargo">
                  <a class="link-detalhes-vaga" data-id-vaga="2839391" title="Desenvolvedor Java Pleno"
                     href="/vagas/v2839391/desenvolvedor-java">Desenvolvedor <mark>Java</mark></a></h2>
                  <span class="emprVaga"> Talent Group </span>
                  <span class="nivelVaga"> Pleno </span>
                  <div class="detalhes"><p>Contratação: PJ</p></div>
                  <span class="vaga-local"><i></i> 100% Home Office </span>
                  <span class="data-publicacao"><i></i>Ontem</span></li>
                <li class="publicidade"></li></ul>
                """;

        List<Vaga> vagas = fonte.converter(html, LocalDate.of(2026, 10, 26));

        assertEquals(1, vagas.size());
        Vaga v = vagas.getFirst();
        assertEquals("VAGAS.com:2839391", v.chave());
        assertEquals("Talent Group", v.empresa());
        assertEquals("Pleno", v.tipoContrato());
        assertEquals("remoto", v.modalidade());
        assertEquals("https://www.vagas.com.br/vagas/v2839391/desenvolvedor-java", v.url());
        assertEquals(LocalDate.of(2026, 10, 25), v.publicadaEm());
    }

    @Test
    void completaComDescricaoEModeloDeContratacao() {
        Vaga card = new Vaga("1", "VAGAS.com", "Dev Java", "A", "", "", "Pleno", "https://x", "trecho", null);
        String html = """
                <span class="info-localizacao" title="100% Home Office"> 100% Home Office </span>
                <span class="info-modelo-contratual" title="Pessoa Jurídica"> Pessoa Jurídica </span>
                <div class="job-tab-content job-description__text texto"><p>Java 17 e Spring Boot</p></div>
                """;

        Vaga v = fonte.completar(card, html);

        assertEquals("Java 17 e Spring Boot", v.descricao());
        assertEquals("Pleno · Pessoa Jurídica", v.tipoContrato());
        assertEquals("remoto", v.modalidade());
    }

    @Test
    void montaSlugDoTermo() {
        assertEquals("java-junior", VagasComFonte.slug("Java Júnior"));
    }
}
