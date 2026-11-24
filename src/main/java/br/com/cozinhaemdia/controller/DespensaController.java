package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.DespensaService;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DespensaController {
    private static final java.util.List<String> UNIDADES = java.util.List.of("g", "kg", "ml", "l", "unidade", "dente", "colher", "xícara");
    private final DespensaService despensaService; private final UsuarioRepository usuarios; private final CorretorIngredientes corretor;
    public DespensaController(DespensaService despensaService, UsuarioRepository usuarios, CorretorIngredientes corretor) { this.despensaService=despensaService; this.usuarios=usuarios; this.corretor=corretor; }
    @GetMapping("/despensa") public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) { model.addAttribute("itens", despensaService.listar(auth.getId())); model.addAttribute("unidades", UNIDADES); model.addAttribute("sugestoes", corretor.nomesConhecidos()); return "despensa"; }
    @PostMapping("/despensa") public String adicionar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam String nome, @RequestParam Double quantidade, @RequestParam String unidade, RedirectAttributes flash) {
        CorretorIngredientes.Correcao correcao = corretor.corrigir(nome);
        try { despensaService.adicionar(usuarios.findById(auth.getId()).orElseThrow(), correcao.corrigido(), quantidade, unidade); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/despensa"; }
        flash.addFlashAttribute("sucesso", correcao.alterado()
            ? "Ingrediente adicionado à despensa. Corrigimos \"" + correcao.original() + "\" para \"" + correcao.corrigido() + "\"."
            : "Ingrediente adicionado à despensa.");
        return "redirect:/despensa";
    }
    @PostMapping("/despensa/editar") public String editar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, @RequestParam Double quantidade, @RequestParam String unidade, RedirectAttributes flash) {
        try { despensaService.atualizar(itemId, auth.getId(), quantidade, unidade); flash.addFlashAttribute("sucesso", "Ingrediente atualizado."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }
    @PostMapping("/despensa/remover") public String remover(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, RedirectAttributes flash) {
        try { despensaService.remover(itemId, auth.getId()); flash.addFlashAttribute("sucesso", "Ingrediente removido."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }
}
