package io.github.mathmartins0.vagas;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtra e dá nota de aderência para a vaga.
 * Primeiro descarta o que não serve (sem Java, sênior, não remota, sem PJ explícito);
 * o que sobra é ordenado por nível, stack e sinais de PJ.
 */
public class Pontuador {

    private static final String[] PALAVRAS_HIBRIDO = {
            "hibrido", "hibrida", "hybrid", "presencial", "presenciais", "on-site", "onsite",
            "no escritorio", "in-office", "in office"};
    private static final String[] PALAVRAS_REMOTO = {
            "remoto", "remota", "remote", "home office", "home-office", "100% remoto", "totalmente remoto"};
    private static final String[] PALAVRAS_PJ = {
            "pj", "pessoa juridica", "cnpj", "prestador de servico", "prestador de servicos",
            "prestacao de servico", "prestacao de servicos"};

    private final List<String> habilidades;
    private final boolean apenasRemoto;
    private final boolean apenasPj;

    public Pontuador(List<String> habilidades, boolean apenasRemoto, boolean apenasPj) {
        this.habilidades = habilidades;
        this.apenasRemoto = apenasRemoto;
        this.apenasPj = apenasPj;
    }

    public Avaliacao avaliar(Vaga vaga) {
        String titulo = Texto.normalizar(vaga.titulo());
        String contrato = Texto.normalizar(vaga.tipoContrato());
        // título + tags/rótulos da fonte, que em várias fontes trazem nível e modalidade
        String cabecalho = titulo + " " + contrato;
        String tudo = cabecalho + " " + Texto.normalizar(vaga.descricao());
        List<String> motivos = new ArrayList<>();

        // Java no título/tags, Spring na descrição, ou Java citado mais de uma vez na descrição.
        // Evita vagas que só citam Java de passagem (ex.: "Minecraft Java Edition").
        String descricao = Texto.normalizar(vaga.descricao());
        if (!Texto.contem(cabecalho, "java", "spring", "spring boot")
                && !Texto.contem(descricao, "spring", "spring boot")
                && Texto.contar(descricao, "java") < 2) {
            return descartar("não é vaga de Java");
        }
        if (Texto.contem(titulo, "estagio", "estagiario", "trainee", "aprendiz")) {
            return descartar("vaga de estágio ou trainee");
        }
        boolean pleno = Texto.contem(cabecalho, "pleno", "pl", "mid", "mid-level");
        boolean junior = Texto.contem(cabecalho, "junior", "jr");
        if (!pleno && !junior && Texto.contem(cabecalho, "senior", "sr", "especialista", "specialist", "spec",
                "expert", "lead", "lider", "staff", "principal", "arquiteto", "architect")) {
            return descartar("vaga sênior ou acima");
        }
        if (Texto.contem(contrato, "vaga internacional")) {
            return descartar("vaga internacional");
        }

        // Qualquer menção a híbrido ou presencial derruba a vaga, mesmo se a fonte disser "remoto":
        // é comum "100% home office fora de SP, presencial 1x por semana para quem mora em SP".
        boolean mencionaHibrido = Texto.contem(tudo, PALAVRAS_HIBRIDO);
        boolean remoto = !mencionaHibrido
                && (Texto.normalizar(vaga.modalidade()).equals("remoto") || Texto.contem(cabecalho, PALAVRAS_REMOTO)
                || Texto.contem(tudo, "100% remoto", "totalmente remoto", "100% remote", "fully remote",
                        "100% home office"));
        if (apenasRemoto && !remoto) {
            return descartar(mencionaHibrido ? "menciona híbrido ou presencial" : "não diz que é remota");
        }

        boolean pj = contrato.contains("legal_entity") || Texto.contem(tudo, PALAVRAS_PJ);
        if (apenasPj && !pj) {
            return descartar("não diz que aceita PJ");
        }

        int nota = 0;
        if (Texto.contem(titulo, "java", "backend", "back-end", "back end", "spring")) {
            nota += 20;
            motivos.add("+20 título de Java/backend");
        }
        if (pj) {
            nota += 30;
            motivos.add(Texto.contem(cabecalho, PALAVRAS_PJ) || contrato.contains("legal_entity")
                    ? "+30 PJ informado pela fonte ou no título"
                    : "+30 PJ citado na descrição");
        } else if (contrato.contains("effective") || Texto.contem(tudo, "clt")) {
            nota -= 30;
            motivos.add("-30 parece ser só CLT");
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
