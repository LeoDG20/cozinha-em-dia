package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.service.ReceitaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReceitaController {
    private final ReceitaService receitaService; private final UsuarioRepository usuarios;
    public ReceitaController(ReceitaService receitaService, UsuarioRepository usuarios) { this.receitaService=receitaService; this.usuarios=usuarios; }
    @GetMapping("/receitas") public String listar(HttpSession session, Model model) { model.addAttribute("receitas", receitaService.listar(uid(session))); return "receitas"; }
    @GetMapping("/receitas/nova") public String nova(Model model) { model.addAttribute("edicao", false); model.addAttribute("formAction", "/receitas"); model.addAttribute("ingredientesTexto", ""); return "receita-form"; }
    @PostMapping("/receitas") public String criar(HttpSession session, @RequestParam String nome, @RequestParam(required=false) String descricao, @RequestParam String modoPreparo, @RequestParam(required=false) Integer tempoPreparoMin, @RequestParam(required=false) Integer porcoes, @RequestParam String ingredientesTexto, RedirectAttributes flash) {
        try { receitaService.criar(usuario(session), nome, descricao, modoPreparo, tempoPreparoMin, porcoes, ingredientesTexto); flash.addFlashAttribute("sucesso", "Receita cadastrada com sucesso."); return "redirect:/receitas"; }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/receitas/nova"; }
    }
    @GetMapping("/receitas/{id}") public String detalhe(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes flash) {
        Receita receita=receitaService.buscar(id, uid(session)).orElse(null);
        if (receita==null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        model.addAttribute("receita", receita); model.addAttribute("itens", receitaService.ingredientesDaReceita(id)); return "receita-detalhe";
    }
    @GetMapping("/receitas/{id}/editar") public String editar(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes flash) {
        Receita receita=receitaService.buscar(id, uid(session)).orElse(null);
        if (receita==null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        model.addAttribute("receita", receita); model.addAttribute("edicao", true); model.addAttribute("formAction", "/receitas/"+id+"/editar"); model.addAttribute("ingredientesTexto", receitaService.textoIngredientes(receita));
        return "receita-form";
    }
    @PostMapping("/receitas/{id}/editar") public String atualizar(@PathVariable Long id, HttpSession session, @RequestParam String nome, @RequestParam(required=false) String descricao, @RequestParam String modoPreparo, @RequestParam(required=false) Integer tempoPreparoMin, @RequestParam(required=false) Integer porcoes, @RequestParam String ingredientesTexto, RedirectAttributes flash) {
        Receita receita=receitaService.buscar(id, uid(session)).orElse(null);
        if (receita==null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        try { receitaService.atualizar(receita, usuario(session), nome, descricao, modoPreparo, tempoPreparoMin, porcoes, ingredientesTexto); flash.addFlashAttribute("sucesso", "Receita atualizada."); return "redirect:/receitas/"+id; }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/receitas/"+id+"/editar"; }
    }
    @PostMapping("/receitas/{id}/excluir") public String excluir(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        Receita receita=receitaService.buscar(id, uid(session)).orElse(null);
        if (receita!=null) { receitaService.excluir(receita); flash.addFlashAttribute("sucesso", "Receita excluída."); }
        return "redirect:/receitas";
    }
    private Long uid(HttpSession s) { return (Long)s.getAttribute("usuarioId"); }
    private Usuario usuario(HttpSession s) { return usuarios.findById(uid(s)).orElseThrow(); }
}
