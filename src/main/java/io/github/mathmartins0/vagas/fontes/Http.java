package io.github.mathmartins0.vagas.fontes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Cliente HTTP compartilhado pelas fontes. */
final class Http {

    private static final String NAVEGADOR =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Http() {
    }

    static String get(String url) throws IOException, InterruptedException {
        return get(url, Map.of());
    }

    static String get(String url, Map<String, String> cabecalhos) throws IOException, InterruptedException {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json, text/html")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .header("User-Agent", NAVEGADOR)
                .GET();
        cabecalhos.forEach(requisicao::header);
        HttpResponse<String> resposta = CLIENTE.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() == 429) {
            // "muitas requisições": espera um pouco e tenta uma vez de novo
            Thread.sleep(20_000);
            resposta = CLIENTE.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
        }
        if (resposta.statusCode() != 200) {
            throw new IOException("HTTP " + resposta.statusCode() + " em " + url);
        }
        return resposta.body();
    }
}
