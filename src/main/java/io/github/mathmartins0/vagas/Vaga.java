package io.github.mathmartins0.vagas;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern GUPY_JOB_ID = Pattern.compile("\"jobId\"\\s*:\\s*(\\d+)");

    /** Chave única entre fontes, usada para não mostrar a mesma vaga duas vezes. */
    public String chave() {
        return fonte + ":" + id;
    }

    /**
     * Chave que identifica a vaga no site onde ela de fato está publicada.
     * Ex.: a Remotar repassa vagas da Gupy; o link traz o jobId da Gupy codificado em base64.
     */
    public String chaveDeOrigem() {
        try {
            URI uri = URI.create(url);
            if (uri.getHost() != null && uri.getHost().endsWith("gupy.io") && uri.getPath().startsWith("/job/")) {
                String codigo = uri.getPath().substring("/job/".length());
                String json = new String(Base64.getDecoder().decode(codigo), StandardCharsets.UTF_8);
                Matcher m = GUPY_JOB_ID.matcher(json);
                if (m.find()) {
                    return "Gupy:" + m.group(1);
                }
            }
        } catch (IllegalArgumentException e) {
            // link fora do padrão: fica com a chave da própria fonte
        }
        return chave();
    }
}
