package io.github.mathmartins0.vagas.fontes;

import io.github.mathmartins0.vagas.Vaga;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Lê, só para leitura, os e-mails de alerta de vagas que chegaram no Gmail nos últimos dias.
 * Serve para sites que bloqueiam acesso automatizado (Indeed, Glassdoor, Programathor, Jobbol):
 * a pessoa cria o alerta no site e o bot lê o que chegou por e-mail.
 * Usuário e senha de app vêm das variáveis EMAIL_USUARIO e EMAIL_SENHA_APP.
 */
public class EmailAlertasFonte implements FonteVagas {

    private static final String SERVIDOR = "imap.gmail.com";

    private final String usuario;
    private final String senha;
    private final List<String> remetentes;
    private final int dias;
    private final LeitorAlertaEmail leitor = new LeitorAlertaEmail();

    public EmailAlertasFonte(String usuario, String senha, List<String> remetentes, int dias) {
        this.usuario = usuario;
        this.senha = senha;
        this.remetentes = remetentes;
        this.dias = dias;
    }

    /** Cria a fonte se as variáveis de ambiente estiverem definidas; senão devolve null. */
    public static EmailAlertasFonte doAmbiente(List<String> remetentes, int dias) {
        String usuario = System.getenv("EMAIL_USUARIO");
        String senha = System.getenv("EMAIL_SENHA_APP");
        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            return null;
        }
        return new EmailAlertasFonte(usuario.trim(), senha.replace(" ", ""), remetentes, dias);
    }

    @Override
    public String nome() {
        return "Alertas por e-mail";
    }

    @Override
    public boolean buscaPorTermo() {
        return false;
    }

    @Override
    public List<Vaga> buscar(String termo) throws IOException {
        Properties propriedades = new Properties();
        propriedades.put("mail.store.protocol", "imaps");
        propriedades.put("mail.imaps.connectiontimeout", "20000");
        propriedades.put("mail.imaps.timeout", "30000");
        List<Vaga> vagas = new ArrayList<>();
        try (Store store = Session.getInstance(propriedades).getStore("imaps")) {
            store.connect(SERVIDOR, usuario, senha);
            Folder caixa = store.getFolder("INBOX");
            caixa.open(Folder.READ_ONLY);
            try {
                Date desde = Date.from(LocalDate.now().minusDays(dias).atStartOfDay(ZoneId.systemDefault()).toInstant());
                for (Message mensagem : caixa.search(new ReceivedDateTerm(ComparisonTerm.GE, desde))) {
                    String remetente = remetente(mensagem);
                    if (remetentes.stream().noneMatch(r -> remetente.contains(r.toLowerCase(Locale.ROOT)))) {
                        continue;
                    }
                    String html = html(mensagem);
                    if (html == null) {
                        continue;
                    }
                    LocalDate recebido = mensagem.getReceivedDate() == null ? LocalDate.now()
                            : mensagem.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    List<Vaga> doEmail = leitor.ler(remetente, mensagem.getSubject(), html, recebido);
                    System.out.printf("  e-mail de %s: %d vagas%n", LeitorAlertaEmail.site(remetente), doEmail.size());
                    vagas.addAll(doEmail);
                }
            } finally {
                caixa.close(false);
            }
        } catch (MessagingException e) {
            throw new IOException("não consegui ler o Gmail: " + e.getMessage(), e);
        }
        return vagas;
    }

    private static String remetente(Message mensagem) throws MessagingException {
        Address[] de = mensagem.getFrom();
        return de == null || de.length == 0 ? "" : de[0].toString().toLowerCase(Locale.ROOT);
    }

    /** Primeira parte text/html da mensagem, procurando dentro de multipart. */
    private static String html(Part parte) throws MessagingException, IOException {
        if (parte.isMimeType("text/html")) {
            return (String) parte.getContent();
        }
        if (parte.isMimeType("multipart/*")) {
            Multipart partes = (Multipart) parte.getContent();
            for (int i = 0; i < partes.getCount(); i++) {
                BodyPart filha = partes.getBodyPart(i);
                String html = html(filha);
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }
}
