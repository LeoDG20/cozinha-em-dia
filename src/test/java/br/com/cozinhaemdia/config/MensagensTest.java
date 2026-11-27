package br.com.cozinhaemdia.config;

import br.com.cozinhaemdia.service.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os arquivos de mensagens em português e em inglês e o tradutor de textos e exceções. */
class MensagensTest {
    private static final Locale INGLES = Locale.ENGLISH;
    private static final Pattern PARAMETRO = Pattern.compile("\\{(\\d+)");

    private static Properties carregar(String nome) throws Exception {
        Properties p = new Properties();
        try (InputStream in = MensagensTest.class.getResourceAsStream("/" + nome);
             InputStreamReader leitor = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(leitor);
        }
        return p;
    }

    private static Mensagens mensagens() {
        ResourceBundleMessageSource origem = new ResourceBundleMessageSource();
        origem.setBasename("messages");
        origem.setDefaultEncoding("UTF-8");
        origem.setAlwaysUseMessageFormat(true);
        origem.setFallbackToSystemLocale(false);
        return new Mensagens(origem);
    }

    @Test
    void osDoisIdiomasTemExatamenteAsMesmasChaves() throws Exception {
        TreeSet<String> pt = new TreeSet<>(carregar("messages.properties").stringPropertyNames());
        TreeSet<String> en = new TreeSet<>(carregar("messages_en.properties").stringPropertyNames());
        TreeSet<String> soPt = new TreeSet<>(pt); soPt.removeAll(en);
        TreeSet<String> soEn = new TreeSet<>(en); soEn.removeAll(pt);
        assertTrue(soPt.isEmpty(), "chaves sem tradução para o inglês: " + soPt);
        assertTrue(soEn.isEmpty(), "chaves em inglês sem texto em português: " + soEn);
    }

    @Test
    void cadaTraducaoUsaOsMesmosParametrosDoOriginal() throws Exception {
        Properties pt = carregar("messages.properties"), en = carregar("messages_en.properties");
        for (String chave : pt.stringPropertyNames()) {
            assertEquals(parametros(pt.getProperty(chave)), parametros(en.getProperty(chave)), "parâmetros diferentes em " + chave);
            assertTrue(!pt.getProperty(chave).isBlank() && !en.getProperty(chave).isBlank(), "texto vazio em " + chave);
        }
    }

    @Test
    void todosOsTextosPassamPeloMessageFormatSemErro() throws Exception {
        for (String arquivo : new String[]{"messages.properties", "messages_en.properties"}) {
            Properties p = carregar(arquivo);
            for (String chave : p.stringPropertyNames())
                new MessageFormat(p.getProperty(chave)).format(new Object[]{2, 3, 2, 3});
        }
    }

    private static TreeSet<String> parametros(String texto) {
        TreeSet<String> usados = new TreeSet<>();
        Matcher m = PARAMETRO.matcher(texto);
        while (m.find()) usados.add(m.group(1));
        return usados;
    }

    @Test
    void mensagemComArgumentoEPluralNosDoisIdiomas() {
        Mensagens m = mensagens();
        try {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
            assertEquals("1 item foi somado à despensa.", m.get("msg.mercado.somado", 1));
            assertEquals("3 itens foram somados à despensa.", m.get("msg.mercado.somado", 3));
            LocaleContextHolder.setLocale(INGLES);
            assertEquals("1 item was added to the pantry.", m.get("msg.mercado.somado", 1));
            assertEquals("3 items were added to the pantry.", m.get("msg.mercado.somado", 3));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void unidadesGuardadasEmPortuguesSaemNoIdiomaDaPagina() {
        Mensagens m = mensagens();
        try {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
            assertEquals("xícara", m.unidade("xícara"));
            assertEquals("colher de chá", m.unidade("colher de chá"));
            LocaleContextHolder.setLocale(INGLES);
            assertEquals("cup", m.unidade("xícara"));
            assertEquals("teaspoon", m.unidade("colher de chá"));
            assertEquals("g", m.unidade("g"));
            assertEquals("pitada", m.unidade("pitada"), "unidade sem tradução volta como foi escrita");
            assertEquals("", m.unidade(null));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void excecaoDeNegocioEhTraduzidaEAsDemaisMantemOTexto() {
        Mensagens m = mensagens();
        NegocioException ex = new NegocioException("erro.quantidade.positiva", "A quantidade deve ser maior que zero.");
        try {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("pt-BR"));
            assertEquals("A quantidade deve ser maior que zero.", m.de(ex));
            LocaleContextHolder.setLocale(INGLES);
            assertEquals("The amount must be greater than zero.", m.de(ex));
            assertEquals("texto avulso", m.de(new IllegalArgumentException("texto avulso")));
            assertEquals("Texto padrão", m.de(new NegocioException("chave.inexistente", "Texto padrão")));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
