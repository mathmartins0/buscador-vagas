package io.github.mathmartins0.vagas;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Configuração lida de config.json. */
public record Config(
        List<String> termosBusca,
        List<String> termosBuscaSuporte,
        List<String> termosBuscaQa,
        boolean apenasRemoto,
        boolean apenasPj,
        boolean suporteNoturnoExigePj,
        List<String> fontesDesligadas,
        String linkedinGeoId,
        int notaMinima,
        int diasMaximos,
        AlertasEmail alertasEmail,
        Jooble jooble,
        Perfil perfil,
        String modeloMensagem,
        String modeloMensagemSuporte,
        String modeloMensagemQa) {

    /** De quem são os e-mails de alerta (trecho do endereço) e quantos dias para trás ler. */
    public record AlertasEmail(List<String> remetentes, int dias) {
    }

    /** Termos buscados no Jooble (poucos, porque a chave tem cota de requisições). */
    public record Jooble(List<String> termos) {
    }

    public record Perfil(
            String nome,
            String email,
            String telefone,
            String linkedin,
            String github,
            List<String> habilidades,
            List<String> habilidadesSuporte,
            List<String> habilidadesQa) {
    }

    /** Termos de dev + suporte + QA, sem repetir. */
    public List<String> todosOsTermos() {
        java.util.LinkedHashSet<String> termos = new java.util.LinkedHashSet<>(termosBusca);
        if (termosBuscaSuporte != null) {
            termos.addAll(termosBuscaSuporte);
        }
        if (termosBuscaQa != null) {
            termos.addAll(termosBuscaQa);
        }
        return List.copyOf(termos);
    }

    public static Config carregar(Path arquivo) throws IOException {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(arquivo.toFile(), Config.class);
    }
}
