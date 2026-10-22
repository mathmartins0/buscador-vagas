package io.github.mathmartins0.vagas;

import io.github.mathmartins0.vagas.fontes.FonteVagas;
import io.github.mathmartins0.vagas.fontes.GupyFonte;
import io.github.mathmartins0.vagas.fontes.LinkedInFonte;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca vagas nas fontes, tira repetidas e já vistas, dá nota e gera o relatório do dia.
 * Uso: java -jar target/buscador-vagas.jar [config.json]
 */
public class App {

    public static void main(String[] args) throws Exception {
        Config config = Config.carregar(Path.of(args.length > 0 ? args[0] : "config.json"));
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        List<FonteVagas> fontes = new ArrayList<>();
        fontes.add(new GupyFonte());
        if (config.linkedinAtivo()) {
            fontes.add(new LinkedInFonte(config.linkedinLocal()));
        }

        Map<String, Vaga> encontradas = new LinkedHashMap<>();
        for (FonteVagas fonte : fontes) {
            for (String termo : config.termosBusca()) {
                try {
                    List<Vaga> vagas = fonte.buscar(termo);
                    vagas.forEach(v -> encontradas.putIfAbsent(v.chave(), v));
                    System.out.printf("%s \"%s\": %d vagas%n", fonte.nome(), termo, vagas.size());
                } catch (Exception e) {
                    System.err.printf("%s \"%s\": falhou (%s)%n", fonte.nome(), termo, e.getMessage());
                }
                Thread.sleep(1500);
            }
        }

        if (encontradas.isEmpty()) {
            System.err.println("Nenhuma fonte respondeu. Verifique a conexão ou se alguma API mudou.");
            System.exit(1);
        }

        Historico historico = new Historico(Path.of("data/vistas.json"));
        Pontuador pontuador = new Pontuador(config.perfil().habilidades());
        GeradorMensagem gerador = new GeradorMensagem(config.perfil(), config.modeloMensagem());
        LocalDate limite = hoje.minusDays(config.diasMaximos());

        List<Relatorio.Item> itens = new ArrayList<>();
        for (Vaga vaga : encontradas.values()) {
            if (historico.jaVista(vaga) || (vaga.publicadaEm() != null && vaga.publicadaEm().isBefore(limite))) {
                continue;
            }
            historico.marcar(vaga);
            Avaliacao avaliacao = pontuador.avaliar(vaga);
            if (!avaliacao.descartada() && avaliacao.nota() >= config.notaMinima()) {
                itens.add(new Relatorio.Item(vaga, avaliacao, gerador.gerar(vaga, avaliacao)));
            }
        }
        itens.sort(Comparator.comparingInt((Relatorio.Item i) -> i.avaliacao().nota()).reversed());

        Relatorio relatorio = new Relatorio();
        Path arquivo = relatorio.salvar(Path.of("relatorios"), hoje, relatorio.montar(hoje, itens));
        historico.salvar();
        System.out.printf("%d vagas no relatório: %s%n", itens.size(), arquivo);
    }
}
