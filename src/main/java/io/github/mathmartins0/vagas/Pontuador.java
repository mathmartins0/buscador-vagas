package io.github.mathmartins0.vagas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filtra e dá nota de aderência para a vaga.
 * Primeiro decide se é vaga de dev Java ou de suporte técnico/helpdesk e descarta o que não serve
 * (outra área, sênior, não remota, sem PJ explícito); o que sobra é ordenado por nível, stack,
 * sinais de PJ e turno noturno.
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
    private static final String[] CARGOS_HELPDESK = {
            "helpdesk", "help desk", "help-desk", "service desk", "servicedesk", "suporte tecnico",
            "suporte de ti", "suporte ti", "tecnico de ti", "tecnico de informatica", "tecnico em informatica"};
    private static final String[] CONTEXTO_TI = {
            "ti", "tecnico", "tecnica", "informatica", "n1", "n2", "n3", "nivel 1", "nivel 2", "windows", "linux",
            "redes", "hardware", "chamados", "glpi", "itil", "active directory", "office 365", "infraestrutura"};
    private static final String[] PALAVRAS_NOTURNO = {
            "noturno", "noturna", "madrugada", "turno da noite", "periodo da noite", "overnight", "night shift",
            "terceiro turno", "3o turno", "3º turno"};
    /** Horários como "22h às 6h", "23:00 - 07:00", "das 0h as 6h". */
    private static final Pattern HORARIO_NOTURNO = Pattern.compile(
            "(?<![0-9])(19|2[0-3]|0?0)\\s*(h|:)\\s*\\d{0,2}\\s*(as|a|-|ate)\\s*0?[0-8]\\s*(h|:)");

    private final List<String> habilidadesDev;
    private final List<String> habilidadesSuporte;
    private final boolean apenasRemoto;
    private final boolean apenasPj;
    private final boolean suporteNoturnoExigePj;

    public Pontuador(List<String> habilidadesDev, List<String> habilidadesSuporte, boolean apenasRemoto,
                     boolean apenasPj, boolean suporteNoturnoExigePj) {
        this.habilidadesDev = habilidadesDev;
        this.habilidadesSuporte = habilidadesSuporte;
        this.apenasRemoto = apenasRemoto;
        this.apenasPj = apenasPj;
        this.suporteNoturnoExigePj = suporteNoturnoExigePj;
    }

    public Pontuador(List<String> habilidades, boolean apenasRemoto, boolean apenasPj) {
        this(habilidades, List.of(), apenasRemoto, apenasPj, true);
    }

    public Avaliacao avaliar(Vaga vaga) {
        String titulo = Texto.normalizar(vaga.titulo());
        String contrato = Texto.normalizar(vaga.tipoContrato());
        // título + tags/rótulos da fonte, que em várias fontes trazem nível e modalidade
        String cabecalho = titulo + " " + contrato;
        String descricao = Texto.normalizar(vaga.descricao());
        String tudo = cabecalho + " " + descricao;
        List<String> motivos = new ArrayList<>();

        Categoria categoria = categoria(titulo, cabecalho, descricao, tudo);
        if (categoria == null) {
            return descartar("não é vaga de Java nem de suporte");
        }
        if (Texto.contem(titulo, "estagio", "estagiario", "trainee", "aprendiz")) {
            return descartar("vaga de estágio ou trainee");
        }
        boolean pleno = Texto.contem(cabecalho, "pleno", "pl", "mid", "mid-level", "n2", "nivel 2");
        boolean junior = Texto.contem(cabecalho, "junior", "jr", "n1", "nivel 1");
        if (!pleno && !junior && Texto.contem(cabecalho, "senior", "sr", "especialista", "specialist", "spec",
                "expert", "lead", "lider", "staff", "principal", "arquiteto", "architect", "coordenador",
                "supervisor", "gerente", "n3", "nivel 3")) {
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

        boolean noturna = Texto.contem(tudo, PALAVRAS_NOTURNO) || HORARIO_NOTURNO.matcher(tudo).find();
        boolean pj = contrato.contains("legal_entity") || Texto.contem(tudo, PALAVRAS_PJ);
        boolean exigePj = apenasPj && !(categoria == Categoria.SUPORTE && noturna && !suporteNoturnoExigePj);
        if (exigePj && !pj) {
            return descartar("não diz que aceita PJ");
        }

        int nota = 0;
        if (categoria == Categoria.DEV && Texto.contem(titulo, "java", "backend", "back-end", "back end", "spring")) {
            nota += 20;
            motivos.add("+20 título de Java/backend");
        } else if (categoria == Categoria.SUPORTE) {
            nota += 20;
            motivos.add("+20 vaga de suporte técnico/helpdesk");
        }
        if (pj) {
            nota += 30;
            motivos.add(Texto.contem(cabecalho, PALAVRAS_PJ) || contrato.contains("legal_entity")
                    ? "+30 PJ informado pela fonte ou no título"
                    : "+30 PJ citado na descrição");
        } else if (contrato.contains("effective") || Texto.contem(tudo, "clt")) {
            nota -= 30;
            motivos.add("-30 parece ser só CLT");
        } else if (!exigePj) {
            motivos.add("+0 não diz se é PJ (suporte noturno entra mesmo assim)");
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
        if (noturna) {
            nota += 10;
            motivos.add("+10 turno noturno");
        }

        List<String> emComum = new ArrayList<>();
        for (String habilidade : categoria == Categoria.DEV ? habilidadesDev : habilidadesSuporte) {
            if (Texto.contem(tudo, habilidade)) {
                emComum.add(habilidade);
            }
        }
        int bonus = Math.min(15, emComum.size() * 3);
        if (bonus > 0) {
            nota += bonus;
            motivos.add("+" + bonus + " habilidades em comum: " + String.join(", ", emComum));
        }

        return new Avaliacao(Math.max(0, Math.min(100, nota)), false, motivos, emComum, categoria, noturna);
    }

    /**
     * Dev: Java no título/tags, Spring na descrição, ou Java citado mais de uma vez na descrição
     * (evita vagas que só citam Java de passagem, como "Minecraft Java Edition").
     * Suporte: helpdesk/service desk no título, ou "suporte" no título com contexto de TI.
     */
    private static Categoria categoria(String titulo, String cabecalho, String descricao, String tudo) {
        if (Texto.contem(cabecalho, "java", "spring", "spring boot")
                || Texto.contem(descricao, "spring", "spring boot")
                || Texto.contar(descricao, "java") >= 2) {
            return Categoria.DEV;
        }
        if (Texto.contem(titulo, CARGOS_HELPDESK)
                || (Texto.contem(titulo, "suporte", "support") && Texto.contem(tudo, CONTEXTO_TI))) {
            return Categoria.SUPORTE;
        }
        return null;
    }

    private static Avaliacao descartar(String motivo) {
        return new Avaliacao(0, true, List.of(motivo), List.of(), null, false);
    }
}
