package io.github.mathmartins0.vagas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Monta o relatório do dia em Markdown, da vaga com maior nota para a menor. */
public class Relatorio {

    public record Item(Vaga vaga, Avaliacao avaliacao, String mensagem) {
    }

    public String montar(LocalDate dia, List<Item> itens) {
        StringBuilder md = new StringBuilder();
        md.append("# Vagas de ").append(dia).append("\n\n");
        if (itens.isEmpty()) {
            return md.append("Nenhuma vaga nova acima da nota mínima hoje.\n").toString();
        }
        md.append(itens.size()).append(" vagas novas. Abra o link, revise a mensagem e envie.\n\n");
        md.append("| Nota | Vaga | Empresa | Local | Fonte |\n|---:|---|---|---|---|\n");
        for (Item item : itens) {
            Vaga v = item.vaga();
            md.append("| ").append(item.avaliacao().nota())
                    .append(" | [").append(celula(v.titulo())).append("](").append(v.url()).append(")")
                    .append(" | ").append(celula(v.empresa()))
                    .append(" | ").append(celula(local(v)))
                    .append(" | ").append(v.fonte()).append(" |\n");
        }
        md.append("\n---\n");
        for (Item item : itens) {
            Vaga v = item.vaga();
            md.append("\n## ").append(item.avaliacao().nota()).append(" · ").append(v.titulo())
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

    public Path salvar(Path pasta, LocalDate dia, String conteudo) throws IOException {
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve(dia + ".md");
        Files.writeString(arquivo, conteudo);
        return arquivo;
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
