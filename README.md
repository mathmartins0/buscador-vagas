# Buscador de Vagas

Bot em Java 21 que procura vagas de desenvolvedor Java, dá uma nota de aderência (PJ, pleno, remoto, stack) e prepara uma mensagem de candidatura personalizada para cada uma. Ele **não se candidata sozinho**: você abre o link, revisa a mensagem e envia. Assim nenhuma conta corre risco de bloqueio por automação.

## Como funciona

1. Busca os termos do `config.json` na **Gupy** (API pública do portal de vagas) e, se ativado, na **busca pública do LinkedIn** (a lista que aparece sem login; não usa sua conta).
2. Remove repetidas, vagas já mostradas em dias anteriores (`data/vistas.json`) e vagas mais antigas que `diasMaximos`.
3. Dá uma nota de 0 a 100 para cada vaga:

| Critério | Pontos |
|---|---:|
| Título de Java/backend | +20 |
| Aceita PJ (tipo PJ na Gupy, ou "PJ", "CNPJ", "pessoa jurídica" no texto) | +30 |
| Só CLT | −30 |
| Pleno no título / nível não informado / sênior | +20 / +10 / +5 |
| Remoto / híbrido | +15 / +5 |
| Cada habilidade do seu perfil citada na vaga | +3 (até +15) |
| Sem Java/Spring, júnior, estágio ou trainee | descartada |

4. Gera `relatorios/AAAA-MM-DD.md` com as vagas acima de `notaMinima`, da melhor para a pior, cada uma com os motivos da nota e a mensagem pronta.

## Rodar no seu computador

Precisa de Java 21 e Maven.

```bash
mvn package
java -jar target/buscador-vagas.jar config.json
```

## Rodar todo dia no GitHub (sem deixar o computador ligado)

O workflow `.github/workflows/buscar-vagas.yml` roda de segunda a sexta às 8h (horário de Brasília). Ele salva o relatório no repositório e abre uma **issue** com as vagas do dia, e o GitHub te avisa por e-mail. Para rodar na hora: aba **Actions** → **Buscar vagas** → **Run workflow**.

## Personalizar

Tudo fica no `config.json`:

- `termosBusca`: o que pesquisar.
- `linkedinAtivo`: `false` para usar só a Gupy.
- `notaMinima`: suba para receber menos vagas e mais certeiras.
- `perfil.habilidades`: usadas na nota e na mensagem.
- `modeloMensagem`: o texto da mensagem. Aceita `{empresa}`, `{titulo}`, `{habilidades}`, `{nome}`, `{email}`, `{telefone}`, `{linkedin}` e `{github}`.

Para adicionar outra fonte de vagas, implemente `FonteVagas` e registre em `App`.

## Testes

```bash
mvn test
```
