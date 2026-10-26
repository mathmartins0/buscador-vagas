package io.github.mathmartins0.vagas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VagaTest {

    private static Vaga comUrl(String fonte, String id, String url) {
        return new Vaga(id, fonte, "Dev", "", "", "", "", url, "", null);
    }

    @Test
    void reconheceVagaDaGupyRepassadaPorOutraFonte() {
        // {"jobId":12771648,"source":"remotar"}
        Vaga daRemotar = comUrl("Remotar", "9", "https://fcamara.gupy.io/job/eyJqb2JJZCI6MTI3NzE2NDgsInNvdXJjZSI6InJlbW90YXIifQ==?jobBoardSource=remotar");

        assertEquals("Gupy:12771648", daRemotar.chaveDeOrigem());
    }

    @Test
    void linkComumUsaAChaveDaFonte() {
        assertEquals("LinkedIn:1", comUrl("LinkedIn", "1", "https://br.linkedin.com/jobs/view/1").chaveDeOrigem());
        assertEquals("X:2", comUrl("X", "2", "https://acme.gupy.io/job/nao-e-base64!").chaveDeOrigem());
    }
}
