package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackendBrFonteTest {

    @Test
    void converteIssuesComRotulos() throws Exception {
        String corpo = """
                [{"number":101,"title":"[Remoto] Back-end Java Pleno - Acme","html_url":"https://github.com/backend-br/vagas/issues/101",
                  "body":"Contratação PJ","created_at":"2026-10-21T10:00:00Z",
                  "labels":[{"name":"PJ"},{"name":"Pleno"},{"name":"Java"},{"name":"Remoto"}]},
                 {"number":102,"title":"[Híbrido] Dev Java","html_url":"u","body":null,"created_at":"2026-10-21T10:00:00Z",
                  "labels":[{"name":"Remoto"},{"name":"Híbrido"}]},
                 {"number":103,"title":"PR","pull_request":{},"labels":[]}]
                """;

        List<Vaga> vagas = new BackendBrFonte().converter(corpo);

        assertEquals(2, vagas.size());
        assertEquals("backend-br:101", vagas.getFirst().chave());
        assertEquals("PJ, Pleno, Java, Remoto", vagas.getFirst().tipoContrato());
        assertEquals("remoto", vagas.getFirst().modalidade());
        assertEquals("hibrido", vagas.get(1).modalidade());
    }
}
