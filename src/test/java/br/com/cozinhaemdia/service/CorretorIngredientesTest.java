package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.repository.IngredienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testes unitários da correção de nomes de ingredientes digitados com erro. */
class CorretorIngredientesTest {
    private IngredienteRepository repositorio;
    private CorretorIngredientes corretor;

    @BeforeEach
    void preparar() {
        repositorio = mock(IngredienteRepository.class);
        when(repositorio.findAll()).thenReturn(List.of());
        corretor = new CorretorIngredientes(repositorio);
    }

    @ParameterizedTest
    @CsvSource({
            "farna, Farinha",
            "farinah, Farinha",
            "fariha, Farinha",
            "cebloa, Cebola",
            "tomte, Tomate",
            "tomates, Tomate",
            "leitr, Leite",
            "mantega, Manteiga",
            "ovoo, Ovo",
            "azeit, Azeite",
            "arros, Arroz"
    })
    void corrigeErrosComunsDeDigitacao(String digitado, String esperado) {
        CorretorIngredientes.Correcao c = corretor.corrigir(digitado);

        assertEquals(esperado, c.corrigido());
        assertTrue(c.alterado(), digitado);
    }

    @Test
    void corrigeApenasAPalavraErradaDeUmNomeComposto() {
        assertEquals("Farinha de trigo", corretor.corrigir("farna de trigo").corrigido());
        assertEquals("Leite condensado", corretor.corrigir("Leite condesado").corrigido());
        assertEquals("Queijo ralado", corretor.corrigir("quejo ralado").corrigido());
    }

    @Test
    void nomeConhecidoNaoEhAlteradoENaoGeraAviso() {
        CorretorIngredientes.Correcao c = corretor.corrigir("farinha de trigo");

        assertEquals("Farinha de trigo", c.corrigido());
        assertFalse(c.alterado());
    }

    @Test
    void ignoraAcentosEMaiusculasUsandoAGrafiaCadastrada() {
        CorretorIngredientes.Correcao c = corretor.corrigir("ACUCAR");

        assertEquals("Açúcar", c.corrigido());
        assertFalse(c.alterado());
    }

    @Test
    void palavrasCurtasEDesconhecidasPermanecemComoDigitadas() {
        assertFalse(corretor.corrigir("xyz").alterado());
        assertFalse(corretor.corrigir("Pitaya").alterado());
        assertFalse(corretor.corrigir("Gelatina").alterado());
        assertEquals("Gelatina", corretor.corrigir("Gelatina").corrigido());
    }

    @Test
    void entradaVaziaOuNulaNaoQuebra() {
        assertEquals("", corretor.corrigir(null).corrigido());
        assertEquals("", corretor.corrigir("   ").corrigido());
    }

    @Test
    void preferePalavraMaisFrequenteQuandoHaEmpate() {
        // "farna" está a duas edições de "farinha" e de "farofa"; "farinha" aparece em mais ingredientes.
        assertEquals("Farinha", corretor.corrigir("farna").corrigido());
    }

    @Test
    void ingredientesCadastradosTemPrioridadeEEntramNoVocabulario() {
        when(repositorio.findAll()).thenReturn(List.of(new Ingrediente("Tahine"), new Ingrediente("Cúrcuma da casa")));

        assertEquals("Tahine", corretor.corrigir("tahnie").corrigido());
        assertEquals("Cúrcuma da casa", corretor.corrigir("curcuma da casa").corrigido());
        assertTrue(corretor.nomesConhecidos().contains("Tahine"));
    }

    @Test
    void corrigirLinhasCorrigeSoONomeEPreservaQuantidadeEUnidade() {
        String texto = "farna | 200 | g\nOvo | 2 | unidade\n\nlete | 1,5 | l";

        CorretorIngredientes.TextoCorrigido resultado = corretor.corrigirLinhas(texto);

        assertEquals("Farinha | 200 | g\nOvo | 2 | unidade\n\nLeite | 1,5 | l", resultado.texto());
        assertEquals(2, resultado.avisos().size());
        assertTrue(resultado.avisos().get(0).contains("farna"));
        assertTrue(resultado.avisos().get(0).contains("Farinha"));
    }

    @Test
    void corrigirLinhasNaoMexeEmLinhasForaDoFormato() {
        CorretorIngredientes.TextoCorrigido resultado = corretor.corrigirLinhas("farna 200 g");

        assertEquals("farna 200 g", resultado.texto());
        assertTrue(resultado.avisos().isEmpty());
    }

    @Test
    void sugerirOrdenaPorPrefixoDepoisPorConteudoEDepoisPorSemelhanca() {
        List<String> prefixo = corretor.sugerir("farin", 10);
        assertTrue(prefixo.stream().allMatch(n -> n.startsWith("Farinha")), prefixo.toString());

        List<String> conteudo = corretor.sugerir("trigo", 10);
        assertTrue(conteudo.contains("Farinha de trigo"));

        List<String> parecidos = corretor.sugerir("farna", 10);
        assertTrue(parecidos.contains("Farinha de trigo"), parecidos.toString());

        assertEquals(3, corretor.sugerir("", 3).size());
    }

    @Test
    void buscaToleranteAceitaErrosDeDigitacaoEPalavrasParciais() {
        assertTrue(CorretorIngredientes.correspondeBusca("bolo de farna", "Bolo de farinha de trigo"));
        assertTrue(CorretorIngredientes.correspondeBusca("omelte", "Omelete de queijo"));
        assertTrue(CorretorIngredientes.correspondeBusca("queijo", "Omelete de queijo"));
        assertTrue(CorretorIngredientes.correspondeBusca("", "Qualquer coisa"));
        assertFalse(CorretorIngredientes.correspondeBusca("pizza", "Omelete de queijo"));
        assertFalse(CorretorIngredientes.correspondeBusca("ovo", "Omelete de queijo"));
    }

    @Test
    void distanciaConsideraTrocaDeLetrasVizinhas() {
        assertEquals(1, CorretorIngredientes.distancia("tomate", "tomtae"));
        assertEquals(1, CorretorIngredientes.distancia("arroz", "arorz"));
        assertEquals(2, CorretorIngredientes.distancia("farna", "farinha"));
        assertEquals(0, CorretorIngredientes.distancia("sal", "sal"));
    }

    @Test
    void normalizarRemoveAcentosEEspacosRepetidos() {
        assertEquals("acucar mascavo", CorretorIngredientes.normalizar("  Açúcar   Mascavo "));
        assertEquals("alho poro", CorretorIngredientes.normalizar("Alho-poró"));
    }
}
