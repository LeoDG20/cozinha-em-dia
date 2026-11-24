package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Compartilhamento;
import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.CompartilhamentoRepository;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes unitários das regras de compartilhamento de receitas entre usuários. */
class CompartilhamentoServiceTest {
    private CompartilhamentoRepository compartilhamentos;
    private ReceitaRepository receitas;
    private ReceitaIngredienteRepository receitaIngredientes;
    private UsuarioRepository usuarios;
    private ArmazenamentoArquivos arquivos;
    private CompartilhamentoService service;

    private Usuario ana;
    private Usuario bruno;
    private Receita bolo;

    @BeforeEach
    void preparar() {
        compartilhamentos = mock(CompartilhamentoRepository.class);
        receitas = mock(ReceitaRepository.class);
        receitaIngredientes = mock(ReceitaIngredienteRepository.class);
        usuarios = mock(UsuarioRepository.class);
        arquivos = mock(ArmazenamentoArquivos.class);
        service = new CompartilhamentoService(compartilhamentos, receitas, usuarios, new CopiaReceitaService(receitas, receitaIngredientes, arquivos));

        ana = usuario(1L, "Ana", "ana@teste.com");
        bruno = usuario(2L, "Bruno", "bruno@teste.com");
        bolo = new Receita();
        ReflectionTestUtils.setField(bolo, "id", 10L);
        bolo.setUsuario(ana);
        bolo.setNome("Bolo de cenoura");
        bolo.setModoPreparo("1. Misture\n2. Asse");

        when(usuarios.findByEmailIgnoreCase("bruno@teste.com")).thenReturn(Optional.of(bruno));
        when(usuarios.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.of(ana));
        when(compartilhamentos.save(any(Compartilhamento.class))).thenAnswer(c -> c.getArgument(0));
        when(receitas.save(any(Receita.class))).thenAnswer(c -> c.getArgument(0));
    }

    private static Usuario usuario(Long id, String nome, String email) {
        Usuario u = new Usuario(nome, email, "hash");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void donoCompartilhaComOutroUsuarioPeloEmail() {
        Compartilhamento c = service.compartilhar(bolo, ana, " bruno@teste.com ", "  Esse ficou ótimo!  ");

        assertSame(bruno, c.getDestino());
        assertSame(bolo, c.getReceita());
        assertEquals("Esse ficou ótimo!", c.getMensagem());
        assertFalse(c.isCopiada());
    }

    @Test
    void mensagemVaziaFicaNula() {
        assertNull(service.compartilhar(bolo, ana, "bruno@teste.com", "   ").getMensagem());
    }

    @Test
    void somenteODonoPodeCompartilhar() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.compartilhar(bolo, bruno, "ana@teste.com", null));

        assertTrue(erro.getMessage().contains("suas próprias"));
        verify(compartilhamentos, never()).save(any());
    }

    @Test
    void recusaEmailVazioDesconhecidoOuDoProprioUsuario() {
        assertThrows(IllegalArgumentException.class, () -> service.compartilhar(bolo, ana, " ", null));
        IllegalArgumentException desconhecido = assertThrows(IllegalArgumentException.class, () -> service.compartilhar(bolo, ana, "ninguem@teste.com", null));
        assertTrue(desconhecido.getMessage().contains("Não há usuário"));
        IllegalArgumentException proprio = assertThrows(IllegalArgumentException.class, () -> service.compartilhar(bolo, ana, "ana@teste.com", null));
        assertTrue(proprio.getMessage().contains("você mesmo"));
        verify(compartilhamentos, never()).save(any());
    }

    @Test
    void recusaCompartilharDuasVezesComAMesmaPessoa() {
        when(compartilhamentos.existsByReceitaIdAndDestinoId(10L, 2L)).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.compartilhar(bolo, ana, "bruno@teste.com", null));

        assertTrue(erro.getMessage().contains("já foi compartilhada com Bruno"));
    }

    @Test
    void recusaMensagemMuitoLonga() {
        assertThrows(IllegalArgumentException.class, () -> service.compartilhar(bolo, ana, "bruno@teste.com", "x".repeat(201)));
    }

    @Test
    void receitaVisivelParaODonoEParaQuemRecebeuMasNaoParaOutros() {
        Usuario carla = usuario(3L, "Carla", "carla@teste.com");
        when(receitas.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(bolo));
        when(receitas.findById(10L)).thenReturn(Optional.of(bolo));
        when(compartilhamentos.existsByReceitaIdAndDestinoId(10L, 2L)).thenReturn(true);

        assertTrue(service.visivelPara(10L, ana.getId()).isPresent());
        assertTrue(service.visivelPara(10L, bruno.getId()).isPresent());
        assertTrue(service.visivelPara(10L, carla.getId()).isEmpty());
    }

    @Test
    void destinatarioEDonoPodemDesfazerOCompartilhamentoMasOutrosNao() {
        Compartilhamento c = new Compartilhamento(bolo, bruno, null);
        ReflectionTestUtils.setField(c, "id", 50L);
        when(compartilhamentos.findById(50L)).thenReturn(Optional.of(c));

        assertFalse(service.remover(50L, 99L));
        verify(compartilhamentos, never()).delete(any());
        assertTrue(service.remover(50L, bruno.getId()));
        assertTrue(service.remover(50L, ana.getId()));
        assertFalse(service.remover(404L, ana.getId()));
    }

    @Test
    void salvarCopiaDuplicaReceitaIngredientesEtapasEArquivos() {
        bolo.setDescricao("Fofinho");
        bolo.setTempoPreparoMin(50);
        bolo.setPorcoes(8);
        bolo.setImagemArquivo("capa.jpg");
        bolo.setVideoUrl("https://youtu.be/dQw4w9WgXcQ");
        bolo.getEtapas().add(new EtapaReceita(bolo, 1, "Misture", "etapa1.jpg"));
        bolo.getEtapas().add(new EtapaReceita(bolo, 2, "Asse", null));
        Ingrediente cenoura = new Ingrediente("Cenoura");
        when(receitaIngredientes.findAllByReceitaIdOrderByIdAsc(10L)).thenReturn(List.of(new ReceitaIngrediente(bolo, cenoura, 3.0, "unidade")));
        Compartilhamento c = new Compartilhamento(bolo, bruno, null);
        when(compartilhamentos.findByReceitaIdAndDestinoId(10L, 2L)).thenReturn(Optional.of(c));
        when(arquivos.copiar("capa.jpg")).thenReturn("capa-copia.jpg");
        when(arquivos.copiar("etapa1.jpg")).thenReturn("etapa1-copia.jpg");

        Receita copia = service.salvarCopia(10L, bruno);

        assertNotSame(bolo, copia);
        assertSame(bruno, copia.getUsuario());
        assertEquals("Bolo de cenoura", copia.getNome());
        assertEquals(50, copia.getTempoPreparoMin());
        assertEquals("capa-copia.jpg", copia.getImagemArquivo());
        assertEquals("https://youtu.be/dQw4w9WgXcQ", copia.getVideoUrl());
        assertEquals(2, copia.getEtapas().size());
        assertEquals("etapa1-copia.jpg", copia.getEtapas().get(0).getImagemArquivo());
        assertNull(copia.getEtapas().get(1).getImagemArquivo());
        assertSame(copia, copia.getEtapas().get(0).getReceita());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReceitaIngrediente>> itens = ArgumentCaptor.forClass(List.class);
        verify(receitaIngredientes).saveAll(itens.capture());
        assertEquals(1, itens.getValue().size());
        assertSame(copia, itens.getValue().get(0).getReceita());
        assertSame(cenoura, itens.getValue().get(0).getIngrediente());
        assertTrue(c.isCopiada());
    }

    @Test
    void naoSalvaCopiaDeReceitaNaoCompartilhadaOuJaSalva() {
        when(compartilhamentos.findByReceitaIdAndDestinoId(10L, 2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.salvarCopia(10L, bruno));

        Compartilhamento ja = new Compartilhamento(bolo, bruno, null);
        ja.setCopiada(true);
        when(compartilhamentos.findByReceitaIdAndDestinoId(10L, 2L)).thenReturn(Optional.of(ja));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.salvarCopia(10L, bruno));
        assertTrue(erro.getMessage().contains("já salvou"));
        verify(receitas, never()).save(any());
    }
}
