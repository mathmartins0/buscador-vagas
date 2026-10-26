package io.github.mathmartins0.vagas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioTest {

    private final Relatorio relatorio = new Relatorio();
    private final LocalDate dia = LocalDate.of(2026, 10, 26);

    private static Relatorio.Item item(String titulo) {
        Vaga v = new Vaga(titulo, "T", titulo, "Acme", "", "remoto", "", "https://x/" + titulo, "", null);
        return new Relatorio.Item(v, new Avaliacao(80, false, List.of("+30 PJ"), List.of()), "Olá");
    }

    @Test
    void segundaExecucaoSemNovidadesNaoApagaORelatorio(@TempDir Path pasta) throws Exception {
        Path arquivo = relatorio.salvar(pasta, dia, List.of(item("Vaga A")));
        String antes = Files.readString(arquivo);

        relatorio.salvar(pasta, dia, List.of());

        assertEquals(antes, Files.readString(arquivo));
    }

    @Test
    void segundaExecucaoComNovidadesAcrescentaNoFim(@TempDir Path pasta) throws Exception {
        relatorio.salvar(pasta, dia, List.of(item("Vaga A")));

        String conteudo = Files.readString(relatorio.salvar(pasta, dia, List.of(item("Vaga B"))));

        assertTrue(conteudo.contains("Vaga A"));
        assertTrue(conteudo.contains("## Mais vagas encontradas no mesmo dia"));
        assertTrue(conteudo.indexOf("Vaga B") > conteudo.indexOf("Vaga A"));
    }

    @Test
    void relatorioVazioEhSubstituidoQuandoAparecemVagas(@TempDir Path pasta) throws Exception {
        relatorio.salvar(pasta, dia, List.of());

        String conteudo = Files.readString(relatorio.salvar(pasta, dia, List.of(item("Vaga A"))));

        assertTrue(conteudo.startsWith("# Vagas de 2026-10-26"));
        assertTrue(conteudo.contains("Vaga A"));
    }
}
