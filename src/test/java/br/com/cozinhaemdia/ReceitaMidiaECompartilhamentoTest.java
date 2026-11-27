package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.service.ArmazenamentoArquivos;
import br.com.cozinhaemdia.service.ReceitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Receitas com imagem, vídeos, etapas com foto e compartilhamento entre usuários, de ponta a ponta. */
@SpringBootTest
@AutoConfigureMockMvc
class ReceitaMidiaECompartilhamentoTest {
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    static final String DONO = "demo@cozinha.local", AMIGO = "amigo.midia@teste.local", ESTRANHO = "estranho.midia@teste.local";

    @TempDir static Path pastaUploads;
    @Autowired MockMvc mvc;
    @Autowired ReceitaRepository receitas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder codificador;
    @Autowired UserDetailsService detalhes;
    @Autowired ArmazenamentoArquivos arquivos;
    @Autowired ReceitaService receitaService;
    @Autowired PlatformTransactionManager transacoes;
    @Autowired br.com.cozinhaemdia.repository.AmizadeRepository amizades;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
        registro.add("app.uploads.dir", () -> pastaUploads.toString());
    }

    @BeforeEach
    void criarUsuarios() {
        for (String email : List.of(AMIGO, ESTRANHO))
            if (usuarios.findByEmailIgnoreCase(email).isEmpty()) usuarios.save(new Usuario(email.split("\\.")[0], email, codificador.encode("senha-forte-123")));
        if (!amizades.saoAmigos(usuario(DONO).getId(), usuario(AMIGO).getId())) {
            br.com.cozinhaemdia.model.Amizade a = new br.com.cozinhaemdia.model.Amizade(usuario(DONO), usuario(AMIGO));
            a.setAceita(true);
            amizades.save(a);
        }
    }

    private RequestPostProcessor como(String email) { return user(detalhes.loadUserByUsername(email)); }

    private static MockMultipartFile arq(String campo, byte[] conteudo) { return new MockMultipartFile(campo, "arquivo.bin", "application/octet-stream", conteudo); }
    private static MockMultipartFile vazio(String campo) { return new MockMultipartFile(campo, "", "application/octet-stream", new byte[0]); }

    private MockHttpServletRequestBuilder formulario(String url, String nome) {
        return multipart(url).file(vazio("etapaFoto_10")).file(arq("etapaFoto_20", PNG)).file(arq("imagem", JPEG))
                .param("nome", nome).param("descricao", "Teste de mídia")
                .param("tempoPreparoMin", "30").param("porcoes", "4")
                .param("ingNome", "farna").param("ingQtd", "200").param("ingUn", "g")
                .param("ingNome", "ovo").param("ingQtd", "2").param("ingUn", "unidade")
                .param("etapaChave", "10").param("etapaChave", "20")
                .param("etapaTexto_10", "Misture a farinha, o ovo e o sal.").param("etapaFotoAtual_10", "")
                .param("etapaTexto_20", "Asse por 30 minutos.").param("etapaFotoAtual_20", "")
                .param("videoUrl", "https://youtu.be/dQw4w9WgXcQ")
                .with(csrf());
    }

    private Long criar(String email, String nome) throws Exception {
        MvcResult r = mvc.perform(formulario("/receitas", nome).with(como(email))).andExpect(status().is3xxRedirection()).andReturn();
        String destino = r.getResponse().getRedirectedUrl();
        assertNotNull(destino);
        return Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
    }

    /** Carrega a receita com as etapas dentro de uma transação, para poder inspecioná-la no teste. */
    private Receita carregar(Long id) {
        return new TransactionTemplate(transacoes).execute(st -> { Receita r = receitas.findById(id).orElseThrow(); r.getEtapas().size(); return r; });
    }

    private Usuario usuario(String email) { return usuarios.findByEmailIgnoreCase(email).orElseThrow(); }

    @Test
    void cadastraReceitaComImagemVideoEtapasFotoECorrigeIngrediente() throws Exception {
        MvcResult r = mvc.perform(formulario("/receitas", "Bolo com mídia").with(como(DONO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("sucesso", "Receita cadastrada com sucesso.")).andReturn();
        @SuppressWarnings("unchecked") List<br.com.cozinhaemdia.service.CorretorIngredientes.Aviso> avisos = (List<br.com.cozinhaemdia.service.CorretorIngredientes.Aviso>) r.getFlashMap().get("avisos");
        assertTrue(avisos.stream().anyMatch(a -> a.original().contains("farna") && a.corrigido().contains("Farinha")), "esperava aviso de correção de farna");

        Long idCriado = receitas.findAllByUsuarioIdOrderByNomeAsc(usuario(DONO).getId()).stream().filter(x -> x.getNome().equals("Bolo com mídia")).findFirst().orElseThrow().getId();
        Receita receita = carregar(idCriado);
        assertTrue(receita.getImagemArquivo().endsWith(".jpg"));
        assertEquals("dQw4w9WgXcQ", receita.getVideoYoutubeId());
        assertEquals(2, receita.getEtapas().size());
        assertNull(receita.getEtapas().get(0).getImagemArquivo(), "a foto deve ficar na etapa 2, não na 1");
        assertTrue(receita.getEtapas().get(1).getImagemArquivo().endsWith(".png"));
        assertEquals("1. Misture a farinha, o ovo e o sal.\n2. Asse por 30 minutos.", receita.getModoPreparo());
        assertTrue(receitaService.textoIngredientes(receita).startsWith("Farinha | 200 | g"));
        assertTrue(arquivos.carregar(receita.getImagemArquivo()).isPresent());

        mvc.perform(get("/midia/" + receita.getImagemArquivo()).with(como(DONO))).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/receitas/" + receita.getId()).with(como(DONO))).andExpect(status().isOk())
                .andExpect(content().string(containsString("youtube-nocookie.com/embed/dQw4w9WgXcQ")))
                .andExpect(content().string(containsString("Misture a farinha, o ovo e o sal.")))
                .andExpect(content().string(containsString("/midia/" + receita.getEtapas().get(1).getImagemArquivo())));
    }

    @Test
    void midiaExigeLoginECsrfProtegeOEnvio() throws Exception {
        mvc.perform(get("/midia/qualquer.jpg")).andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/receitas").param("nome", "Sem token").with(como(DONO))).andExpect(status().isForbidden());
    }

    @Test
    void arquivoInvalidoVoltaAoFormularioComOsDadosDigitadosENadaEhGravado() throws Exception {
        long antes = receitas.countByUsuarioId(usuario(DONO).getId());

        mvc.perform(multipart("/receitas").file(new MockMultipartFile("imagem", "foto.jpg", "image/jpeg", "isto não é uma imagem".getBytes()))
                        .param("nome", "Receita com erro").param("descricao", "d")
                        .param("ingNome", "ovo").param("ingQtd", "1").param("ingUn", "unidade")
                        .param("etapaChave", "1").param("etapaTexto_1", "Passo").param("etapaFotoAtual_1", "")
                        .with(csrf()).with(como(DONO)))
                .andExpect(status().isOk()).andExpect(view().name("receita-form"))
                .andExpect(model().attributeExists("erro"))
                .andExpect(content().string(containsString("Receita com erro")))
                .andExpect(content().string(containsString("Passo")));

        assertEquals(antes, receitas.countByUsuarioId(usuario(DONO).getId()));
    }

    @Test
    void compartilhaComAmigoQueVeSalvaCopiaIndependenteEDonoRevoga() throws Exception {
        Long id = criar(DONO, "Bolo para dividir");
        Receita original = carregar(id);
        String capaOriginal = original.getImagemArquivo();

        // Antes de compartilhar, o amigo não enxerga a receita.
        mvc.perform(get("/receitas/" + id).with(como(AMIGO))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("erro", "Receita não encontrada."));

        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).param("mensagem", "Experimente!").with(csrf()).with(como(DONO)))
                .andExpect(flash().attributeExists("sucesso"));
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).with(csrf()).with(como(DONO)))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", "ninguem@teste.local").with(csrf()).with(como(DONO)))
                .andExpect(flash().attributeExists("erro"));

        // O amigo vê a lista, abre a receita, mas não pode editar nem excluir a do dono.
        mvc.perform(get("/compartilhadas").with(como(AMIGO))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bolo para dividir"))).andExpect(content().string(containsString("Experimente!")));
        mvc.perform(get("/receitas/" + id).with(como(AMIGO))).andExpect(status().isOk()).andExpect(model().attribute("dono", false));
        mvc.perform(post("/receitas/" + id + "/excluir").with(csrf()).with(como(AMIGO))).andExpect(status().is3xxRedirection());
        mvc.perform(get("/receitas/" + id + "/editar").with(como(AMIGO))).andExpect(flash().attribute("erro", "Receita não encontrada."));
        assertTrue(receitas.findById(id).isPresent());

        // Um terceiro não tem acesso.
        mvc.perform(get("/receitas/" + id).with(como(ESTRANHO))).andExpect(flash().attribute("erro", "Receita não encontrada."));
        mvc.perform(post("/receitas/" + id + "/salvar-copia").with(csrf()).with(como(ESTRANHO))).andExpect(flash().attributeExists("erro"));

        // Salvar cópia cria uma receita própria, com arquivos duplicados.
        MvcResult salvo = mvc.perform(post("/receitas/" + id + "/salvar-copia").with(csrf()).with(como(AMIGO))).andExpect(status().is3xxRedirection()).andReturn();
        Long idCopia = Long.valueOf(salvo.getResponse().getRedirectedUrl().replaceAll(".*/", ""));
        Receita copia = carregar(idCopia);
        assertEquals(usuario(AMIGO).getId(), copia.getUsuario().getId());
        assertNotEquals(capaOriginal, copia.getImagemArquivo());
        assertEquals(2, copia.getEtapas().size());
        mvc.perform(post("/receitas/" + id + "/salvar-copia").with(csrf()).with(como(AMIGO))).andExpect(flash().attributeExists("erro"));

        // Excluir o original apaga os arquivos dele, mas a cópia do amigo continua inteira.
        mvc.perform(post("/receitas/" + id + "/excluir").with(csrf()).with(como(DONO))).andExpect(flash().attribute("sucesso", "Receita excluída."));
        assertTrue(receitas.findById(id).isEmpty());
        assertFalse(Files.exists(pastaUploads.resolve(capaOriginal)));
        assertTrue(Files.exists(pastaUploads.resolve(copia.getImagemArquivo())));
        mvc.perform(get("/receitas/" + idCopia).with(como(AMIGO))).andExpect(status().isOk()).andExpect(model().attribute("dono", true));
    }

    @Test
    void destinatarioPodeDispensarEDonoPodeRevogar() throws Exception {
        Long id = criar(DONO, "Receita revogável");
        mvc.perform(post("/receitas/" + id + "/compartilhar").param("email", AMIGO).with(csrf()).with(como(DONO)));
        Long compartilhamento = ((List<?>) mvc.perform(get("/compartilhadas").with(como(AMIGO))).andReturn().getModelAndView().getModel().get("recebidas"))
                .stream().map(o -> ((br.com.cozinhaemdia.model.Compartilhamento) o).getId()).findFirst().orElseThrow();

        mvc.perform(post("/compartilhamentos/" + compartilhamento + "/remover").with(csrf()).with(como(ESTRANHO))).andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/compartilhamentos/" + compartilhamento + "/remover").with(csrf()).with(como(DONO))).andExpect(flash().attribute("sucesso", "Compartilhamento removido."));
        mvc.perform(get("/receitas/" + id).with(como(AMIGO))).andExpect(flash().attribute("erro", "Receita não encontrada."));
    }

    @Test
    void buscaEncontraPorNomeIngredienteECorrigeErroDeDigitacao() throws Exception {
        criar(DONO, "Torta de limão buscável");

        Map<String, Object> porIngrediente = mvc.perform(get("/receitas").param("q", "farna").with(como(DONO))).andReturn().getModelAndView().getModel();
        assertTrue(((List<?>) porIngrediente.get("analises")).size() >= 1, "farna deve encontrar receitas com farinha");

        Map<String, Object> porNome = mvc.perform(get("/receitas").param("q", "torta limao").with(como(DONO))).andReturn().getModelAndView().getModel();
        assertEquals(1, ((List<?>) porNome.get("analises")).size());

        Map<String, Object> nada = mvc.perform(get("/receitas").param("q", "xyzqwk").with(como(DONO))).andReturn().getModelAndView().getModel();
        assertEquals(0, ((List<?>) nada.get("analises")).size());
    }

    @Test
    void edicaoMantemFotoDaEtapaTrocaAImagemEPermiteRemoverVideo() throws Exception {
        Long id = criar(DONO, "Receita editável");
        Receita antes = carregar(id);
        String fotoEtapa = antes.getEtapas().get(1).getImagemArquivo();
        String capaAntiga = antes.getImagemArquivo();

        mvc.perform(multipart("/receitas/" + id + "/editar").file(vazio("etapaFoto_10")).file(vazio("etapaFoto_20")).file(arq("imagem", PNG))
                        .param("nome", "Receita editada").param("descricao", "x")
                        .param("ingNome", "ovo").param("ingQtd", "3").param("ingUn", "unidade")
                        .param("etapaChave", "10").param("etapaChave", "20")
                        .param("etapaTexto_10", "Primeiro passo").param("etapaFotoAtual_10", "")
                        .param("etapaTexto_20", "Segundo passo").param("etapaFotoAtual_20", fotoEtapa)
                        .param("videoUrl", "").param("removerVideo", "on").with(csrf()).with(como(DONO)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("sucesso", "Receita atualizada."));

        Receita depois = carregar(id);
        assertEquals("Receita editada", depois.getNome());
        assertEquals(fotoEtapa, depois.getEtapas().get(1).getImagemArquivo());
        assertTrue(depois.getImagemArquivo().endsWith(".png"));
        assertFalse(Files.exists(pastaUploads.resolve(capaAntiga)));
        assertNull(depois.getVideoUrl());
    }
}
