package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Amizade;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.AmizadeRepository;
import br.com.cozinhaemdia.repository.DespensaRepository;
import br.com.cozinhaemdia.repository.IngredienteRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
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

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Fluxo de amigos, lista de mercado, conversor de medidas e telas de acesso. */
@SpringBootTest
@AutoConfigureMockMvc
class AmigosMercadoEMedidasTest {
    static final String ANA = "ana.amigos@teste.local", BRUNO = "bruno.amigos@teste.local", CARLA = "carla.amigos@teste.local",
            COZINHEIRA = "cozinheira.mercado@teste.local", COMPRADORA = "compradora.mercado@teste.local";

    @TempDir static Path pastaUploads;
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AmizadeRepository amizades;
    @Autowired DespensaRepository despensa;
    @Autowired IngredienteRepository ingredientes;
    @Autowired PasswordEncoder codificador;
    @Autowired UserDetailsService detalhes;
    @Autowired PlatformTransactionManager transacoes;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
        registro.add("app.uploads.dir", () -> pastaUploads.toString());
    }

    @BeforeEach
    void preparar() {
        for (String email : List.of(ANA, BRUNO, CARLA, COZINHEIRA, COMPRADORA))
            if (usuarios.findByEmailIgnoreCase(email).isEmpty())
                usuarios.save(new Usuario(email.split("\\.")[0], email, codificador.encode("senha-forte-123")));
        // Cada teste começa sem amizades entre os usuários de teste.
        new TransactionTemplate(transacoes).executeWithoutResult(st -> {
            for (Amizade a : amizades.findAll()) {
                String de = a.getSolicitante().getEmail(), para = a.getDestinatario().getEmail();
                if (List.of(ANA, BRUNO, CARLA).contains(de) && List.of(ANA, BRUNO, CARLA).contains(para)) amizades.delete(a);
            }
        });
    }

    private RequestPostProcessor como(String email) { return user(detalhes.loadUserByUsername(email)); }
    private Usuario usuario(String email) { return usuarios.findByEmailIgnoreCase(email).orElseThrow(); }

    private Long idDoPedido(String solicitante, String destinatario) {
        return amizades.findBySolicitanteIdAndDestinatarioId(usuario(solicitante).getId(), usuario(destinatario).getId()).orElseThrow().getId();
    }

    private Long criarReceita(String email, String nome) throws Exception {
        MvcResult r = mvc.perform(post("/receitas")
                        .param("nome", nome).param("descricao", "Teste").param("tempoPreparoMin", "20").param("porcoes", "2")
                        .param("ingNome", "ovo").param("ingQtd", "2").param("ingUn", "unidade")
                        .param("ingNome", "farinha de trigo").param("ingQtd", "2").param("ingUn", "xícara")
                        .param("etapaChave", "1").param("etapaTexto_1", "Misture tudo.").param("etapaFotoAtual_1", "")
                        .with(csrf()).with(como(email)))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = r.getResponse().getRedirectedUrl();
        assertNotNull(destino);
        return Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
    }

    // ---------- Amigos ----------

    @Test
    void pedidoDeAmizadeSoViraAmizadeQuandoODestinatarioAceita() throws Exception {
        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/amigos")).andExpect(flash().attributeExists("sucesso"));
        assertFalse(amizades.saoAmigos(usuario(ANA).getId(), usuario(BRUNO).getId()));

        mvc.perform(get("/amigos").with(como(BRUNO))).andExpect(status().isOk()).andExpect(view().name("amigos"))
                .andExpect(model().attribute("recebidos", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(content().string(containsString("Pedidos recebidos")));
        mvc.perform(get("/amigos").with(como(ANA)))
                .andExpect(model().attribute("enviados", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("amigos", org.hamcrest.Matchers.hasSize(0)));

        mvc.perform(post("/amigos/" + idDoPedido(ANA, BRUNO) + "/aceitar").with(csrf()).with(como(BRUNO)))
                .andExpect(redirectedUrl("/amigos")).andExpect(flash().attributeExists("sucesso"));
        assertTrue(amizades.saoAmigos(usuario(ANA).getId(), usuario(BRUNO).getId()));
        mvc.perform(get("/amigos").with(como(ANA))).andExpect(model().attribute("amigos", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void quemEnviouOPedidoNaoPodeAceitarPorConta() throws Exception {
        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)));
        Long id = idDoPedido(ANA, BRUNO);

        mvc.perform(post("/amigos/" + id + "/aceitar").with(csrf()).with(como(ANA))).andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/amigos/" + id + "/aceitar").with(csrf()).with(como(CARLA))).andExpect(flash().attributeExists("erro"));
        assertFalse(amizades.saoAmigos(usuario(ANA).getId(), usuario(BRUNO).getId()));
    }

    @Test
    void recusarCancelarEDesfazerRemovemARelacao() throws Exception {
        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)));
        mvc.perform(post("/amigos/" + idDoPedido(ANA, BRUNO) + "/recusar").with(csrf()).with(como(BRUNO)))
                .andExpect(flash().attributeExists("sucesso"));
        assertTrue(amizades.findBySolicitanteIdAndDestinatarioId(usuario(ANA).getId(), usuario(BRUNO).getId()).isEmpty());

        mvc.perform(post("/amigos").param("email", CARLA).with(csrf()).with(como(ANA)));
        mvc.perform(post("/amigos/" + idDoPedido(ANA, CARLA) + "/recusar").with(csrf()).with(como(ANA)))
                .andExpect(flash().attributeExists("sucesso"));

        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)));
        mvc.perform(post("/amigos/" + idDoPedido(ANA, BRUNO) + "/aceitar").with(csrf()).with(como(BRUNO)));
        mvc.perform(post("/amigos/" + idDoPedido(ANA, BRUNO) + "/desfazer").with(csrf()).with(como(BRUNO)))
                .andExpect(flash().attributeExists("sucesso"));
        assertFalse(amizades.saoAmigos(usuario(ANA).getId(), usuario(BRUNO).getId()));
    }

    @Test
    void enviarPedidoParaEmailInexistenteOuParaSiMesmoMostraErro() throws Exception {
        mvc.perform(post("/amigos").param("email", "ninguem@teste.local").with(csrf()).with(como(ANA)))
                .andExpect(flash().attribute("erro", containsString("Não há usuário")));
        mvc.perform(post("/amigos").param("email", ANA).with(csrf()).with(como(ANA)))
                .andExpect(flash().attribute("erro", containsString("a si mesmo")));
    }

    @Test
    void pedidoDeVoltaConfirmaAAmizadeNaHora() throws Exception {
        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)));
        mvc.perform(post("/amigos").param("email", ANA).with(csrf()).with(como(BRUNO)))
                .andExpect(flash().attribute("sucesso", containsString("agora são amigos")));
        assertTrue(amizades.saoAmigos(usuario(ANA).getId(), usuario(BRUNO).getId()));
    }

    @Test
    void receitaSoPodeSerCompartilhadaComAmigos() throws Exception {
        Long receita = criarReceita(ANA, "Bolo só para amigos");

        mvc.perform(post("/receitas/" + receita + "/compartilhar").param("amigo", String.valueOf(usuario(BRUNO).getId())).with(csrf()).with(como(ANA)))
                .andExpect(flash().attribute("erro", containsString("ainda não é seu amigo")));
        mvc.perform(get("/compartilhadas").with(como(BRUNO))).andExpect(content().string(not(containsString("Bolo só para amigos"))));

        mvc.perform(post("/amigos").param("email", BRUNO).with(csrf()).with(como(ANA)));
        mvc.perform(post("/amigos/" + idDoPedido(ANA, BRUNO) + "/aceitar").with(csrf()).with(como(BRUNO)));

        mvc.perform(get("/receitas/" + receita).with(como(ANA))).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"amigo\"")));
        mvc.perform(post("/receitas/" + receita + "/compartilhar").param("amigo", String.valueOf(usuario(BRUNO).getId())).with(csrf()).with(como(ANA)))
                .andExpect(flash().attribute("sucesso", containsString("Receita compartilhada")));
        mvc.perform(get("/compartilhadas").with(como(BRUNO))).andExpect(content().string(containsString("Bolo só para amigos")));
    }

    @Test
    void detalheSemAmigosOrientaAAdicionarAmigos() throws Exception {
        Long receita = criarReceita(ANA, "Receita sem amigos");
        mvc.perform(get("/receitas/" + receita).with(como(ANA))).andExpect(status().isOk())
                .andExpect(model().attribute("totalAmigos", 0))
                .andExpect(content().string(containsString("Adicionar amigos")));
    }

    // ---------- Lista de mercado ----------

    @Test
    void mercadoSomaOQueFaltaDasReceitasEscolhidas() throws Exception {
        Long receita = criarReceita(COZINHEIRA, "Panqueca do mercado");

        MvcResult r = mvc.perform(get("/mercado").param("f", "1").param("receitas", String.valueOf(receita)).with(como(COZINHEIRA)))
                .andExpect(status().isOk()).andExpect(view().name("mercado"))
                .andExpect(model().attributeExists("analises", "escolhidas", "compras")).andReturn();
        List<?> compras = (List<?>) r.getModelAndView().getModel().get("compras");
        assertEquals(2, compras.size(), "ovo e farinha de trigo devem aparecer na lista");

        Map<String, Object> vazio = mvc.perform(get("/mercado").param("f", "1").with(como(COZINHEIRA)))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertTrue(((List<?>) vazio.get("compras")).isEmpty(), "sem receitas escolhidas a lista fica vazia");
    }

    @Test
    void comprarSomaOsItensMarcadosNaDespensa() throws Exception {
        criarReceita(COMPRADORA, "Receita para comprar");

        mvc.perform(post("/mercado/comprar").param("item", "12|unidade|Ovo").param("item", "480|g|Farinha de trigo").with(csrf()).with(como(COMPRADORA)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("sucesso", containsString("2 itens")));

        Long usuarioId = usuario(COMPRADORA).getId();
        Long ovo = ingredientes.findByNomeIgnoreCase("Ovo").orElseThrow().getId();
        assertEquals(12.0, despensa.findByUsuarioIdAndIngredienteId(usuarioId, ovo).orElseThrow().getQuantidade(), 0.0001);
    }

    @Test
    void comprarSemMarcarNenhumItemMostraErro() throws Exception {
        mvc.perform(post("/mercado/comprar").with(csrf()).with(como(COZINHEIRA)))
                .andExpect(redirectedUrl("/mercado?f=1"))
                .andExpect(flash().attributeExists("erro"));
    }

    // ---------- Medidas ----------

    @Test
    void paginaDeMedidasConverteComIngrediente() throws Exception {
        mvc.perform(get("/medidas").with(como(ANA))).andExpect(status().isOk()).andExpect(view().name("medidas"))
                .andExpect(model().attributeExists("unidades", "referencia"))
                .andExpect(model().attribute("resultado", org.hamcrest.Matchers.nullValue()));

        mvc.perform(get("/medidas").param("valor", "2").param("de", "xícara").param("para", "g").param("ingrediente", "Farinha de trigo").with(como(ANA)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 xícara de Farinha de trigo = 240 g")));
    }

    @Test
    void paginasDeMedidasEMercadoExigemLogin() throws Exception {
        mvc.perform(get("/medidas")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/mercado")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/amigos")).andExpect(status().is3xxRedirection());
    }

    // ---------- Telas de acesso ----------

    @Test
    void loginECadastroNaoUsamFotoDePrato() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("acesso-capa"))))
                .andExpect(content().string(not(containsString("upload.wikimedia.org"))))
                .andExpect(content().string(containsString("demo@cozinha.local")));
        mvc.perform(get("/cadastro")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("acesso-capa"))))
                .andExpect(content().string(not(containsString("upload.wikimedia.org"))));
    }
}
