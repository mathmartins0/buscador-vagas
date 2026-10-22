package io.github.mathmartins0.vagas;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** Guarda as vagas já mostradas, para o relatório trazer só novidades. */
public class Historico {

    private final Path arquivo;
    private final Set<String> vistas;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public Historico(Path arquivo) throws IOException {
        this.arquivo = arquivo;
        this.vistas = Files.exists(arquivo)
                ? json.readValue(arquivo.toFile(), new TypeReference<TreeSet<String>>() { })
                : new TreeSet<>();
    }

    public boolean jaVista(Vaga vaga) {
        return vistas.contains(vaga.chave());
    }

    public void marcar(Vaga vaga) {
        vistas.add(vaga.chave());
    }

    public void salvar() throws IOException {
        Files.createDirectories(arquivo.getParent());
        json.writeValue(arquivo.toFile(), vistas);
    }
}
