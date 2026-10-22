package io.github.mathmartins0.vagas;

import java.util.List;

/** Preenche o modelo de mensagem do config.json com os dados da vaga. */
public class GeradorMensagem {

    private static final int MAX_HABILIDADES = 4;

    private final Config.Perfil perfil;
    private final String modelo;

    public GeradorMensagem(Config.Perfil perfil, String modelo) {
        this.perfil = perfil;
        this.modelo = modelo;
    }

    public String gerar(Vaga vaga, Avaliacao avaliacao) {
        List<String> destaque = avaliacao.habilidadesEmComum().isEmpty()
                ? perfil.habilidades()
                : avaliacao.habilidadesEmComum();
        String habilidades = juntar(destaque.subList(0, Math.min(MAX_HABILIDADES, destaque.size())));

        return modelo
                .replace("{empresa}", vazioSe(vaga.empresa(), "time"))
                .replace("{titulo}", vaga.titulo())
                .replace("{habilidades}", habilidades)
                .replace("{nome}", perfil.nome())
                .replace("{email}", perfil.email())
                .replace("{telefone}", perfil.telefone())
                .replace("{linkedin}", perfil.linkedin())
                .replace("{github}", perfil.github());
    }

    /** "a, b e c" */
    static String juntar(List<String> itens) {
        if (itens.size() <= 1) {
            return String.join("", itens);
        }
        return String.join(", ", itens.subList(0, itens.size() - 1)) + " e " + itens.getLast();
    }

    private static String vazioSe(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
