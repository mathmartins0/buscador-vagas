package io.github.mathmartins0.vagas;

/** Tipo de vaga que o bot procura. */
public enum Categoria {
    DEV("Dev Java"),
    SUPORTE("Suporte/Helpdesk");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
