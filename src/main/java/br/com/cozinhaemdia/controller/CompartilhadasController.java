package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.config.Mensagens;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CompartilhamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Receitas que outros usuários compartilharam com a pessoa autenticada. */
@Controller
public class CompartilhadasController {
    private final CompartilhamentoService compartilhamentos;
    private final Mensagens mensagens;

    @Autowired
    public CompartilhadasController(CompartilhamentoService compartilhamentos, Mensagens mensagens) { this.compartilhamentos = compartilhamentos; this.mensagens = mensagens; }

    @GetMapping("/compartilhadas")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        model.addAttribute("recebidas", compartilhamentos.recebidas(auth.getId()));
        return "compartilhadas";
    }

    /** Desfaz um compartilhamento: o destinatário dispensa a receita ou o dono deixa de compartilhá-la. */
    @PostMapping("/compartilhamentos/{id}/remover")
    public String remover(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth,
                          @RequestParam(required = false) Long receita, RedirectAttributes flash) {
        if (compartilhamentos.remover(id, auth.getId())) flash.addFlashAttribute("sucesso", mensagens.get("msg.compartilhamento.removido"));
        else flash.addFlashAttribute("erro", mensagens.get("erro.compartilhamento.nao-encontrado"));
        return receita != null ? "redirect:/receitas/" + receita : "redirect:/compartilhadas";
    }
}
