package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;

import java.io.IOException;
import java.util.List;

/** Um lugar de onde vêm vagas (Gupy, LinkedIn...). */
public interface FonteVagas {

    String nome();

    List<Vaga> buscar(String termo) throws IOException, InterruptedException;
}
