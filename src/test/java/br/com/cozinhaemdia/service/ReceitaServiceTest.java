package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.IngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes unitários do cadastro de receitas e da leitura da lista de ingredientes. */
class ReceitaServiceTest {
    private ReceitaRepository receitas;
    private ReceitaIngredienteRepository receitaIngredientes;
    private IngredienteRepository ingredientes;
    private ReceitaService service;
    private final Usuario usuario = new Usuario("Teste", "teste@cozinha.local", "hash");

    @BeforeEach
    void preparar() {
        receitas = mock(ReceitaRepository.class);
        receitaIngredientes = mock(ReceitaIngredienteRepository.class);
        ingredientes = mock(IngredienteRepository.class);
        service = new ReceitaService(receitas, receitaIngredientes, ingredientes);
        when(receitas.save(any(Receita.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(ingredientes.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(ingredientes.save(any(Ingrediente.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void criarInterpretaCadaLinhaDeIngrediente() {
        String texto = "Ovo | 2 | unidade\n\nFarinha | 150,5 | G\r\nLeite|200|ml";

        Receita receita = service.criar(usuario, "  Bolo  ", "Simples", "Misture tudo.", 40, 8, texto);

        assertEquals("Bolo", receita.getNome());
        ArgumentCaptor<List<ReceitaIngrediente>> salvos = ArgumentCaptor.forClass(List.class);
        verify(receitaIngredientes).saveAll(salvos.capture());
        List<ReceitaIngrediente> itens = salvos.getValue();
        assertEquals(3, itens.size());
        assertEquals("Farinha", itens.get(1).getIngrediente().getNome());
        assertEquals(150.5, itens.get(1).getQuantidade(), 0.0001);
        assertEquals("g", itens.get(1).getUnidade());
        assertEquals("Leite", itens.get(2).getIngrediente().getNome());
    }

    @Test
    void criarReaproveitaIngredienteJaCadastrado() {
        Ingrediente existente = new Ingrediente("Ovo");
        when(ingredientes.findByNomeIgnoreCase("ovo")).thenReturn(Optional.of(existente));

        service.criar(usuario, "Ovo cozido", null, "Cozinhe por 10 minutos.", 10, 1, "ovo | 1 | unidade");

        verify(ingredientes, never()).save(any(Ingrediente.class));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        "Ovo 2 unidade; use Nome do ingrediente | quantidade | unidade",
        "Ovo | dois | unidade; quantidade inválida",
        "Ovo | 0 | unidade; deve ser maior que zero",
        "Ovo | 2 | ; preencha nome, quantidade e unidade",
        "Ovo | 1 | unidade\\novo | 2 | unidade; aparece mais de uma vez"
    })
    void criarRejeitaListaDeIngredientesInvalida(String texto, String trechoDaMensagem) {
        String ingredientesTexto = texto.replace("\\n", "\n");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> service.criar(usuario, "Receita", null, "Preparo", 10, 1, ingredientesTexto));

        assertTrue(erro.getMessage().contains(trechoDaMensagem), erro.getMessage());
        verify(receitas, never()).save(any(Receita.class));
    }

    @Test
    void criarExigeNomeModoDePreparoEIngredientes() {
        assertThrows(IllegalArgumentException.class, () -> service.criar(usuario, " ", null, "Preparo", 10, 1, "Ovo | 1 | unidade"));
        assertThrows(IllegalArgumentException.class, () -> service.criar(usuario, "Receita", null, "  ", 10, 1, "Ovo | 1 | unidade"));
        assertThrows(IllegalArgumentException.class, () -> service.criar(usuario, "Receita", null, "Preparo", 10, 1, "  "));
        verify(receitas, never()).save(any(Receita.class));
    }

    @Test
    void formatarExibeQuantidadesNoPadraoBrasileiro() {
        assertEquals("0", ReceitaService.formatar(null));
        assertEquals("2", ReceitaService.formatar(2.0));
        assertEquals("1,5", ReceitaService.formatar(1.5));
        assertEquals("0,25", ReceitaService.formatar(0.25));
    }
}
