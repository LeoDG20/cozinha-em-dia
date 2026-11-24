package br.com.cozinhaemdia.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da leitura de links do YouTube. Só o identificador de 11 caracteres pode chegar ao player. */
class YoutubeUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "http://youtube.com/watch?v=dQw4w9WgXcQ&t=42s",
            "https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "youtube.com/watch?v=dQw4w9WgXcQ",
            "  https://youtu.be/dQw4w9WgXcQ  "
    })
    void extraiOIdentificadorDeLinksValidos(String link) {
        assertEquals("dQw4w9WgXcQ", YoutubeUtil.extrairId(link).orElseThrow(), link);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "https://vimeo.com/123456789",
            "https://evil.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=curto",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ\"><script>alert(1)</script>",
            "javascript:alert(1)",
            "https://www.youtube.com/"
    })
    void recusaLinksInvalidosOuPerigosos(String link) {
        assertTrue(YoutubeUtil.extrairId(link).isEmpty(), String.valueOf(link));
    }

    @Test
    void enderecoIncorporadoUsaODominioSemCookies() {
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ", YoutubeUtil.enderecoIncorporado("dQw4w9WgXcQ"));
    }
}
