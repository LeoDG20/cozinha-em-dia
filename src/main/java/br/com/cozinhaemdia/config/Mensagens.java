package br.com.cozinhaemdia.config;

import br.com.cozinhaemdia.service.NegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Traduz mensagens e exceções de negócio para o idioma da requisição atual. */
@Component
public class Mensagens {
    private final MessageSource origem;

    @Autowired
    public Mensagens(MessageSource origem) { this.origem = origem; }

    /** Mensagem pelo código, no idioma atual. */
    public String get(String codigo, Object... argumentos) {
        return origem.getMessage(codigo, argumentos, LocaleContextHolder.getLocale());
    }

    /** Nome da unidade (guardada sempre em português) no idioma atual; sem tradução, devolve a própria unidade. */
    public String unidade(String unidade) {
        if (unidade == null) return "";
        String chave = "unidade." + java.text.Normalizer.normalize(unidade.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace(' ', '-');
        return origem.getMessage(chave, null, unidade, LocaleContextHolder.getLocale());
    }

    /** Texto de uma exceção: traduzido quando ela traz um código, o texto original nos demais casos. */
    public String de(IllegalArgumentException ex) {
        if (ex instanceof NegocioException n)
            return origem.getMessage(n.getCodigo(), n.getArgumentos(), ex.getMessage(), LocaleContextHolder.getLocale());
        return ex.getMessage();
    }
}
