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
    @CsvSource({"g, ml", "kg, l", "unidade, g", "dente, unidade", "colher, ml", "xícara, ml"})
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
}
