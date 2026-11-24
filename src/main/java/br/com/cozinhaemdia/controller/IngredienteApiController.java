package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.CorretorIngredientes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Respostas em JSON usadas pelos formulários para sugerir e corrigir nomes de ingredientes enquanto o usuário digita. */
@RestController
@RequestMapping("/api/ingredientes")
public class IngredienteApiController {
    private final CorretorIngredientes corretor;

    public IngredienteApiController(CorretorIngredientes corretor) { this.corretor = corretor; }

    @GetMapping("/corrigir")
    public CorretorIngredientes.Correcao corrigir(@RequestParam(defaultValue = "") String nome) {
        return corretor.corrigir(nome);
    }

    @GetMapping("/sugestoes")
    public List<String> sugestoes(@RequestParam(name = "q", defaultValue = "") String termo) {
        return corretor.sugerir(termo, 8);
    }
}
