package br.com.cozinhaemdia;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Banco SQLite descartável: os testes não tocam em data/receitas_despensa.db
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:./target/teste-autenticacao.db")
@AutoConfigureMockMvc
class AutenticacaoTest {
    @Autowired MockMvc mvc;

    @BeforeAll
    static void bancoLimpo() throws Exception {
        Files.deleteIfExists(Path.of("target", "teste-autenticacao.db"));
    }

    @Test
    void paginaProtegidaRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/receitas")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginECadastroSaoPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/cadastro")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void postSemCsrfEhRejeitado() throws Exception {
        mvc.perform(post("/login").param("email", "demo@cozinha.local").param("senha", "demo123")).andExpect(status().isForbidden());
    }

    @Test
    void loginDemoComBcryptFunciona() throws Exception {
        MvcResult r = mvc.perform(post("/login").param("email", "demo@cozinha.local").param("senha", "demo123").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/")).andReturn();
        MockHttpSession sessao = (MockHttpSession) r.getRequest().getSession(false);
        assertNotNull(sessao);
        mvc.perform(get("/").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Conta demonstração")));
    }

    @Test
    void senhaErradaVoltaAoLogin() throws Exception {
        mvc.perform(post("/login").param("email", "demo@cozinha.local").param("senha", "errada").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?erro"));
        mvc.perform(get("/login").param("erro", "")).andExpect(content().string(org.hamcrest.Matchers.containsString("E-mail ou senha inválidos.")));
    }

    @Test
    void cadastroAutenticaOUsuarioELogoutEncerraASessao() throws Exception {
        MvcResult r = mvc.perform(post("/cadastro").param("nome", "Maria").param("email", "maria@teste.com")
                .param("senha", "segredo1").param("confirmarSenha", "segredo1").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/")).andReturn();
        MockHttpSession sessao = (MockHttpSession) r.getRequest().getSession(false);
        mvc.perform(get("/despensa").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(sessao).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?saiu"));
        mvc.perform(get("/despensa").session(sessao)).andExpect(status().is3xxRedirection());
        // e a nova conta consegue entrar pelo login normal
        mvc.perform(post("/login").param("email", "MARIA@teste.com").param("senha", "segredo1").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void telasProtegidasRenderizamParaUsuarioLogado() throws Exception {
        for (String url : new String[]{"/", "/receitas", "/receitas/nova", "/despensa", "/o-que-posso-fazer"})
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void acoesDeEscritaExigemCsrfEFuncionamComToken() throws Exception {
        mvc.perform(post("/despensa").param("nome", "Pimenta").param("quantidade", "10").param("unidade", "g")).andExpect(status().isForbidden());
        mvc.perform(post("/despensa").param("nome", "Pimenta").param("quantidade", "10").param("unidade", "g").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/despensa"));
    }
}
