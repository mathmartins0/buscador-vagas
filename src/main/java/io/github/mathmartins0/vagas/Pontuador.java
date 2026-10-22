package io.github.mathmartins0.vagas;

import java.util.ArrayList;
import java.util.List;

/** Dá uma nota de aderência para a vaga, pensando em PJ, Java, pleno e remoto. */
public class Pontuador {

    private final List<String> habilidades;
    private final boolean apenasRemoto;

    public Pontuador(List<String> habilidades, boolean apenasRemoto) {
        this.habilidades = habilidades;
        this.apenasRemoto = apenasRemoto;
    }

    public Avaliacao avaliar(Vaga vaga) {
        String titulo = Texto.normalizar(vaga.titulo());
        String tudo = titulo + " " + Texto.normalizar(vaga.descricao());
        String contrato = Texto.normalizar(vaga.tipoContrato());
        List<String> motivos = new ArrayList<>();

        if (!Texto.contem(tudo, "java", "spring", "spring boot")) {
            return descartar("não menciona Java nem Spring");
        }
        if (Texto.contem(titulo, "estagio", "estagiario", "trainee", "aprendiz")) {
            return descartar("vaga de estágio ou trainee");
        }
        boolean pleno = Texto.contem(titulo, "pleno", "pl", "mid", "mid-level");
        boolean junior = Texto.contem(titulo, "junior", "jr");
        if (!pleno && !junior && Texto.contem(titulo, "senior", "sr", "especialista", "specialist", "spec",
                "expert", "lead", "lider", "staff", "principal", "arquiteto", "architect")) {
            return descartar("vaga sênior ou acima");
        }
        String modalidade = Texto.normalizar(vaga.modalidade());
        boolean remoto = modalidade.equals("remoto")
                || Texto.contem(titulo, "remoto", "remote", "home office")
                || Texto.contem(tudo, "100% remoto", "totalmente remoto", "100% remote", "fully remote");
        if (apenasRemoto && !remoto) {
            return descartar("não é remota");
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

        if (pleno) {
            nota += 20;
            motivos.add("+20 vaga de pleno");
        } else if (junior) {
            nota += 15;
            motivos.add("+15 vaga de júnior");
        } else {
            nota += 10;
            motivos.add("+10 nível não informado");
        }

        if (remoto) {
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
