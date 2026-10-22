package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkedInFonteTest {

    @Test
    void converteCardsDaBuscaPublica() {
        String html = """
                <li><div class="base-card base-search-card" data-entity-urn="urn:li:jobPosting:4012345678">
                  <a class="base-card__full-link" href="https://br.linkedin.com/jobs/view/dev-java-4012345678?refId=abc"></a>
                  <h3 class="base-search-card__title"> Desenvolvedor Java Pleno - PJ </h3>
                  <h4 class="base-search-card__subtitle"><a>Acme</a></h4>
                  <span class="job-search-card__location">São Paulo, SP (Remoto)</span>
                  <time datetime="2026-10-21">1 dia atrás</time>
                </div></li>
                """;

        List<Vaga> vagas = new LinkedInFonte("Brasil").converter(html);

        assertEquals(1, vagas.size());
        Vaga v = vagas.getFirst();
        assertEquals("LinkedIn:4012345678", v.chave());
        assertEquals("Desenvolvedor Java Pleno - PJ", v.titulo());
        assertEquals("Acme", v.empresa());
        assertEquals("remoto", v.modalidade());
        assertEquals("https://br.linkedin.com/jobs/view/dev-java-4012345678", v.url());
        assertEquals(LocalDate.of(2026, 10, 21), v.publicadaEm());
    }
}
