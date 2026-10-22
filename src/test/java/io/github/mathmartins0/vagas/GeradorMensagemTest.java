package io.github.mathmartins0.vagas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeradorMensagemTest {

    private final Config.Perfil perfil = new Config.Perfil(
            "Matheus", "m@x.com", "12 9999", "linkedin.com/in/m", "github.com/m", List.of("Java", "Docker"));

    @Test
    void preencheOModeloComAsHabilidadesEmComum() {
        GeradorMensagem gerador = new GeradorMensagem(perfil, "Oi {empresa}, vaga {titulo}: {habilidades}. {nome}");
        Vaga vaga = new Vaga("1", "T", "Dev Java", "Acme", "", "", "", "", "", null);
        Avaliacao avaliacao = new Avaliacao(80, false, List.of(), List.of("Spring Boot", "SQL Server", "Docker"));

        assertEquals("Oi Acme, vaga Dev Java: Spring Boot, SQL Server e Docker. Matheus", gerador.gerar(vaga, avaliacao));
    }

    @Test
    void semHabilidadesEmComumUsaAsDoPerfil() {
        GeradorMensagem gerador = new GeradorMensagem(perfil, "{empresa}: {habilidades}");
        Vaga vaga = new Vaga("1", "T", "Dev", "", "", "", "", "", "", null);

        assertEquals("time: Java e Docker", gerador.gerar(vaga, new Avaliacao(50, false, List.of(), List.of())));
    }
}
