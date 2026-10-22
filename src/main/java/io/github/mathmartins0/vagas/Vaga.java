package io.github.mathmartins0.vagas;

import java.time.LocalDate;

/**
 * Uma vaga encontrada em alguma fonte.
 *
 * @param tipoContrato texto livre vindo da fonte (ex.: "vacancy_legal_entity" na Gupy); pode ser vazio
 * @param modalidade   "remoto", "hibrido", "presencial" ou vazio quando a fonte não informa
 */
public record Vaga(
        String id,
        String fonte,
        String titulo,
        String empresa,
        String local,
        String modalidade,
        String tipoContrato,
        String url,
        String descricao,
        LocalDate publicadaEm) {

    /** Chave única entre fontes, usada para não mostrar a mesma vaga duas vezes. */
    public String chave() {
        return fonte + ":" + id;
    }
}
