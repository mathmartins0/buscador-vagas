package io.github.mathmartins0.vagas;

import java.util.List;

/**
 * Resultado da análise de uma vaga.
 *
 * @param nota       de 0 a 100
 * @param descartada true quando a vaga não serve de jeito nenhum (ex.: estágio, sem Java)
 * @param motivos    explicação curta de cada ponto somado ou tirado
 * @param habilidadesEmComum habilidades do perfil que aparecem na vaga
 * @param categoria  dev ou suporte; null quando descartada antes de saber
 * @param noturna    true quando a vaga é no turno da noite/madrugada
 */
public record Avaliacao(int nota, boolean descartada, List<String> motivos, List<String> habilidadesEmComum,
                        Categoria categoria, boolean noturna) {

    public Avaliacao(int nota, boolean descartada, List<String> motivos, List<String> habilidadesEmComum) {
        this(nota, descartada, motivos, habilidadesEmComum, Categoria.DEV, false);
    }
}
