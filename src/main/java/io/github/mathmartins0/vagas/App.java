package io.github.mathmartins0.vagas;

import io.github.mathmartins0.vagas.fontes.BackendBrFonte;
import io.github.mathmartins0.vagas.fontes.EmailAlertasFonte;
import io.github.mathmartins0.vagas.fontes.FonteVagas;
import io.github.mathmartins0.vagas.fontes.GupyFonte;
import io.github.mathmartins0.vagas.fontes.InfoJobsFonte;
import io.github.mathmartins0.vagas.fontes.LinkedInFonte;
import io.github.mathmartins0.vagas.fontes.RemotarFonte;
import io.github.mathmartins0.vagas.fontes.VagasComFonte;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Busca vagas nas fontes, tira repetidas e já vistas, dá nota e gera o relatório do dia.
 * Uso: java -jar target/buscador-vagas.jar [config.json]
 */
public class App {

    private static String definida(String variavel) {
        String valor = System.getenv(variavel);
        return valor == null || valor.isBlank() ? "vazio" : "ok";
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.carregar(Path.of(args.length > 0 ? args[0] : "config.json"));
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

        List<FonteVagas> todas = List.of(
                new RemotarFonte(),
                new BackendBrFonte(),
                new VagasComFonte(),
                new InfoJobsFonte(),
                new GupyFonte(),
                new LinkedInFonte(config.linkedinGeoId(), config.apenasRemoto()));
        List<FonteVagas> fontes = new ArrayList<>(todas.stream()
                .filter(f -> config.fontesDesligadas() == null || !config.fontesDesligadas().contains(f.nome()))
                .toList());
        EmailAlertasFonte alertas = config.alertasEmail() == null ? null
                : EmailAlertasFonte.doAmbiente(config.alertasEmail().remetentes(), config.alertasEmail().dias());
        if (alertas != null) {
            fontes.add(alertas);
        } else {
            System.out.printf("Alertas por e-mail desligados (EMAIL_USUARIO %s, EMAIL_SENHA_APP %s).%n",
                    definida("EMAIL_USUARIO"), definida("EMAIL_SENHA_APP"));
        }

        Map<String, Vaga> encontradas = new LinkedHashMap<>();
        Map<String, FonteVagas> fontePorNome = new LinkedHashMap<>();
        for (FonteVagas fonte : fontes) {
            fontePorNome.put(fonte.nome(), fonte);
            List<String> termos = fonte.buscaPorTermo() ? config.termosBusca() : List.of("");
            for (String termo : termos) {
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
        Pontuador pontuador = new Pontuador(config.perfil().habilidades(), config.apenasRemoto(), config.apenasPj());
        GeradorMensagem gerador = new GeradorMensagem(config.perfil(), config.modeloMensagem());
        LocalDate limite = hoje.minusDays(config.diasMaximos());

        List<Relatorio.Item> itens = new ArrayList<>();
        Map<String, Integer> descartes = new TreeMap<>();
        for (Vaga vaga : encontradas.values()) {
            if (historico.jaVista(vaga) || (vaga.publicadaEm() != null && vaga.publicadaEm().isBefore(limite))) {
                continue;
            }
            try {
                FonteVagas origem = fontePorNome.get(vaga.fonte());
                vaga = origem == null ? vaga : origem.detalhar(vaga);
            } catch (Exception e) {
                System.err.printf("Detalhes de %s falharam (%s)%n", vaga.url(), e.getMessage());
                continue;
            }
            historico.marcar(vaga);
            Avaliacao avaliacao = pontuador.avaliar(vaga);
            descartes.merge(avaliacao.descartada() ? avaliacao.motivos().getFirst() : "entrou no relatório", 1, Integer::sum);
            if (!avaliacao.descartada() && avaliacao.nota() >= config.notaMinima()) {
                itens.add(new Relatorio.Item(vaga, avaliacao, gerador.gerar(vaga, avaliacao)));
            }
        }
        itens.sort(Comparator.comparingInt((Relatorio.Item i) -> i.avaliacao().nota()).reversed());

        Relatorio relatorio = new Relatorio();
        Path arquivo = relatorio.salvar(Path.of("relatorios"), hoje, itens);
        historico.salvar();
        System.out.printf("%d vagas no relatório: %s%n", itens.size(), arquivo);
        descartes.forEach((motivo, total) -> System.out.printf("  %3d · %s%n", total, motivo));
    }
}
