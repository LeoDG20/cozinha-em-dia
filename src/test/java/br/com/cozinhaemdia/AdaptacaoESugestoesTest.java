package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Versões adaptadas, exibição de com quem a receita foi compartilhada, sugestões de digitação e lista de compras. */
@SpringBootTest
@AutoConfigureMockMvc
class AdaptacaoESugestoesTest {
    static final String DONO = "demo@cozinha.local", AMIGO = "amigo.adapta@teste.local", ESTRANHO = "estranho.adapta@teste.local";

    @TempDir static Path pastaUploads;
    @Autowired MockMvc mvc;
    @Autowired ReceitaRepository receitas;
    @Autowired ReceitaIngredienteRepository receitaIngredientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    @Autowired UserDetailsService detalhes;
    @Autowired PlatformTransactionManager transacoes;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
        registro.add("app.uploads.dir", () -> pastaUploads.toString());
    }

    @BeforeEach
    void criarUsuarios() {
        for (String email : List.of(AMIGO, ESTRANHO))
            if (usuarios.findByEmailIgnoreCase(email).isEmpty()) usuarios.save(new Usuario(email.split("\\.")[0], email, codificador.encode("senha-forte-123")));
    }

    private RequestPostProcessor como(String email) { return user(detalhes.loadUserByUsername(email)); }
    private Usuario usuario(String email) { return usuarios.findByEmailIgnoreCase(email).orElseThrow(); }

    private Long criar(String email, String nome) throws Exception {
        MvcResult r = mvc.perform(post("/receitas")
                        .param("nome", nome).param("descricao", "Descrição de teste").param("tempoPreparoMin", "25").param("porcoes", "4")
                        .param("ingNome", "ovo").param("ingQtd", "2").param("ingUn", "unidade")
                        .param("ingNome", "farinha de trigo").param("ingQtd", "200").param("ingUn", "g")
                        .param("etapaChave", "1").param("etapaChave", "2")
                        .param("etapaTexto_1", "Misture tudo.").param("etapaFotoAtual_1", "")
                        .param("etapaTexto_2", "Leve ao forno.").param("etapaFotoAtual_2", "")
                        .with(csrf()).with(como(email)))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = r.getResponse().getRedirectedUrl();
        assertNotNull(destino);
        return Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
    }

    private Receita carregar(Long id) {
        return new TransactionTemplate(transacoes).execute(st -> { Receita r = receitas.findById(id).orElseThrow(); r.getEtapas().size(); return r; });
    }

    @Test
    void adaptarCriaVersaoCompletaComNovoNomeEAbreAEdicao() throws Exception {
        Long id = criar(DONO, "Panqueca para adaptar");

        MvcResult r = mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "Panqueca sem lactose").with(csrf()).with(como(DONO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/receitas/*/editar"))
                .andExpect(flash().attributeExists("sucesso")).andReturn();

        String url = r.getResponse().getRedirectedUrl();
        Long idVersao = Long.valueOf(url.replace("/receitas/", "").replace("/editar", ""));
        Receita versao = carregar(idVersao);
        assertEquals("Panqueca sem lactose", versao.getNome());
        assertEquals("Panqueca para adaptar", versao.getOrigemNome());
        assertEquals(2, versao.getEtapas().size());
        assertEquals(2, receitaIngredientes.findAllByReceitaIdOrderByIdAsc(idVersao).size());
        assertEquals(usuario(DONO).getId(), versao.getUsuario().getId());
        assertEquals("Panqueca para adaptar", carregar(id).getNome());
        assertNull(carregar(id).getOrigemNome());

        mvc.perform(get("/receitas/" + idVersao + "/editar").with(como(DONO))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Versão adaptada de")));
        mvc.perform(get("/receitas/" + idVersao).with(como(DONO))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Panqueca sem lactose")));
    }

    @Test
    void adaptarRecusaNomeRepetidoEVazioMantendoAPaginaDaReceita() throws Exception {
        Long id = criar(DONO, "Torta que não repete");
        long antes = receitas.countByUsuarioId(usuario(DONO).getId());

        mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "torta que não repete").with(csrf()).with(como(DONO)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("erro"))
                .andExpect(redirectedUrl("/receitas/" + id));
        mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "   ").with(csrf()).with(como(DONO)))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/receitas/" + id + "/adaptar").with(csrf()).with(como(DONO)))
                .andExpect(flash().attributeExists("erro"));

        assertEquals(antes, receitas.countByUsuarioId(usuario(DONO).getId()));
    }

    @Test
    void quemRecebeuPodeAdaptarMasEstranhoENaoLogadoNao() throws Exception {
        Long id = criar(DONO, "Receita adaptável por amigo");
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).with(csrf()).with(como(DONO)));

        mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "Minha versão").with(csrf()).with(como(ESTRANHO)))
                .andExpect(flash().attribute("erro", "Receita não encontrada."));
        mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "Minha versão"))
                .andExpect(status().isForbidden());

        MvcResult r = mvc.perform(post("/receitas/" + id + "/adaptar").param("nome", "Versão do amigo").with(csrf()).with(como(AMIGO)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("/receitas/*/editar")).andReturn();
        Long idVersao = Long.valueOf(r.getResponse().getRedirectedUrl().replace("/receitas/", "").replace("/editar", ""));
        assertEquals(usuario(AMIGO).getId(), carregar(idVersao).getUsuario().getId());
        assertEquals("Receita adaptável por amigo", carregar(idVersao).getOrigemNome());
    }

    @Test
    void listaEDetalheMostramComQuemAReceitaFoiCompartilhada() throws Exception {
        Long id = criar(DONO, "Receita com destinatários");
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).with(csrf()).with(como(DONO)));

        @SuppressWarnings("unchecked")
        Map<Long, List<String>> mapa = (Map<Long, List<String>>) mvc.perform(get("/receitas").with(como(DONO)))
                .andExpect(status().isOk()).andExpect(view().name("receitas")).andReturn().getModelAndView().getModel().get("compartilhadoCom");
        assertTrue(mapa.get(id).contains(usuario(AMIGO).getNome()));

        mvc.perform(get("/receitas/" + id).with(como(DONO))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Compartilhada com")))
                .andExpect(content().string(containsString(usuario(AMIGO).getEmail())));

        // Quem recebeu não vê a lista de destinatários nem o painel de compartilhar.
        mvc.perform(get("/receitas/" + id).with(como(AMIGO))).andExpect(status().isOk())
                .andExpect(model().attribute("dono", false))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Gerenciar acesso"))));
    }

    @Test
    void donoRevogaPelaPaginaDaReceitaEVoltaParaEla() throws Exception {
        Long id = criar(DONO, "Receita revogada na página");
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).with(csrf()).with(como(DONO)));
        Long compartilhamento = ((List<?>) mvc.perform(get("/compartilhadas").with(como(AMIGO))).andReturn().getModelAndView().getModel().get("recebidas"))
                .stream().map(o -> ((br.com.cozinhaemdia.model.Compartilhamento) o).getId()).max(Long::compare).orElseThrow();

        mvc.perform(post("/compartilhamentos/" + compartilhamento + "/remover").param("receita", String.valueOf(id)).with(csrf()).with(como(DONO)))
                .andExpect(redirectedUrl("/receitas/" + id));
        mvc.perform(get("/receitas/" + id).with(como(AMIGO))).andExpect(flash().attribute("erro", "Receita não encontrada."));
    }

    @Test
    void apiVerificaNomeConhecidoEOferecesugestoesParaErroDeDigitacao() throws Exception {
        mvc.perform(get("/api/ingredientes/verificar").param("nome", "Farinha de trigo").with(como(DONO)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conhecido").value(true));

        mvc.perform(get("/api/ingredientes/verificar").param("nome", "xocolate").with(como(DONO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conhecido").value(false))
                .andExpect(jsonPath("$.sugestoes[0]").value(containsString("hocolate")));

        mvc.perform(get("/api/ingredientes/verificar").param("nome", "xyzqwk").with(como(DONO)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sugestoes").isEmpty());

        mvc.perform(get("/api/ingredientes/verificar").param("nome", "farinha")).andExpect(status().is3xxRedirection());
    }

    @Test
    void buscaSemResultadoOferecesugestoesDeBusca() throws Exception {
        Map<String, Object> m = mvc.perform(get("/receitas").param("q", "lazanha").with(como(DONO)))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertNotNull(m.get("sugestoes"));
    }

    @Test
    void despensaMostraUsoPorIngredienteEListaDeCompras() throws Exception {
        mvc.perform(get("/despensa").with(como(DONO))).andExpect(status().isOk()).andExpect(view().name("despensa"))
                .andExpect(model().attributeExists("itens", "unidades", "sugestoes", "uso", "compras"))
                .andExpect(content().string(containsString("Lista de compras")));
    }

    @Test
    void paginaDeErroAmigavelParaEnderecoInexistente() throws Exception {
        mvc.perform(get("/receitas/99999999").with(como(DONO))).andExpect(status().is3xxRedirection());
        mvc.perform(get("/rota-que-nao-existe").with(como(DONO))).andExpect(status().isNotFound());
    }
}
