package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoJobsFonteTest {

    @Test
    void converteCartoes() {
        String html = """
                <div id="vacancy12135182" data-id="12135182" class="pt-24 js_vacancyLoad js_rowCard"
                     data-href="/vaga-de-desenvolvedor-java__12135182.aspx">
                  <div hidden class="js_date" data-value="2026/10/20 11:55:00"></div>
                  <h2 class="h3 js_vacancyTitle"> Desenvolvedor Java Pleno </h2>
                  <div class="d-flex"><div class="text-body"><a class="text-body" href="https://www.infojobs.com.br/x"> 3AM IT SERVICES <span>LTDA</span></a></div></div>
                  <div class="mb-8"> Curitiba - PR </div>
                  <div> Híbrido </div>
                  <div class="text-medium"> Modelo de contratação: PJ </div>
                </div>
                """;

        List<Vaga> vagas = new InfoJobsFonte().converter(html);

        Vaga v = vagas.getFirst();
        assertEquals("InfoJobs:12135182", v.chave());
        assertEquals("Desenvolvedor Java Pleno", v.titulo());
        assertEquals("3AM IT SERVICES", v.empresa());
        assertEquals("https://www.infojobs.com.br/vaga-de-desenvolvedor-java__12135182.aspx", v.url());
        assertEquals(LocalDate.of(2026, 10, 20), v.publicadaEm());
        assertTrue(v.descricao().contains("Híbrido"));
    }
}
