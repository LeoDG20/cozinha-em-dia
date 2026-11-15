package br.com.cozinhaemdia;

import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Validações do formulário de cadastro feitas no servidor. */
@SpringBootTest
@AutoConfigureMockMvc
class CadastroValidacaoTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
    }

    @Test
    void emailSemFormatoValidoNaoCriaConta() throws Exception {
        for (String email : new String[]{"sem-arroba.com", "joao@", "joao@dominio", "joao silva@teste.com"}) {
            cadastrar("João", email, "segredo1", "segredo1")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe um e-mail válido")));
            assertFalse(usuarios.existsByEmailIgnoreCase(email), email);
        }
    }

    @Test
    void emailMaiorQueAColunaNaoCriaConta() throws Exception {
        String email = "a".repeat(150) + "@teste.com.br";
        cadastrar("Ana", email, "segredo1", "segredo1")
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Informe um e-mail válido")));
        assertFalse(usuarios.existsByEmailIgnoreCase(email));
    }

    @Test
    void nomeMaiorQueCemCaracteresNaoCriaConta() throws Exception {
        cadastrar("N".repeat(101), "nome.longo@teste.com", "segredo1", "segredo1")
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("no máximo 100 caracteres")));
        assertFalse(usuarios.existsByEmailIgnoreCase("nome.longo@teste.com"));
    }

    @Test
    void senhasDiferentesNaoCriamConta() throws Exception {
        cadastrar("Bia", "bia@teste.com", "segredo1", "segredo2")
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("As senhas não coincidem.")));
        assertFalse(usuarios.existsByEmailIgnoreCase("bia@teste.com"));
    }

    @Test
    void emailJaCadastradoNaoCriaOutraConta() throws Exception {
        cadastrar("Outra demo", "DEMO@cozinha.local", "segredo1", "segredo1")
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Já existe uma conta com esse e-mail.")));
    }

    @Test
    void dadosValidosCriamAContaComEmailNormalizado() throws Exception {
        cadastrar("  Carlos  ", "  Carlos@Teste.com ", "segredo1", "segredo1")
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
        assertTrue(usuarios.findByEmailIgnoreCase("carlos@teste.com").map(u -> u.getEmail().equals("carlos@teste.com")).orElse(false));
    }

    private ResultActions cadastrar(String nome, String email, String senha, String confirmarSenha) throws Exception {
        return mvc.perform(post("/cadastro").param("nome", nome).param("email", email)
            .param("senha", senha).param("confirmarSenha", confirmarSenha).with(csrf()));
    }
}
