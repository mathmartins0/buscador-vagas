package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemotarFonteTest {

    @Test
    void converteVagasComTags() throws Exception {
        String corpo = """
                {"meta":{"last_page":1},"data":[
                 {"id":168218,"title":"Desenvolvedor Java Pleno","description":"<p>Spring Boot</p>","type":"remote",
                  "externalLink":"https://acme.vagas.solides.com.br/vaga/1?origem=remotar","createdAt":"2026-10-26T01:59:04.512-03:00",
                  "company":{"name":"Acme"},
                  "jobTags":[{"tag":{"name":"🌍 100% Remoto"}},{"tag":{"name":"🤝🏽 PJ"}},{"tag":{"name":"😎 Pleno"}}]},
                 {"id":2,"title":"Dev Java","description":"","type":"remote","externalLink":null,"createdAt":"x",
                  "company":{"name":"B"},"jobTags":[{"tag":{"name":"🏢 Vaga híbrida"}}]}]}
                """;

        List<Vaga> vagas = new RemotarFonte().converter(corpo);

        Vaga v = vagas.getFirst();
        assertEquals("Remotar:168218", v.chave());
        assertEquals("Acme", v.empresa());
        assertEquals("remoto", v.modalidade());
        assertEquals("100% Remoto, PJ, Pleno", v.tipoContrato());
        assertEquals("https://acme.vagas.solides.com.br/vaga/1?origem=remotar", v.url());
        assertEquals(LocalDate.of(2026, 10, 26), v.publicadaEm());
        assertEquals("hibrido", vagas.get(1).modalidade());
        assertEquals("https://remotar.com.br/job/2", vagas.get(1).url());
    }
}
