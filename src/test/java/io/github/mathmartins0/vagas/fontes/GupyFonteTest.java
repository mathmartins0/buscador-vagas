package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GupyFonteTest {

    @Test
    void converteRespostaDaApi() throws Exception {
        String corpo = """
                {"data":[{"id":123,"name":"Desenvolvedor Java Pleno","careerPageName":"Acme",
                  "type":"vacancy_legal_entity","jobUrl":"https://acme.gupy.io/jobs/123",
                  "description":"<p>Spring Boot</p>","workplaceType":"remote","isRemoteWork":true,
                  "city":"São Paulo","state":"SP","publishedDate":"2026-10-20T12:00:00.000Z"}]}
                """;

        List<Vaga> vagas = new GupyFonte().converter(corpo);

        assertEquals(1, vagas.size());
        Vaga v = vagas.getFirst();
        assertEquals("Gupy:123", v.chave());
        assertEquals("Acme", v.empresa());
        assertEquals("São Paulo/SP", v.local());
        assertEquals("remoto", v.modalidade());
        assertEquals("vacancy_legal_entity", v.tipoContrato());
        assertEquals(LocalDate.of(2026, 10, 20), v.publicadaEm());
    }
}
