package io.github.mathmartins0.vagas;

import java.util.List;

/** Preenche o modelo de mensagem do config.json com os dados da vaga (um modelo para dev, outro para suporte). */
public class GeradorMensagem {

    private static final int MAX_HABILIDADES = 4;

    private final Config.Perfil perfil;
    private final String modeloDev;
    private final String modeloSuporte;

    public GeradorMensagem(Config.Perfil perfil, String modeloDev, String modeloSuporte) {
        this.perfil = perfil;
        this.modeloDev = modeloDev;
        this.modeloSuporte = modeloSuporte == null || modeloSuporte.isBlank() ? modeloDev : modeloSuporte;
    }

    public GeradorMensagem(Config.Perfil perfil, String modelo) {
        this(perfil, modelo, modelo);
    }

    public String gerar(Vaga vaga, Avaliacao avaliacao) {
        boolean suporte = avaliacao.categoria() == Categoria.SUPORTE;
        List<String> doPerfil = suporte && perfil.habilidadesSuporte() != null
                ? perfil.habilidadesSuporte() : perfil.habilidades();
        List<String> destaque = avaliacao.habilidadesEmComum().isEmpty() ? doPerfil : avaliacao.habilidadesEmComum();
        String habilidades = juntar(destaque.subList(0, Math.min(MAX_HABILIDADES, destaque.size())));

        return (suporte ? modeloSuporte : modeloDev)
                .replace("{empresa}", vazioSe(vaga.empresa(), "time"))
                .replace("{titulo}", vaga.titulo())
                .replace("{habilidades}", habilidades)
                .replace("{turno}", avaliacao.noturna() ? ", inclusive no turno da noite" : "")
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
