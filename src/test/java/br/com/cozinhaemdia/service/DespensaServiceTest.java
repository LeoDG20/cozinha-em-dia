package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Despensa;
import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.DespensaRepository;
import br.com.cozinhaemdia.repository.IngredienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes unitários das regras da despensa: soma de quantidades, unidade única por ingrediente e remoção. */
class DespensaServiceTest {
    private static final Long USUARIO_ID = 7L;
    private static final Long INGREDIENTE_ID = 3L;

    private DespensaRepository despensa;
    private IngredienteRepository ingredientes;
    private DespensaService service;
    private Usuario usuario;
    private Ingrediente farinha;

    @BeforeEach
    void preparar() {
        despensa = mock(DespensaRepository.class);
        ingredientes = mock(IngredienteRepository.class);
        service = new DespensaService(despensa, ingredientes);

        usuario = new Usuario("Teste", "teste@cozinha.local", "hash");
        ReflectionTestUtils.setField(usuario, "id", USUARIO_ID);
        farinha = new Ingrediente("Farinha");
        ReflectionTestUtils.setField(farinha, "id", INGREDIENTE_ID);

        when(ingredientes.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(ingredientes.save(any(Ingrediente.class))).thenAnswer(chamada -> {
            Ingrediente novo = chamada.getArgument(0);
            ReflectionTestUtils.setField(novo, "id", INGREDIENTE_ID);
            return novo;
        });
        when(despensa.findByUsuarioIdAndIngredienteId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void listarConsultaPelaContaDoUsuario() {
        List<Despensa> itens = List.of(new Despensa(usuario, farinha, 1.0, "kg"));
        when(despensa.findAllByUsuarioIdOrderByIngredienteNomeAsc(USUARIO_ID)).thenReturn(itens);

        assertSame(itens, service.listar(USUARIO_ID));
    }

    @Test
    void adicionarCriaIngredienteEItemNovos() {
        service.adicionar(usuario, "  Farinha  ", 500.0, " G ");

        ArgumentCaptor<Ingrediente> ingrediente = ArgumentCaptor.forClass(Ingrediente.class);
        verify(ingredientes).save(ingrediente.capture());
        assertEquals("Farinha", ingrediente.getValue().getNome());

        ArgumentCaptor<Despensa> item = ArgumentCaptor.forClass(Despensa.class);
        verify(despensa).save(item.capture());
        assertEquals(500.0, item.getValue().getQuantidade(), 0.0001);
        assertEquals("g", item.getValue().getUnidade());
        assertSame(usuario, item.getValue().getUsuario());
    }

    @Test
    void adicionarReaproveitaIngredienteJaCadastrado() {
        when(ingredientes.findByNomeIgnoreCase("farinha")).thenReturn(Optional.of(farinha));

        service.adicionar(usuario, "farinha", 1.0, "kg");

        verify(ingredientes, never()).save(any(Ingrediente.class));
        ArgumentCaptor<Despensa> item = ArgumentCaptor.forClass(Despensa.class);
        verify(despensa).save(item.capture());
        assertSame(farinha, item.getValue().getIngrediente());
    }

    @Test
    void adicionarSomaQuantidadeQuandoOItemJaExiste() {
        Despensa existente = new Despensa(usuario, farinha, 200.0, "g");
        when(ingredientes.findByNomeIgnoreCase("Farinha")).thenReturn(Optional.of(farinha));
        when(despensa.findByUsuarioIdAndIngredienteId(USUARIO_ID, INGREDIENTE_ID)).thenReturn(Optional.of(existente));

        service.adicionar(usuario, "Farinha", 300.5, "G");

        assertEquals(500.5, existente.getQuantidade(), 0.0001);
        verify(despensa).save(existente);
    }

    @Test
    void adicionarConverteParaAUnidadeJaCadastradaAoSomar() {
        Despensa existente = new Despensa(usuario, farinha, 200.0, "g");
        when(ingredientes.findByNomeIgnoreCase("Farinha")).thenReturn(Optional.of(farinha));
        when(despensa.findByUsuarioIdAndIngredienteId(USUARIO_ID, INGREDIENTE_ID)).thenReturn(Optional.of(existente));

        service.adicionar(usuario, "Farinha", 1.5, "kg");

        assertEquals(1700.0, existente.getQuantidade(), 0.0001);
        assertEquals("g", existente.getUnidade());
        verify(despensa).save(existente);
    }

    @Test
    void adicionarRecusaUnidadeIncompativelComAJaCadastrada() {
        Despensa existente = new Despensa(usuario, farinha, 200.0, "g");
        when(ingredientes.findByNomeIgnoreCase("Farinha")).thenReturn(Optional.of(farinha));
        when(despensa.findByUsuarioIdAndIngredienteId(USUARIO_ID, INGREDIENTE_ID)).thenReturn(Optional.of(existente));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.adicionar(usuario, "Farinha", 1.0, "ml"));

        assertTrue(erro.getMessage().contains("'g'"));
        assertEquals(200.0, existente.getQuantidade(), 0.0001);
        verify(despensa, never()).save(any(Despensa.class));
    }

    @Test
    void atualizarSubstituiQuantidadeEUnidadeDoItem() {
        Despensa item = new Despensa(usuario, farinha, 200.0, "g");
        when(despensa.findByIdAndUsuarioId(10L, USUARIO_ID)).thenReturn(Optional.of(item));

        service.atualizar(10L, USUARIO_ID, 2.5, " KG ");

        assertEquals(2.5, item.getQuantidade(), 0.0001);
        assertEquals("kg", item.getUnidade());
        verify(despensa).save(item);
    }

    @Test
    void atualizarValidaQuantidadeEUnidade() {
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, USUARIO_ID, null, "g"));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, USUARIO_ID, 0.0, "g"));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, USUARIO_ID, 1.0, " "));

        verify(despensa, never()).save(any(Despensa.class));
    }

    @Test
    void atualizarItemDeOutroUsuarioFalha() {
        when(despensa.findByIdAndUsuarioId(10L, 99L)).thenReturn(Optional.empty());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, 99L, 1.0, "g"));

        assertEquals("Item não encontrado na sua despensa.", erro.getMessage());
        verify(despensa, never()).save(any(Despensa.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void adicionarExigeNomeDoIngrediente(String nome) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.adicionar(usuario, nome, 1.0, "kg"));

        assertEquals("Informe o nome do ingrediente.", erro.getMessage());
        verify(despensa, never()).save(any(Despensa.class));
    }

    @Test
    void adicionarExigeQuantidadePositiva() {
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(usuario, "Farinha", null, "kg"));
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(usuario, "Farinha", 0.0, "kg"));
        assertThrows(IllegalArgumentException.class, () -> service.adicionar(usuario, "Farinha", -2.0, "kg"));

        verify(despensa, never()).save(any(Despensa.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void adicionarExigeUnidade(String unidade) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.adicionar(usuario, "Farinha", 1.0, unidade));

        assertEquals("Selecione uma unidade.", erro.getMessage());
        verify(despensa, never()).save(any(Despensa.class));
    }

    @Test
    void removerApagaSomenteItemDoProprioUsuario() {
        Despensa item = new Despensa(usuario, farinha, 1.0, "kg");
        when(despensa.findByIdAndUsuarioId(10L, USUARIO_ID)).thenReturn(Optional.of(item));

        service.remover(10L, USUARIO_ID);

        verify(despensa).delete(item);
    }

    @Test
    void removerItemDeOutroUsuarioOuInexistenteFalha() {
        when(despensa.findByIdAndUsuarioId(10L, 99L)).thenReturn(Optional.empty());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.remover(10L, 99L));

        assertEquals("Item não encontrado na sua despensa.", erro.getMessage());
        verify(despensa, never()).delete(any(Despensa.class));
    }
}
