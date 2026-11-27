package br.com.cozinhaemdia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Páginas de acesso em um servidor real, sem sessão prévia. O MockMvc não envia a resposta aos poucos, então só aqui aparece
 * o erro em que a tela de login era cortada antes do formulário quando o token CSRF ainda precisava criar a sessão.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrimeiraVisitaTest {
    @LocalServerPort int porta;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception { BancoPostgresDeTeste.configurar(registro); }

    private String baixar(String caminho) throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).build(), HttpResponse.BodyHandlers.ofString());
        return r.body();
    }

    @Test
    void loginECadastroChegamCompletosComFormularioETokenCsrf() throws Exception {
        for (String pagina : new String[]{"/login", "/login?lang=en", "/cadastro", "/cadastro?lang=en"}) {
            String html = baixar(pagina);
            assertTrue(html.contains("</html>"), pagina + " foi cortada");
            assertTrue(html.contains("<form") && html.contains("name=\"_csrf\""), pagina + " ficou sem formulário ou sem token CSRF");
        }
    }
}
