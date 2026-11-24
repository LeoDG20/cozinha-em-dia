package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes unitários da cópia e da adaptação de receitas. */
class CopiaReceitaServiceTest {
    private ReceitaRepository receitas;
    private ReceitaIngredienteRepository itens;
    private ArmazenamentoArquivos arquivos;
    private CopiaReceitaService service;

    private Usuario ana;
    private Usuario bruno;
    private Receita bolo;

    @BeforeEach
    void preparar() {
        receitas = mock(ReceitaRepository.class);
        itens = mock(ReceitaIngredienteRepository.class);
        arquivos = mock(ArmazenamentoArquivos.class);
        service = new CopiaReceitaService(receitas, itens, arquivos);

        ana = usuario(1L, "Ana");
        bruno = usuario(2L, "Bruno");

        bolo = new Receita();
        ReflectionTestUtils.setField(bolo, "id", 10L);
        bolo.setUsuario(ana);
        bolo.setNome("Bolo de cenoura");
        bolo.setDescricao("Com cobertura");
        bolo.setModoPreparo("1. Bata\n2. Asse");
        bolo.setTempoPreparoMin(50);
        bolo.setPorcoes(8);
        bolo.setImagemUrl("https://exemplo.test/bolo.jpg");
        bolo.setImagemArquivo("capa-original.jpg");
        bolo.setVideoUrl("https://youtu.be/dQw4w9WgXcQ");
        bolo.setVideoArquivo("video-original.mp4");
        bolo.getEtapas().add(new EtapaReceita(bolo, 1, "Bata no liquidificador", null));
        bolo.getEtapas().add(new EtapaReceita(bolo, 2, "Asse por 40 minutos", "etapa-original.png"));

        Ingrediente cenoura = new Ingrediente("Cenoura");
        Ingrediente farinha = new Ingrediente("Farinha de trigo");
        when(itens.findAllByReceitaIdOrderByIdAsc(10L)).thenReturn(List.of(
                new ReceitaIngrediente(bolo, cenoura, 3.0, "unidade"),
                new ReceitaIngrediente(bolo, farinha, 300.0, "g")));
        when(receitas.save(any(Receita.class))).thenAnswer(c -> c.getArgument(0));
        when(arquivos.copiar(any())).thenAnswer(c -> "copia-de-" + c.getArgument(0));
    }

    private static Usuario usuario(Long id, String nome) {
        Usuario u = new Usuario(nome, nome.toLowerCase() + "@teste.com", "hash");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void adaptarCopiaTudoEMarcaAOrigem() {
        Receita versao = service.adaptar(bolo, bruno, "Bolo de cenoura sem glúten");

        assertNotSame(bolo, versao);
        assertSame(bruno, versao.getUsuario());
        assertEquals("Bolo de cenoura sem glúten", versao.getNome());
        assertEquals("Bolo de cenoura", versao.getOrigemNome());
        assertTrue(versao.isAdaptada());
        assertEquals("Com cobertura", versao.getDescricao());
        assertEquals("1. Bata\n2. Asse", versao.getModoPreparo());
        assertEquals(50, versao.getTempoPreparoMin());
        assertEquals(8, versao.getPorcoes());
        assertEquals("https://exemplo.test/bolo.jpg", versao.getImagemUrl());
        assertEquals("https://youtu.be/dQw4w9WgXcQ", versao.getVideoUrl());
    }

    @Test
    void adaptarDuplicaOsArquivosEAsEtapas() {
        Receita versao = service.adaptar(bolo, bruno, "Bolo adaptado");

        assertEquals("copia-de-capa-original.jpg", versao.getImagemArquivo());
        assertEquals("copia-de-video-original.mp4", versao.getVideoArquivo());
        assertEquals(2, versao.getEtapas().size());
        assertEquals("Bata no liquidificador", versao.getEtapas().get(0).getDescricao());
        assertNull(versao.getEtapas().get(0).getImagemArquivo());
        assertEquals("copia-de-etapa-original.png", versao.getEtapas().get(1).getImagemArquivo());
        assertSame(versao, versao.getEtapas().get(1).getReceita());
    }

    @Test
    @SuppressWarnings("unchecked")
    void adaptarCopiaAsLinhasDeIngredientesParaANovaReceita() {
        Receita versao = service.adaptar(bolo, bruno, "Bolo adaptado");

        ArgumentCaptor<List<ReceitaIngrediente>> captor = ArgumentCaptor.forClass(List.class);
        verify(itens).saveAll(captor.capture());
        List<ReceitaIngrediente> copiados = captor.getValue();
        assertEquals(2, copiados.size());
        assertSame(versao, copiados.get(0).getReceita());
        assertEquals("Cenoura", copiados.get(0).getIngrediente().getNome());
        assertEquals(3.0, copiados.get(0).getQuantidade());
        assertEquals("g", copiados.get(1).getUnidade());
    }

    @Test
    void adaptarNaoAlteraAReceitaDeOrigem() {
        service.adaptar(bolo, bruno, "Bolo adaptado");

        assertEquals("Bolo de cenoura", bolo.getNome());
        assertSame(ana, bolo.getUsuario());
        assertNull(bolo.getOrigemNome());
        assertFalse(bolo.isAdaptada());
        assertEquals("capa-original.jpg", bolo.getImagemArquivo());
        assertEquals(2, bolo.getEtapas().size());
    }

    @Test
    void adaptarNormalizaEspacosDoNome() {
        Receita versao = service.adaptar(bolo, bruno, "   Bolo   de   cenoura   light  ");

        assertEquals("Bolo de cenoura light", versao.getNome());
    }

    @Test
    void adaptarRecusaNomeVazioNuloOuMuitoLongo() {
        assertThrows(IllegalArgumentException.class, () -> service.adaptar(bolo, bruno, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.adaptar(bolo, bruno, null));
        IllegalArgumentException longo = assertThrows(IllegalArgumentException.class, () -> service.adaptar(bolo, bruno, "x".repeat(121)));
        assertTrue(longo.getMessage().contains("120"));
        assertEquals("x".repeat(120), service.adaptar(bolo, bruno, "x".repeat(120)).getNome());
    }

    @Test
    void adaptarRecusaNomeQueOUsuarioJaTem() {
        when(receitas.existsByUsuarioIdAndNomeIgnoreCase(2L, "Bolo de cenoura")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.adaptar(bolo, bruno, "  Bolo de cenoura "));

        assertTrue(erro.getMessage().contains("Você já tem uma receita chamada"));
        verify(receitas, never()).save(any());
        verify(itens, never()).saveAll(anyList());
        verify(arquivos, never()).copiar(any());
    }

    @Test
    void copiarMantemONomeEntreUsuariosENaoMarcaComoAdaptada() {
        Receita copia = service.copiar(bolo, bruno);

        assertEquals("Bolo de cenoura", copia.getNome());
        assertSame(bruno, copia.getUsuario());
        assertNull(copia.getOrigemNome());
        assertFalse(copia.isAdaptada());
        assertNotEquals(bolo.getImagemArquivo(), copia.getImagemArquivo());
        assertEquals(2, copia.getEtapas().size());
    }
}
