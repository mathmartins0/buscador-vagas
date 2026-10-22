package io.github.mathmartins0.vagas;

import java.util.ArrayList;
import java.util.List;

/** Dá uma nota de aderência para a vaga, pensando em PJ, Java, pleno e remoto. */
public class Pontuador {

    private final List<String> habilidades;

    public Pontuador(List<String> habilidades) {
        this.habilidades = habilidades;
    }

    public Avaliacao avaliar(Vaga vaga) {
        String titulo = Texto.normalizar(vaga.titulo());
        String tudo = titulo + " " + Texto.normalizar(vaga.descricao());
        String contrato = Texto.normalizar(vaga.tipoContrato());
        List<String> motivos = new ArrayList<>();

        if (!Texto.contem(tudo, "java", "spring", "spring boot")) {
            return descartar("não menciona Java nem Spring");
        }
        if (Texto.contem(titulo, "estagio", "estagiario", "trainee", "junior", "jr", "aprendiz")) {
            return descartar("vaga de nível júnior ou estágio");
        }

        int nota = 0;

        if (Texto.contem(titulo, "java", "backend", "back-end", "back end", "spring")) {
            nota += 20;
            motivos.add("+20 título de Java/backend");
        }

        boolean pj = contrato.contains("legal_entity")
                || Texto.contem(contrato, "contrato", "contract", "freelance", "temporario")
                || Texto.contem(tudo, "pj", "pessoa juridica", "contrato pj", "cnpj", "prestador de servico");
        boolean clt = contrato.contains("effective") || Texto.contem(tudo, "clt");
        if (pj) {
            nota += 30;
            motivos.add("+30 aceita PJ");
        } else if (clt) {
            nota -= 30;
            motivos.add("-30 parece ser só CLT");
        } else {
            motivos.add("+0 tipo de contrato não informado");
        }

        if (Texto.contem(titulo, "pleno", "pl", "mid", "mid-level")) {
            nota += 20;
            motivos.add("+20 vaga de pleno");
        } else if (Texto.contem(titulo, "senior", "sr", "especialista", "lead", "lider")) {
            nota += 5;
            motivos.add("+5 vaga sênior (seu histórico cobre)");
        } else {
            nota += 10;
            motivos.add("+10 nível não informado");
        }

        String modalidade = Texto.normalizar(vaga.modalidade());
        if (modalidade.equals("remoto") || Texto.contem(tudo, "100% remoto", "home office", "remote")) {
            nota += 15;
            motivos.add("+15 remoto");
        } else if (modalidade.equals("hibrido") || Texto.contem(tudo, "hibrido")) {
            nota += 5;
            motivos.add("+5 híbrido");
        }

        List<String> emComum = new ArrayList<>();
        for (String habilidade : habilidades) {
            if (Texto.contem(tudo, habilidade)) {
                emComum.add(habilidade);
            }
        }
        int bonus = Math.min(15, emComum.size() * 3);
        if (bonus > 0) {
            nota += bonus;
            motivos.add("+" + bonus + " habilidades em comum: " + String.join(", ", emComum));
        }

        return new Avaliacao(Math.max(0, Math.min(100, nota)), false, motivos, emComum);
    }

    private static Avaliacao descartar(String motivo) {
        return new Avaliacao(0, true, List.of(motivo), List.of());
    }
}
