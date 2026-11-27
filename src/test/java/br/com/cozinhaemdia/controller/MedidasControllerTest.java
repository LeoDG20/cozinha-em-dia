package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.config.Mensagens;
import br.com.cozinhaemdia.controller.MedidasController.Resultado;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Textos e validações do conversor de medidas. */
class MedidasControllerTest {
    private static final Mensagens MENSAGENS = mensagens();

    private static Mensagens mensagens() {
        ResourceBundleMessageSource origem = new ResourceBundleMessageSource();
        origem.setBasename("messages");
        origem.setDefaultEncoding("UTF-8");
        origem.setAlwaysUseMessageFormat(true);
        origem.setFallbackToSystemLocale(false);
        return new Mensagens(origem);
    }

    @Test
    void convertePesoEVolumeCaseiroComIngrediente() {
        Resultado r = MedidasController.converter("2", "xícara", "g", "Farinha de trigo", MENSAGENS);

        assertTrue(r.ok());
        assertEquals("2 xícara de Farinha de trigo = 240 g", r.texto());
        assertNotNull(r.aviso(), "conversão por densidade deve avisar que é aproximada");
    }

    @Test
    void conversaoDeVolumeNaoPrecisaDeIngredienteENaoAvisa() {
        Resultado r = MedidasController.converter("1", "xícara", "ml", "", MENSAGENS);

        assertTrue(r.ok());
        assertEquals("1 xícara = 240 ml", r.texto());
        assertNull(r.aviso());
    }

    @Test
    void aceitaVirgulaComoSeparadorDecimal() {
        Resultado r = MedidasController.converter("1,5", "l", "ml", null, MENSAGENS);

        assertTrue(r.ok());
        assertEquals("1,5 l = 1500 ml", r.texto());
    }

    @Test
    void pedeOIngredienteParaTrocarPesoPorVolume() {
        Resultado r = MedidasController.converter("1", "xícara", "g", "", MENSAGENS);

        assertFalse(r.ok());
        assertTrue(r.aviso().contains("informe o ingrediente"));
    }

    @Test
    void avisaQuandoOIngredienteNaoTemDensidadeConhecida() {
        Resultado r = MedidasController.converter("1", "xícara", "g", "Ingrediente raro", MENSAGENS);

        assertFalse(r.ok());
        assertTrue(r.aviso().contains("Ingrediente raro"));
    }

    @Test
    void recusaValorInvalido() {
        assertFalse(MedidasController.converter("abc", "g", "kg", "", MENSAGENS).ok());
        assertFalse(MedidasController.converter("0", "g", "kg", "", MENSAGENS).ok());
        assertFalse(MedidasController.converter("-3", "g", "kg", "", MENSAGENS).ok());
    }

    @Test
    void arredondaResultadosPequenosComDuasCasas() {
        assertEquals("1 colher de chá = 0,33 colher", MedidasController.converter("1", "colher de chá", "colher", "", MENSAGENS).texto());
    }
}
