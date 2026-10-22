package io.github.mathmartins0.vagas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PontuadorTest {

    private final Pontuador pontuador = new Pontuador(List.of("Spring Boot", "SQL Server", "Docker", "Kubernetes"));

    private static Vaga vaga(String titulo, String descricao, String contrato, String modalidade) {
        return new Vaga("1", "Teste", titulo, "Empresa", "São Paulo", modalidade, contrato, "https://x", descricao, null);
    }

    @Test
    void vagaIdealGanhaNotaAlta() {
        Avaliacao a = pontuador.avaliar(vaga("Desenvolvedor Java Pleno",
                "Contratação PJ. Stack: Spring Boot, SQL Server, Docker e Kubernetes.", "", "remoto"));

        assertFalse(a.descartada());
        assertEquals(97, a.nota()); // 20 título + 30 PJ + 20 pleno + 15 remoto + 12 habilidades
        assertEquals(List.of("Spring Boot", "SQL Server", "Docker", "Kubernetes"), a.habilidadesEmComum());
    }

    @Test
    void tipoPessoaJuridicaDaGupyContaComoPj() {
        Avaliacao a = pontuador.avaliar(vaga("Backend Developer", "Java 17", "vacancy_legal_entity", ""));

        assertTrue(a.motivos().contains("+30 aceita PJ"));
    }

    @Test
    void tipoContratoDoLinkedInContaComoPj() {
        Avaliacao a = pontuador.avaliar(vaga("Desenvolvedor Java", "Spring Boot", "Pleno-sênior · Contrato", ""));

        assertTrue(a.motivos().contains("+30 aceita PJ"));
    }

    @Test
    void soCltPerdePontos() {
        Avaliacao a = pontuador.avaliar(vaga("Desenvolvedor Java", "Regime CLT, benefícios.", "", ""));

        assertTrue(a.motivos().contains("-30 parece ser só CLT"));
    }

    @Test
    void cltOuPjContaComoPj() {
        Avaliacao a = pontuador.avaliar(vaga("Desenvolvedor Java", "Contratação CLT ou PJ.", "", ""));

        assertTrue(a.motivos().contains("+30 aceita PJ"));
    }

    @Test
    void descartaVagaSemJava() {
        assertTrue(pontuador.avaliar(vaga("Desenvolvedor Python", "Django e PJ", "", "remoto")).descartada());
    }

    @Test
    void descartaJuniorEEstagio() {
        assertTrue(pontuador.avaliar(vaga("Desenvolvedor Java Júnior", "Spring", "", "")).descartada());
        assertTrue(pontuador.avaliar(vaga("Estágio em Desenvolvimento Java", "", "", "")).descartada());
    }

    @Test
    void naoConfundePalavraDentroDeOutra() {
        // "javascript" não é Java, e "pj" dentro de outra palavra não conta
        assertTrue(pontuador.avaliar(vaga("Desenvolvedor JavaScript", "React", "", "")).descartada());
    }
}
