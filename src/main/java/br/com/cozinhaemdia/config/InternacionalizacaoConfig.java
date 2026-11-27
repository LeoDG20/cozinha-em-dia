package br.com.cozinhaemdia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Internacionalização: português do Brasil (padrão) e inglês.
 * O idioma é trocado com o parâmetro ?lang=pt-BR ou ?lang=en e fica guardado em um cookie.
 */
@Configuration
public class InternacionalizacaoConfig implements WebMvcConfigurer {
    public static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");
    public static final Locale INGLES = Locale.ENGLISH;
    public static final List<Locale> IDIOMAS = List.of(PORTUGUES, INGLES);

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("idioma");
        resolver.setDefaultLocale(PORTUGUES);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookiePath("/");
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
