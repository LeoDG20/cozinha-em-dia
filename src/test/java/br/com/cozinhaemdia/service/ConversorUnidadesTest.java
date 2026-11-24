package br.com.cozinhaemdia.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes unitários da conversão entre unidades de massa e de volume. */
class ConversorUnidadesTest {

    @ParameterizedTest
    @CsvSource({
            "1, kg, g, 1000",
            "250, g, kg, 0.25",
            "1.5, l, ml, 1500",
            "300, ml, l, 0.3",
            "2, kg, mg, 2000000",
            "5, cl, ml, 50",
            "3, g, g, 3",
            "2, unidade, unidade, 2"
    })
    void converteEntreUnidadesDaMesmaGrandeza(double valor, String origem, String destino, double esperado) {
        assertEquals(esperado, ConversorUnidades.converter(valor, origem, destino).orElseThrow(), 0.000001);
    }

    @ParameterizedTest
    @CsvSource({"g, ml", "kg, l", "unidade, g", "dente, unidade", "unidade, ml", "xícara, g", "colher, kg"})
    void naoConverteEntreGrandezasDiferentes(String origem, String destino) {
        assertTrue(ConversorUnidades.converter(1, origem, destino).isEmpty());
        assertFalse(ConversorUnidades.saoCompativeis(origem, destino));
    }

    @Test
    void ignoraMaiusculasEEspacos() {
        assertEquals(1000, ConversorUnidades.converter(1, " KG ", "G").orElseThrow(), 0.000001);
    }

    @Test
    void reconheceSinonimosComuns() {
        assertEquals("g", ConversorUnidades.normalizar("Gramas"));
        assertEquals("kg", ConversorUnidades.normalizar("quilo"));
        assertEquals("l", ConversorUnidades.normalizar("Litros"));
        assertEquals("unidade", ConversorUnidades.normalizar("unid"));
        assertEquals(2000, ConversorUnidades.converter(2, "quilos", "gramas").orElseThrow(), 0.000001);
    }

    @Test
    void unidadesVaziasOuNulasNaoSaoCompativeis() {
        assertTrue(ConversorUnidades.converter(1, null, "g").isEmpty());
        assertTrue(ConversorUnidades.converter(1, "g", " ").isEmpty());
        assertEquals("", ConversorUnidades.normalizar(null));
    }

    @Test
    void unidadeDesconhecidaSoEhCompativelComEla() {
        assertTrue(ConversorUnidades.saoCompativeis("pitada", "pitada"));
        assertFalse(ConversorUnidades.saoCompativeis("pitada", "g"));
    }

    @ParameterizedTest
    @CsvSource({
            "1, xícara, ml, 240",
            "2, xícara, ml, 480",
            "1, colher, ml, 15",
            "1, colher de chá, ml, 5",
            "3, colher de chá, colher, 1",
            "1, l, xícara, 4.1666667",
            "500, ml, xícara, 2.0833333"
    })
    void converteMedidasCaseirasParaVolume(double valor, String origem, String destino, double esperado) {
        assertEquals(esperado, ConversorUnidades.converter(valor, origem, destino).orElseThrow(), 0.0001);
    }

    @Test
    void reconheceVariacoesDeEscritaDasMedidasCaseiras() {
        assertEquals("colher", ConversorUnidades.normalizar("Colher de sopa"));
        assertEquals("colher", ConversorUnidades.normalizar("colheres de sopa"));
        assertEquals("colher de chá", ConversorUnidades.normalizar("colher de cha"));
        assertEquals("colher de chá", ConversorUnidades.normalizar("  Colheres de chá "));
        assertEquals("xícara", ConversorUnidades.normalizar("xicaras"));
    }

    @Test
    void convertePesoEmVolumePelaDensidadeDoIngrediente() {
        assertEquals(240, ConversorUnidades.converter(2, "xícara", "g", "Farinha de trigo").orElseThrow(), 0.0001);
        assertEquals(2, ConversorUnidades.converter(240, "g", "xícara", "farinha de trigo").orElseThrow(), 0.0001);
        assertEquals(0.18, ConversorUnidades.converter(1, "xícara", "kg", "Açúcar").orElseThrow(), 0.0001);
        assertEquals(240, ConversorUnidades.converter(240, "ml", "g", "Água").orElseThrow(), 0.0001);
        assertEquals(7.5, ConversorUnidades.converter(1, "colher", "g", "Farinha").orElseThrow(), 0.0001);
    }

    @Test
    void semDensidadeConhecidaNaoConvertePesoEmVolume() {
        assertTrue(ConversorUnidades.converter(1, "xícara", "g", "Ingrediente desconhecido").isEmpty());
        assertTrue(ConversorUnidades.converter(1, "xícara", "g", "").isEmpty());
        assertTrue(ConversorUnidades.converter(1, "xícara", "g", null).isEmpty());
        assertTrue(ConversorUnidades.converter(1, "unidade", "g", "Farinha").isEmpty());
    }

    @Test
    void conversaoDentroDaMesmaGrandezaIgnoraOIngrediente() {
        assertEquals(1000, ConversorUnidades.converter(1, "kg", "g", "Ingrediente desconhecido").orElseThrow(), 0.0001);
    }

    @Test
    void densidadeUsaPalavraInteiraEIgnoraAcentos() {
        assertTrue(ConversorUnidades.temDensidade("Óleo"));
        assertTrue(ConversorUnidades.temDensidade("Açúcar mascavo"));
        assertTrue(ConversorUnidades.temDensidade("Sal grosso"));
        assertFalse(ConversorUnidades.temDensidade("Salsa"));
        assertFalse(ConversorUnidades.temDensidade("Melancia"));
        assertEquals(0.72, ConversorUnidades.densidade("Açúcar mascavo").orElseThrow(), 0.0001);
    }
}
