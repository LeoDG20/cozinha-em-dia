package br.com.cozinhaemdia.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai o identificador de vídeo de links do YouTube. Somente o identificador (11 caracteres seguros)
 * é usado para montar o player, o que impede a inserção de endereços arbitrários na página.
 */
public final class YoutubeUtil {
    private static final Pattern COMPLETO = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.|music\\.)?(?:youtube\\.com|youtube-nocookie\\.com)/(?:watch\\?(?:[^#]*&)?v=|embed/|shorts/|live/|v/)([A-Za-z0-9_-]{11})(?:[&?#/].*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CURTO = Pattern.compile(
            "^(?:https?://)?youtu\\.be/([A-Za-z0-9_-]{11})(?:[?&#/].*)?$", Pattern.CASE_INSENSITIVE);

    private YoutubeUtil() {}

    /** Identificador do vídeo, ou vazio se o texto não for um link válido do YouTube. */
    public static Optional<String> extrairId(String link) {
        if (link == null) return Optional.empty();
        String texto = link.trim();
        if (texto.isEmpty() || texto.length() > 300) return Optional.empty();
        Matcher m = COMPLETO.matcher(texto);
        if (m.matches()) return Optional.of(m.group(1));
        m = CURTO.matcher(texto);
        if (m.matches()) return Optional.of(m.group(1));
        return Optional.empty();
    }

    /** Endereço do player incorporado, sem cookies de rastreamento. */
    public static String enderecoIncorporado(String id) {
        return "https://www.youtube-nocookie.com/embed/" + id;
    }
}
