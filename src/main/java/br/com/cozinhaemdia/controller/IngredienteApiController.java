package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.CorretorIngredientes;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public IngredienteApiController(CorretorIngredientes corretor) { this.corretor = corretor; }

    @GetMapping("/corrigir")
    public CorretorIngredientes.Correcao corrigir(@RequestParam(defaultValue = "") String nome) {
        return corretor.corrigir(nome);
    }

    /** Indica se o nome é conhecido e, se não for, quais nomes parecidos podem ser o que a pessoa quis digitar. */
    @GetMapping("/verificar")
    public CorretorIngredientes.Verificacao verificar(@RequestParam(defaultValue = "") String nome) {
        return corretor.verificar(nome);
    }

    @GetMapping("/sugestoes")
    public List<String> sugestoes(@RequestParam(name = "q", defaultValue = "") String termo) {
        return corretor.sugerir(termo, 8);
    }
}
