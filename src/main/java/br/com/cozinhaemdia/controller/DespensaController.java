package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.DespensaService;
import br.com.cozinhaemdia.service.ReceitaService;
import br.com.cozinhaemdia.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DespensaController {
    private static final List<String> UNIDADES = List.of("g", "kg", "ml", "l", "unidade", "dente", "colher", "colher de chá", "xícara");

    private final DespensaService despensaService;
    private final UsuarioService usuarios;
    private final CorretorIngredientes corretor;
    private final ReceitaService receitas;

    @Autowired
    public DespensaController(DespensaService despensaService, UsuarioService usuarios, CorretorIngredientes corretor, ReceitaService receitas) {
        this.despensaService = despensaService; this.usuarios = usuarios; this.corretor = corretor; this.receitas = receitas;
    }

    @GetMapping("/despensa")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        model.addAttribute("itens", despensaService.listar(auth.getId()));
        model.addAttribute("unidades", UNIDADES);
        model.addAttribute("sugestoes", corretor.nomesConhecidos());
        model.addAttribute("uso", receitas.usoPorIngrediente(auth.getId()));
        return "despensa";
    }

    @PostMapping("/despensa")
    public String adicionar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam String nome, @RequestParam Double quantidade,
                            @RequestParam String unidade, RedirectAttributes flash) {
        CorretorIngredientes.Correcao correcao = corretor.corrigir(nome);
        try { despensaService.adicionar(usuarios.buscar(auth.getId()), correcao.corrigido(), quantidade, unidade); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/despensa"; }
        flash.addFlashAttribute("sucesso", correcao.alterado()
            ? "Ingrediente adicionado à despensa. Corrigimos \"" + correcao.original() + "\" para \"" + correcao.corrigido() + "\"."
            : "Ingrediente adicionado à despensa.");
        return "redirect:/despensa";
    }

    @PostMapping("/despensa/editar")
    public String editar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, @RequestParam Double quantidade,
                         @RequestParam String unidade, RedirectAttributes flash) {
        try { despensaService.atualizar(itemId, auth.getId(), quantidade, unidade); flash.addFlashAttribute("sucesso", "Ingrediente atualizado."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }

    @PostMapping("/despensa/remover")
    public String remover(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, RedirectAttributes flash) {
        try { despensaService.remover(itemId, auth.getId()); flash.addFlashAttribute("sucesso", "Ingrediente removido."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }
}
