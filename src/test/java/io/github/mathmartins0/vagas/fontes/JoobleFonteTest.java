package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Pontuador;
import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoobleFonteTest {

    private final JoobleFonte fonte = new JoobleFonte("chave", List.of("java pj remoto"));

    @Test
    void converteVagasDaApi() throws Exception {
        String corpo = """
                {"totalCount":2,"jobs":[
                 {"id":-123456789,"title":"Desenvolvedor <b>Java</b> Pleno","location":"Brasil","snippet":"&nbsp;...vaga 100% remota com <b>Spring Boot</b>...",
                  "salary":"","source":"acme.inhire.app","type":"Tempo integral","link":"https://br.jooble.org/desc/-123456789",
                  "company":"Acme","updated":"2026-11-04T10:15:00.0000000"},
                 {"id":2,"title":"Dev Java","location":"São Paulo, SP","snippet":"Modelo híbrido","link":"https://br.jooble.org/desc/2",
                  "company":"","updated":"x"},
                 {"id":3,"title":"Vendedor Interno PJ","location":"Santo André, SP","snippet":"Vendas por telefone, remoto",
                  "link":"https://br.jooble.org/desc/3","company":"","updated":"x"},
                 {"id":4,"title":"Técnico de Suporte Pleno","location":"Contagem, MG","snippet":"Atendimento de chamados, PJ",
                  "link":"https://br.jooble.org/desc/4","company":"","updated":"x"}]}
                """;

        List<Vaga> vagas = fonte.converter(corpo);

        Vaga v = vagas.getFirst();
        assertEquals("Jooble:-123456789", v.chave());
        assertEquals("Desenvolvedor Java Pleno", v.titulo());
        assertEquals("remoto", v.modalidade());
        assertTrue(v.descricao().contains("Spring Boot"));
        assertEquals(LocalDate.of(2026, 11, 4), v.publicadaEm());
        assertNull(vagas.get(1).publicadaEm());

        // o termo buscado não conta: PJ, remoto e área têm que estar na própria vaga
        Pontuador pontuador = new Pontuador(List.of("Spring Boot"), List.of("chamados"), true, true, false);
        assertEquals("não diz que aceita PJ", pontuador.avaliar(v).motivos().getFirst());
        assertTrue(pontuador.avaliar(vagas.get(1)).descartada()); // híbrido
        assertEquals("não é vaga de Java, QA nem suporte", pontuador.avaliar(vagas.get(2)).motivos().getFirst());
        assertEquals("não diz que é remota", pontuador.avaliar(vagas.get(3)).motivos().getFirst());
    }
}
