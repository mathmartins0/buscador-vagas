package io.github.mathmartins0.vagas;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Configuração lida de config.json. */
public record Config(
        List<String> termosBusca,
        boolean apenasRemoto,
        boolean apenasPj,
        List<String> fontesDesligadas,
        String linkedinGeoId,
        int notaMinima,
        int diasMaximos,
        AlertasEmail alertasEmail,
        Perfil perfil,
        String modeloMensagem) {

    /** De quem são os e-mails de alerta (trecho do endereço) e quantos dias para trás ler. */
    public record AlertasEmail(List<String> remetentes, int dias) {
    }

    public record Perfil(
            String nome,
            String email,
            String telefone,
            String linkedin,
            String github,
            List<String> habilidades) {
    }

    public static Config carregar(Path arquivo) throws IOException {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(arquivo.toFile(), Config.class);
    }
}
