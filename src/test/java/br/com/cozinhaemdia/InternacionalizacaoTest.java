package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Troca de idioma (português e inglês), persistência em cookie e ausência de textos sem tradução nas páginas. */
@SpringBootTest
@AutoConfigureMockMvc
class InternacionalizacaoTest {
    static final String EMAIL = "idiomas@teste.local";
    static final List<String> PAGINAS = List.of("/", "/receitas", "/receitas/nova", "/despensa", "/mercado", "/medidas",
            "/amigos", "/compartilhadas", "/o-que-posso-fazer");

    @TempDir static Path pastaUploads;
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    @Autowired UserDetailsService detalhes;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
        registro.add("app.uploads.dir", () -> pastaUploads.toString());
    }

    @BeforeEach
    void preparar() {
        if (usuarios.findByEmailIgnoreCase(EMAIL).isEmpty())
            usuarios.save(new Usuario("Idiomas", EMAIL, codificador.encode("senha-forte-123")));
    }

    private RequestPostProcessor logado() { return user(detalhes.loadUserByUsername(EMAIL)); }
    private static Cookie idioma(String valor) { return new Cookie("idioma", valor); }

    @Test
    void semEscolhaOSistemaFalaPortugues() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ainda não tem conta?")))
                .andExpect(content().string(containsString("lang=\"pt-BR\"")));
    }

    @Test
    void parametroLangTrocaParaInglesEGuardaOCookie() throws Exception {
        mvc.perform(get("/login").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("idioma", "en"))
                .andExpect(content().string(containsString("have an account yet?")))
                .andExpect(content().string(containsString("lang=\"en\"")))
                .andExpect(content().string(not(containsString("Ainda não tem conta"))));
    }

    @Test
    void ocookieMantemOIdiomaNasProximasPaginas() throws Exception {
        mvc.perform(get("/login").cookie(idioma("en")))
                .andExpect(content().string(containsString("Sign up")));
        mvc.perform(get("/login").cookie(idioma("pt-BR")))
                .andExpect(content().string(containsString("Cadastre-se")));
    }

    @Test
    void idiomaInvalidoEIgnorado() throws Exception {
        mvc.perform(get("/login").param("lang", "<script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cadastre-se")));
    }

    @Test
    void seletorDeIdiomaAparecenasPaginasDeAcessoEDentroDoSistema() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(containsString("class=\"idiomas\"")));
        mvc.perform(get("/cadastro")).andExpect(content().string(containsString("class=\"idiomas\"")));
        mvc.perform(get("/despensa").with(logado())).andExpect(content().string(containsString("lang=en")));
    }

    @Test
    void todasAsPaginasTemTraducaoCompletaNosDoisIdiomas() throws Exception {
        for (String idioma : List.of("pt-BR", "en")) {
            for (String pagina : PAGINAS) {
                String html = mvc.perform(get(pagina).with(logado()).cookie(idioma(idioma)))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertFalse(html.contains("??"), pagina + " (" + idioma + ") tem texto sem tradução");
                assertFalse(html.contains("${") || html.contains("#{"), pagina + " (" + idioma + ") deixou uma expressão sem resolver");
            }
        }
    }

    @Test
    void menuPrincipalMudaDeIdioma() throws Exception {
        mvc.perform(get("/despensa").with(logado()).cookie(idioma("en")))
                .andExpect(content().string(containsString("Pantry")))
                .andExpect(content().string(containsString("Shopping list")))
                .andExpect(content().string(not(containsString("Lista de mercado"))));
        mvc.perform(get("/despensa").with(logado()).cookie(idioma("pt-BR")))
                .andExpect(content().string(containsString("Lista de mercado")));
    }

    @Test
    void mensagemDeSucessoEDeErroSaemNoIdiomaEscolhido() throws Exception {
        mvc.perform(post("/despensa").param("nome", "arroz").param("quantidade", "1").param("unidade", "kg")
                        .with(csrf()).with(logado()).cookie(idioma("en")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("sucesso", "Ingredient added to the pantry."));
        mvc.perform(post("/despensa").param("nome", "arroz").param("quantidade", "1").param("unidade", "kg")
                        .with(csrf()).with(logado()).cookie(idioma("pt-BR")))
                .andExpect(flash().attribute("sucesso", "Ingrediente adicionado à despensa."));
        mvc.perform(post("/despensa").param("nome", "  ").param("quantidade", "1").param("unidade", "kg")
                        .with(csrf()).with(logado()).cookie(idioma("en")))
                .andExpect(flash().attribute("erro", "Enter the ingredient name."));
    }

    @Test
    void paginaDeReceitaEUnidadesSaemTraduzidas() throws Exception {
        MvcResult criada = mvc.perform(post("/receitas")
                        .param("nome", "Receita de idiomas").param("descricao", "Teste").param("tempoPreparoMin", "20").param("porcoes", "2")
                        .param("ingNome", "ovo").param("ingQtd", "2").param("ingUn", "unidade")
                        .param("ingNome", "farinha de trigo").param("ingQtd", "1").param("ingUn", "xícara")
                        .param("etapaChave", "1").param("etapaTexto_1", "Misture tudo.").param("etapaFotoAtual_1", "")
                        .with(csrf()).with(logado()))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = criada.getResponse().getRedirectedUrl();
        assertNotNull(destino);

        String ingles = mvc.perform(get(destino).with(logado()).cookie(idioma("en"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(ingles.contains("Comments"));
        assertTrue(ingles.contains("Method"));
        assertTrue(ingles.contains("cup"), "a unidade xícara deve aparecer como cup");
        assertTrue(ingles.contains("20 min"));
        assertFalse(ingles.contains("??"));

        String portugues = mvc.perform(get(destino).with(logado()).cookie(idioma("pt-BR"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(portugues.contains("Comentários"));
        assertTrue(portugues.contains("Modo de preparo"));
        assertTrue(portugues.contains("xícara"));
        assertEquals(-1, portugues.indexOf("??"));
    }

    @Test
    void erroDeNegocioEmInglesTambemEhTraduzido() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "Fulano").param("email", "invalido").param("senha", "abcdef").param("confirmarSenha", "abcdef")
                        .with(csrf()).cookie(idioma("en")))
                .andExpect(content().string(containsString("valid e-mail")));
    }
}
