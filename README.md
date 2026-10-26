# Buscador de Vagas

Bot em Java 21 que procura vagas de desenvolvedor Java, dá uma nota de aderência (PJ, pleno, remoto, stack) e prepara uma mensagem de candidatura personalizada para cada uma. Ele **não se candidata sozinho**: você abre o link, revisa a mensagem e envia. Assim nenhuma conta corre risco de bloqueio por automação.

## Como funciona

1. Busca os termos do `config.json` em seis fontes:

| Fonte | Como lê | O que já vem filtrado |
|---|---|---|
| [Remotar](https://remotar.com.br) | API pública | só remotas; tags PJ, CLT, Pleno, Sênior, híbrida, internacional |
| [backend-br/vagas](https://github.com/backend-br/vagas) | issues do GitHub | rótulos Java, PJ, Remoto, Pleno, Híbrido... |
| [VAGAS.com](https://www.vagas.com.br) | página de busca + página da vaga | "100% Home Office"; modelo de contratação (ex.: Pessoa Jurídica) |
| [InfoJobs](https://www.infojobs.com.br) | página de busca | "Prestador de Serviços (PJ)" e "Home office" |
| [Gupy](https://portal.gupy.io) | página de busca | tipo de contrato e modalidade |
| [LinkedIn](https://www.linkedin.com/jobs) | busca pública, sem login | filtro "Remoto" + descrição da vaga |

   Indeed, Glassdoor, Programathor e Jobbol bloqueiam acesso automatizado. Para eles, o bot lê os **alertas de vaga que chegam no seu Gmail** (veja abaixo).

2. Remove repetidas, vagas já mostradas antes (`data/vistas.json`) e vagas mais antigas que `diasMaximos`.
3. **Descarta** a vaga se:
   - não é de Java (Java no título/tags, Spring na descrição, ou Java citado mais de uma vez);
   - é estágio/trainee, ou sênior e acima (especialista, lead, staff...) sem citar pleno ou júnior;
   - é vaga internacional;
   - com `apenasRemoto`: cita **híbrido ou presencial em qualquer lugar** (título, tags ou descrição), ou não diz que é remota;
   - com `apenasPj`: não diz em lugar nenhum que aceita PJ (tag/filtro da fonte, "PJ", "CNPJ", "pessoa jurídica", "prestador de serviços").
4. Dá uma nota de 0 a 100 para o que sobrou:

| Critério | Pontos |
|---|---:|
| Título de Java/backend | +20 |
| Aceita PJ | +30 |
| Só CLT (quando `apenasPj` está desligado) | −30 |
| Pleno / júnior / nível não informado | +20 / +15 / +10 |
| Remoto | +15 |
| Cada habilidade do seu perfil citada na vaga | +3 (até +15) |

5. Gera `relatorios/AAAA-MM-DD.md` com as vagas acima de `notaMinima`, cada uma com os motivos da nota e a mensagem pronta. No log da execução aparece quantas vagas caíram em cada motivo de descarte.

## Rodar no seu computador

Precisa de Java 21 e Maven.

```bash
mvn package
java -jar target/buscador-vagas.jar config.json
```

## Rodar todo dia no GitHub (sem deixar o computador ligado)

O workflow `.github/workflows/buscar-vagas.yml` roda de segunda a sexta de manhã (com dois horários de reserva, porque o GitHub às vezes pula agendamentos). Ele salva o relatório no repositório e abre uma **issue** com as vagas do dia, e o GitHub te avisa por e-mail. Para rodar na hora: aba **Actions** → **Buscar vagas** → **Run workflow**.

## Alertas por e-mail (Indeed, Glassdoor, Programathor, Jobbol)

O bot não acessa esses sites; ele lê, **só para leitura**, os e-mails de alerta que eles mandam para você, e passa as vagas pelos mesmos filtros.

1. **Crie os alertas** em cada site com o e-mail do Gmail, de preferência com "PJ" e "remoto" na busca (ex.: `java pj`, local `Remoto`). Se o assunto do e-mail tiver "PJ", o bot considera que o site já filtrou por PJ.
2. **Crie uma senha de app do Google**: ative a verificação em duas etapas e gere a senha em <https://myaccount.google.com/apppasswords>. Ela só serve para ler e-mail e pode ser revogada a qualquer momento. Nunca use a senha normal da conta.
3. **Guarde como segredo do repositório** em *Settings → Secrets and variables → Actions → New repository secret*:
   - `EMAIL_USUARIO`: seu endereço do Gmail
   - `EMAIL_SENHA_APP`: a senha de app (16 letras)

Os segredos não aparecem nos logs nem para quem vê o repositório. Sem eles, essa fonte fica desligada. Em `config.json`, `alertasEmail.remetentes` diz de quem são os e-mails lidos e `alertasEmail.dias` quantos dias para trás.

Como cada site monta o e-mail do seu jeito, a leitura pega os links que apontam para vagas e o texto em volta (empresa, local, trecho). Se algum site vier com 0 vagas no log, o formato do e-mail dele precisa de ajuste.

## Personalizar

Tudo fica no `config.json`:

- `termosBusca`: o que pesquisar.
- `apenasRemoto`: `true` para receber só vagas 100% remotas.
- `apenasPj`: `true` para receber só vagas que dizem aceitar PJ.
- `fontesDesligadas`: nomes de fontes para não consultar, ex.: `["LinkedIn", "Gupy"]`.
- `linkedinGeoId`: região da busca no LinkedIn (`106057199` = Brasil).
- `notaMinima`: suba para receber menos vagas e mais certeiras.
- `perfil.habilidades`: usadas na nota e na mensagem.
- `modeloMensagem`: o texto da mensagem. Aceita `{empresa}`, `{titulo}`, `{habilidades}`, `{nome}`, `{email}`, `{telefone}`, `{linkedin}` e `{github}`.

Para adicionar outra fonte de vagas, implemente `FonteVagas` e registre na lista de `App`.

## Testes

```bash
mvn test
```
