package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Comentario;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.AmizadeRepository;
import br.com.cozinhaemdia.repository.ComentarioRepository;
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

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comentários em receitas: quem pode comentar, ler e excluir. */
@SpringBootTest
@AutoConfigureMockMvc
class ComentariosTest {
    static final String DONA = "dona.comentarios@teste.local", AMIGA = "amiga.comentarios@teste.local", ESTRANHO = "estranho.comentarios@teste.local";

    @TempDir static Path pastaUploads;
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AmizadeRepository amizades;
    @Autowired ComentarioRepository comentarios;
    @Autowired PasswordEncoder codificador;
    @Autowired UserDetailsService detalhes;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
        registro.add("app.uploads.dir", () -> pastaUploads.toString());
    }

    @BeforeEach
    void preparar() {
        for (String email : List.of(DONA, AMIGA, ESTRANHO))
            if (usuarios.findByEmailIgnoreCase(email).isEmpty())
                usuarios.save(new Usuario(email.split("\\.")[0], email, codificador.encode("senha-forte-123")));
    }

    private RequestPostProcessor como(String email) { return user(detalhes.loadUserByUsername(email)); }
    private Usuario usuario(String email) { return usuarios.findByEmailIgnoreCase(email).orElseThrow(); }

    private Long criarReceita(String nome) throws Exception {
        MvcResult r = mvc.perform(post("/receitas")
                        .param("nome", nome).param("descricao", "Teste").param("tempoPreparoMin", "20").param("porcoes", "2")
                        .param("ingNome", "ovo").param("ingQtd", "2").param("ingUn", "unidade")
                        .param("etapaChave", "1").param("etapaTexto_1", "Misture tudo.").param("etapaFotoAtual_1", "")
                        .with(csrf()).with(como(DONA)))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = r.getResponse().getRedirectedUrl();
        assertNotNull(destino);
        return Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
    }

    /** Torna DONA e AMIGA amigas (se ainda não forem) e compartilha a receita com a amiga. */
    private void compartilharComAmiga(Long receitaId) throws Exception {
        if (!amizades.saoAmigos(usuario(DONA).getId(), usuario(AMIGA).getId())) {
            mvc.perform(post("/amigos").param("email", AMIGA).with(csrf()).with(como(DONA)));
            Long pedido = amizades.findBySolicitanteIdAndDestinatarioId(usuario(DONA).getId(), usuario(AMIGA).getId()).orElseThrow().getId();
            mvc.perform(post("/amigos/" + pedido + "/aceitar").with(csrf()).with(como(AMIGA)));
        }
        mvc.perform(post("/receitas/" + receitaId + "/compartilhar").param("amigo", String.valueOf(usuario(AMIGA).getId())).with(csrf()).with(como(DONA)))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void donaComentaEOComentarioApareceNaPagina() throws Exception {
        Long id = criarReceita("Bolo com comentário");
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Ficou ótimo com canela.").with(csrf()).with(como(DONA)))
                .andExpect(redirectedUrl("/receitas/" + id + "#comentarios")).andExpect(flash().attributeExists("sucesso"));
        mvc.perform(get("/receitas/" + id).with(como(DONA))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ficou ótimo com canela.")));
    }

    @Test
    void amigaComRecebimentoPodeComentarEDonaVe() throws Exception {
        Long id = criarReceita("Torta compartilhada");
        compartilharComAmiga(id);
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Posso trocar o açúcar por mel?").with(csrf()).with(como(AMIGA)))
                .andExpect(flash().attributeExists("sucesso"));
        mvc.perform(get("/receitas/" + id).with(como(DONA))).andExpect(content().string(containsString("Posso trocar o açúcar por mel?")));
        mvc.perform(get("/receitas/" + id).with(como(AMIGA))).andExpect(content().string(containsString("Posso trocar o açúcar por mel?")));
    }

    @Test
    void quemNaoVeAReceitaNaoComentaNemLe() throws Exception {
        Long id = criarReceita("Receita reservada");
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Intruso").with(csrf()).with(como(ESTRANHO)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(0, comentarios.countByReceitaId(id));
        mvc.perform(get("/receitas/" + id).with(como(ESTRANHO))).andExpect(redirectedUrl("/receitas"));
    }

    @Test
    void comentarioVazioOuLongoDeMaisERecusado() throws Exception {
        Long id = criarReceita("Receita validada");
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "   ").with(csrf()).with(como(DONA)))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "x".repeat(501)).with(csrf()).with(como(DONA)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(0, comentarios.countByReceitaId(id));
    }

    @Test
    void autorEDonaExcluemMasOutraPessoaNao() throws Exception {
        Long id = criarReceita("Receita moderada");
        compartilharComAmiga(id);
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Comentário da amiga").with(csrf()).with(como(AMIGA)));
        Comentario c = comentarios.findAllByReceitaIdOrderByCriadoEmAscIdAsc(id).get(0);

        mvc.perform(post("/receitas/" + id + "/comentarios/" + c.getId() + "/excluir").with(csrf()).with(como(ESTRANHO)))
                .andExpect(flash().attributeExists("erro"));
        assertEquals(1, comentarios.countByReceitaId(id));

        mvc.perform(post("/receitas/" + id + "/comentarios/" + c.getId() + "/excluir").with(csrf()).with(como(DONA)))
                .andExpect(flash().attributeExists("sucesso"));
        assertEquals(0, comentarios.countByReceitaId(id));

        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Outro da amiga").with(csrf()).with(como(AMIGA)));
        Comentario outro = comentarios.findAllByReceitaIdOrderByCriadoEmAscIdAsc(id).get(0);
        mvc.perform(post("/receitas/" + id + "/comentarios/" + outro.getId() + "/excluir").with(csrf()).with(como(AMIGA)))
                .andExpect(flash().attributeExists("sucesso"));
        assertEquals(0, comentarios.countByReceitaId(id));
    }

    @Test
    void excluirAReceitaApagaSeusComentarios() throws Exception {
        Long id = criarReceita("Receita descartada");
        mvc.perform(post("/receitas/" + id + "/comentarios").param("texto", "Vai sumir junto").with(csrf()).with(como(DONA)));
        assertEquals(1, comentarios.countByReceitaId(id));
        mvc.perform(post("/receitas/" + id + "/excluir").with(csrf()).with(como(DONA))).andExpect(redirectedUrl("/receitas"));
        assertEquals(0, comentarios.countByReceitaId(id));
    }

    @Test
    void paginaDaReceitaSemComentariosMostraAvisoVazio() throws Exception {
        Long id = criarReceita("Receita nova");
        mvc.perform(get("/receitas/" + id).with(como(DONA))).andExpect(content().string(containsString("Ainda não há comentários.")))
                .andExpect(content().string(not(containsString("Excluir este comentário"))));
    }
}
