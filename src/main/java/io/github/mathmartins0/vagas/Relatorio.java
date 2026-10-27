package io.github.mathmartins0.vagas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Monta o relatório do dia em Markdown, da vaga com maior nota para a menor. */
public class Relatorio {

    static final String SEM_VAGAS = "Nenhuma vaga nova acima da nota mínima hoje.";

    public record Item(Vaga vaga, Avaliacao avaliacao, String mensagem) {
    }

    public String montar(LocalDate dia, List<Item> itens) {
        StringBuilder md = new StringBuilder();
        md.append("# Vagas de ").append(dia).append("\n\n");
        if (itens.isEmpty()) {
            return md.append(SEM_VAGAS).append("\n").toString();
        }
        md.append(itens.size()).append(" vagas novas. Abra o link, revise a mensagem e envie.\n\n");
        md.append("| Nota | Tipo | Vaga | Empresa | Local | Fonte |\n|---:|---|---|---|---|---|\n");
        for (Item item : itens) {
            Vaga v = item.vaga();
            md.append("| ").append(item.avaliacao().nota())
                    .append(" | ").append(tipo(item.avaliacao()))
                    .append(" | [").append(celula(v.titulo())).append("](").append(v.url()).append(")")
                    .append(" | ").append(celula(v.empresa()))
                    .append(" | ").append(celula(local(v)))
                    .append(" | ").append(v.fonte()).append(" |\n");
        }
        md.append("\n---\n");
        for (Item item : itens) {
            Vaga v = item.vaga();
            md.append("\n## ").append(item.avaliacao().nota()).append(" · ").append(tipo(item.avaliacao()))
                    .append(" · ").append(v.titulo())
                    .append(" (").append(v.empresa()).append(")\n\n")
                    .append("[Abrir vaga](").append(v.url()).append(")\n\n");
            for (String motivo : item.avaliacao().motivos()) {
                md.append("- ").append(motivo).append("\n");
            }
            md.append("\n<details><summary>Mensagem pronta</summary>\n\n```\n")
                    .append(item.mensagem())
                    .append("\n```\n</details>\n");
        }
        return md.toString();
    }

    /**
     * Grava o relatório do dia. Se o bot rodar de novo no mesmo dia, não apaga o que já estava lá:
     * vagas novas entram no fim do arquivo e uma execução sem novidades não muda nada.
     */
    public Path salvar(Path pasta, LocalDate dia, List<Item> itens) throws IOException {
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve(dia + ".md");
        if (!Files.exists(arquivo) || Files.readString(arquivo).contains(SEM_VAGAS)) {
            Files.writeString(arquivo, montar(dia, itens));
        } else if (!itens.isEmpty()) {
            String novas = montar(dia, itens).replaceFirst("^# .*\n", "## Mais vagas encontradas no mesmo dia\n");
            Files.writeString(arquivo, Files.readString(arquivo) + "\n---\n\n" + novas);
        }
        return arquivo;
    }

    private static String tipo(Avaliacao a) {
        String rotulo = a.categoria() == null ? "" : a.categoria().rotulo();
        return a.noturna() ? rotulo + " 🌙 noturna" : rotulo;
    }

    private static String local(Vaga v) {
        String modalidade = v.modalidade() == null ? "" : v.modalidade();
        if (v.local() == null || v.local().isBlank()) {
            return modalidade;
        }
        return modalidade.isBlank() ? v.local() : v.local() + " · " + modalidade;
    }

    private static String celula(String texto) {
        return texto == null ? "" : texto.replace("|", "/").replace("\n", " ");
    }
}
