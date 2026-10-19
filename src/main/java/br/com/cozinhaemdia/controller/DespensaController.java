package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.service.DespensaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DespensaController {
    private final DespensaService despensaService; private final UsuarioRepository usuarios;
    public DespensaController(DespensaService despensaService, UsuarioRepository usuarios) { this.despensaService=despensaService; this.usuarios=usuarios; }
    @GetMapping("/despensa") public String listar(HttpSession session, Model model) { model.addAttribute("itens", despensaService.listar(uid(session))); return "despensa"; }
    @PostMapping("/despensa") public String adicionar(HttpSession session, @RequestParam String nome, @RequestParam Double quantidade, @RequestParam String unidade, RedirectAttributes flash) {
        try { despensaService.adicionar(usuarios.findById(uid(session)).orElseThrow(), nome, quantidade, unidade); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/despensa"; }
        flash.addFlashAttribute("sucesso", "Ingrediente adicionado à despensa."); return "redirect:/despensa";
    }
    @PostMapping("/despensa/remover") public String remover(HttpSession session, @RequestParam Long itemId, RedirectAttributes flash) {
        try { despensaService.remover(itemId, uid(session)); flash.addFlashAttribute("sucesso", "Ingrediente removido."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }
    private Long uid(HttpSession s) { return (Long)s.getAttribute("usuarioId"); }
}
