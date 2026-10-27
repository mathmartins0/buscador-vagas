package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeitorAlertaEmailTest {

    private final LeitorAlertaEmail leitor = new LeitorAlertaEmail();
    private final LocalDate hoje = LocalDate.of(2026, 10, 26);

    @Test
    void extraiVagasDeUmAlertaDoIndeed() {
        String html = """
                <table>
                 <tr><td><a href="https://br.indeed.com/rc/clk/dl?jk=abc123&from=ja">Desenvolvedor Java Pleno</a>
                   <div>Acme Tecnologia</div><div>Remoto</div><div>Contratação PJ, Spring Boot</div></td></tr>
                 <tr><td><a href="https://br.indeed.com/rc/clk/dl?jk=def456&from=ja">Analista de Sistemas Java</a>
                   <div>Beta</div><div>São Paulo, SP</div></td></tr>
                 <tr><td><a href="https://br.indeed.com/jobs?q=java+pj">Ver todas as vagas</a>
                   <a href="https://br.indeed.com/unsubscribe">Cancelar inscrição</a></td></tr>
                </table>
                """;

        List<Vaga> vagas = leitor.ler("Indeed <alert@indeed.com>", "java pj - 2 novas vagas", html, hoje);

        assertEquals(2, vagas.size());
        Vaga v = vagas.getFirst();
        assertEquals("Alerta Indeed", v.fonte());
        assertEquals("Desenvolvedor Java Pleno", v.titulo());
        assertEquals("https://br.indeed.com/rc/clk/dl?jk=abc123&from=ja", v.url());
        assertTrue(v.descricao().contains("Remoto"));
        assertTrue(v.descricao().contains("Acme Tecnologia"));
        assertTrue(v.tipoContrato().startsWith("PJ (busca do alerta"));
        assertEquals(hoje, v.publicadaEm());
    }

    @Test
    void semPjNoAssuntoNaoMarcaComoPj() {
        String html = "<div><a href=\"https://www.programathor.com.br/jobs/123-dev-java\">Desenvolvedor Java</a> Remoto</div>";

        List<Vaga> vagas = leitor.ler("Programathor <contato@programathor.com.br>", "Novas vagas de Java", html, hoje);

        assertEquals(1, vagas.size());
        assertEquals("", vagas.getFirst().tipoContrato());
    }

    @Test
    void mesmaVagaEmDoisEmailsGeraOMesmoId() {
        String a = "<div><a href=\"https://br.indeed.com/rc/clk?jk=1&t=x\">Dev Java Pleno</a> Acme · Remoto · há 1 dia</div>";
        String b = "<div><a href=\"https://br.indeed.com/rc/clk?jk=1&t=y\">Dev Java Pleno</a> Acme · Remoto · há 2 dias</div>";

        assertEquals(leitor.ler("alert@indeed.com", "java pj", a, hoje).getFirst().id(),
                leitor.ler("alert@indeed.com", "java pj", b, hoje).getFirst().id());
    }

    @Test
    void aceitaLinksDeRastreamentoDoResumoDoProgramathor() {
        String html = """
                <table><tr><td><a href="https://corp1.programathor.com.br/trc/c/abc123">Desenvolvedor Java Pleno</a>
                  <p>Acme · Remoto · PJ</p></td></tr>
                <tr><td><a href="https://corp1.programathor.com.br/trc/c/zzz">Ver todas as vagas</a></td></tr></table>
                """;

        List<Vaga> vagas = leitor.ler("ProgramaThor <contato@programathor.com.br>",
                "Resumo Semanal: 3 oportunidade(s) combina(m) com seu perfil", html, hoje);

        assertEquals(1, vagas.size());
        assertEquals("Desenvolvedor Java Pleno", vagas.getFirst().titulo());
    }

    @Test
    void ignoraLinksQueNaoSaoCargos() {
        String html = """
                <div><a href="https://engage.indeed.com/f/a/1">Editar</a> java pj Home-Office</div>
                <div><a href="https://engage.indeed.com/f/a/2">desde ontem</a></div>
                <div><a href="https://engage.indeed.com/f/a/3">Crie seu Currículo Indeed</a></div>
                <div><a href="https://engage.indeed.com/f/a/4">Desenvolvedor Java Pleno</a> Acme · Remoto</div>
                """;

        List<Vaga> vagas = leitor.ler("Indeed <alert@indeed.com>", "java pj - novas vagas", html, hoje);

        assertEquals(List.of("Desenvolvedor Java Pleno"), vagas.stream().map(Vaga::titulo).toList());
    }

    @Test
    void ignoraEmailsDeConfirmacaoDeConta() {
        String html = "<div><a href=\"https://engage.indeed.com/f/a/4\">Desenvolvedor Java Pleno</a></div>";

        assertTrue(leitor.ler("alert@indeed.com", "O seu alerta de vagas para: java pj foi ativado com sucesso!", html, hoje).isEmpty());
        assertTrue(leitor.ler("x@jobbol.com.br", "Confirme seu e-mail - Jobbol", html, hoje).isEmpty());
    }

    @Test
    void identificaOSitePeloRemetente() {
        assertEquals("glassdoor", LeitorAlertaEmail.site("Glassdoor Jobs <noreply@glassdoor.com>"));
        assertEquals("exemplo.com", LeitorAlertaEmail.site("vagas@exemplo.com"));
    }
}
