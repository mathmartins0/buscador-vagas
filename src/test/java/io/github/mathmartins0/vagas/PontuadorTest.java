package io.github.mathmartins0.vagas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PontuadorTest {

    private final Pontuador rigido = new Pontuador(List.of("Spring Boot", "SQL Server", "Docker", "Kubernetes"), true, true);
    private final Pontuador flexivel = new Pontuador(List.of(), false, false);

    private static Vaga vaga(String titulo, String descricao, String contrato, String modalidade) {
        return new Vaga("1", "Teste", titulo, "Empresa", "", modalidade, contrato, "https://x", descricao, null);
    }

    private static String motivo(Avaliacao a) {
        return a.motivos().getFirst();
    }

    @Test
    void vagaIdealGanhaNotaAlta() {
        Avaliacao a = rigido.avaliar(vaga("Desenvolvedor Java Pleno",
                "Contratação PJ. Stack: Spring Boot, SQL Server, Docker e Kubernetes.", "", "remoto"));

        assertFalse(a.descartada());
        assertEquals(97, a.nota()); // 20 título + 30 PJ + 20 pleno + 15 remoto + 12 habilidades
        assertEquals(List.of("Spring Boot", "SQL Server", "Docker", "Kubernetes"), a.habilidadesEmComum());
    }

    @Test
    void semPjExplicitoEhDescartada() {
        Avaliacao a = rigido.avaliar(vaga("Desenvolvedor Java Pleno", "Spring Boot", "", "remoto"));

        assertTrue(a.descartada());
        assertEquals("não diz que aceita PJ", motivo(a));
    }

    @Test
    void contratoGenericoDoLinkedInNaoContaComoPj() {
        assertTrue(rigido.avaliar(vaga("Desenvolvedor Java Pleno", "Spring", "Pleno-sênior · Contrato", "remoto")).descartada());
    }

    @Test
    void reconheceSinaisDePj() {
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno", "", "vacancy_legal_entity", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno", "", "PJ, Pleno, Remoto", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno", "", "Pleno · Pessoa Jurídica", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno", "Contratação CLT ou PJ", "", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno", "Prestador de serviços com CNPJ", "", "remoto")).descartada());
    }

    @Test
    void qualquerMencaoAHibridoOuPresencialDerrubaAVaga() {
        Avaliacao a = rigido.avaliar(vaga("Desenvolvedor Java Pleno",
                "Fora de SP: 100% home office. Residentes em SP: presencial 1x por semana. Contratação: PJ",
                "", "remoto"));

        assertTrue(a.descartada());
        assertEquals("menciona híbrido ou presencial", motivo(a));
        assertTrue(rigido.avaliar(vaga("Dev Java Pleno PJ", "Modelo híbrido", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Dev Java Pleno PJ", "", "PJ, Remoto, Híbrido", "remoto")).descartada());
    }

    @Test
    void semIndicacaoDeRemotoEhDescartada() {
        assertEquals("não diz que é remota", motivo(rigido.avaliar(vaga("Dev Java Pleno PJ", "", "", ""))));
        assertFalse(rigido.avaliar(vaga("[Remoto] Dev Java Pleno PJ", "", "", "")).descartada());
        assertFalse(rigido.avaliar(vaga("Dev Java Pleno PJ", "Atuação 100% remoto", "", "")).descartada());
    }

    @Test
    void descartaSeniorEAcimaMasAceitaPlenoSenior() {
        assertTrue(rigido.avaliar(vaga("Desenvolvedor Java Sênior PJ", "", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Engenheiro(a) de Software SR. Java PJ", "", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Java Developer - Expert-Level PJ", "", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Desenvolvedor Java PJ", "", "PJ, Sênior", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Desenvolvedor Java PJ", "", "PJ, Pleno, Sênior", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Pessoa Desenvolvedora Java Pleno/Sênior PJ", "", "", "remoto")).descartada());
    }

    @Test
    void aceitaJuniorEDescartaEstagio() {
        Avaliacao a = rigido.avaliar(vaga("Desenvolvedor Java Júnior PJ", "Spring", "", "remoto"));

        assertFalse(a.descartada());
        assertTrue(a.motivos().contains("+15 vaga de júnior"));
        assertTrue(rigido.avaliar(vaga("Estágio em Desenvolvimento Java", "PJ", "", "remoto")).descartada());
    }

    @Test
    void descartaVagaInternacional() {
        assertTrue(rigido.avaliar(vaga("Java Developer Pleno", "", "PJ, Vaga internacional", "remoto")).descartada());
    }

    @Test
    void descartaVagaSemJava() {
        assertTrue(rigido.avaliar(vaga("Desenvolvedor Python PJ", "Django", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Desenvolvedor JavaScript PJ", "React", "", "remoto")).descartada());
        assertTrue(rigido.avaliar(vaga("Produtor In Game PJ", "Roblox e Minecraft Java Edition", "", "remoto")).descartada());
        assertFalse(rigido.avaliar(vaga("Software Engineer PJ", "APIs com Spring Boot", "", "remoto")).descartada());
    }

    @Test
    void modoFlexivelMantemCltComNotaMenor() {
        Avaliacao a = flexivel.avaliar(vaga("Desenvolvedor Java", "Regime CLT", "", ""));

        assertFalse(a.descartada());
        assertTrue(a.motivos().contains("-30 parece ser só CLT"));
    }

    private final Pontuador comSuporte = new Pontuador(List.of("Spring Boot"), List.of("Windows", "GLPI"), true, true, false);

    @Test
    void reconheceVagaDeSuporteRemotaPj() {
        Avaliacao a = comSuporte.avaliar(vaga("Analista de Suporte Técnico N1", "Atendimento de chamados no GLPI, Windows. PJ", "", "remoto"));

        assertFalse(a.descartada());
        assertEquals(Categoria.SUPORTE, a.categoria());
        assertTrue(a.motivos().contains("+15 vaga de júnior"));
        assertEquals(List.of("Windows", "GLPI"), a.habilidadesEmComum());
        assertEquals(Categoria.SUPORTE, comSuporte.avaliar(vaga("Helpdesk PJ", "", "", "remoto")).categoria());
    }

    @Test
    void suporteSemContextoDeTiNaoContaComoSuporte() {
        assertEquals("não é vaga de Java nem de suporte",
                motivo(comSuporte.avaliar(vaga("Suporte ao Cliente PJ", "Atendimento a lojistas por chat", "", "remoto"))));
    }

    @Test
    void suporteSeguePjERemoto() {
        assertEquals("não diz que aceita PJ", motivo(comSuporte.avaliar(vaga("Helpdesk N1", "", "", "remoto"))));
        assertTrue(comSuporte.avaliar(vaga("Helpdesk N1 PJ", "Escala híbrida", "", "remoto")).descartada());
        assertTrue(comSuporte.avaliar(vaga("Coordenador de Service Desk PJ", "", "", "remoto")).descartada());
    }

    @Test
    void suporteNoturnoRemotoEntraMesmoSemPj() {
        Avaliacao a = comSuporte.avaliar(vaga("Analista de Service Desk", "Escala das 22h às 6h", "", "remoto"));

        assertFalse(a.descartada());
        assertTrue(a.noturna());
        assertTrue(a.motivos().contains("+10 turno noturno"));
        assertTrue(comSuporte.avaliar(vaga("Helpdesk Noturno", "", "", "")).descartada()); // continua exigindo remoto
    }

    @Test
    void devNoturnoSegueAsMesmasRegras() {
        assertEquals("não diz que aceita PJ",
                motivo(comSuporte.avaliar(vaga("Desenvolvedor Java Pleno", "Turno noturno", "", "remoto"))));
        Avaliacao a = comSuporte.avaliar(vaga("Desenvolvedor Java Pleno PJ", "Período noturno", "", "remoto"));
        assertFalse(a.descartada());
        assertTrue(a.noturna());
    }

    @Test
    void naoConfundeHorarioComercialComNoturno() {
        assertFalse(comSuporte.avaliar(vaga("Helpdesk PJ", "Das 8h às 18h", "", "remoto")).noturna());
        assertFalse(comSuporte.avaliar(vaga("Helpdesk PJ", "Das 10h às 19h", "", "remoto")).noturna());
        assertTrue(comSuporte.avaliar(vaga("Helpdesk PJ", "Plantão 00h as 06h", "", "remoto")).noturna());
    }
}
