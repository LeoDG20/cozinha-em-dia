package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.ArmazenamentoArquivos;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;

/**
 * Entrega as imagens e os vídeos enviados. O acesso exige usuário autenticado, o nome do arquivo é um
 * UUID gerado pelo sistema e a resposta aceita requisições parciais (Range), necessárias para avançar no vídeo.
 */
@Controller
public class MidiaController {
    private final ArmazenamentoArquivos arquivos;

    public MidiaController(ArmazenamentoArquivos arquivos) { this.arquivos = arquivos; }

    @GetMapping("/midia/{nome:.+}")
    public ResponseEntity<Resource> arquivo(@PathVariable String nome) {
        return arquivos.carregar(nome)
                .map(recurso -> ResponseEntity.ok()
                        .contentType(arquivos.tipo(nome))
                        .header("X-Content-Type-Options", "nosniff")
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                        .body(recurso))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
