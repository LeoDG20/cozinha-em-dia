package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.config.Mensagens;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.ComentarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Comentários nas receitas próprias e nas compartilhadas com o usuário. */
@Controller
public class ComentarioController {
    private final ComentarioService comentarios;
    private final Mensagens mensagens;

    @Autowired
    public ComentarioController(ComentarioService comentarios, Mensagens mensagens) { this.comentarios = comentarios; this.mensagens = mensagens; }

    @PostMapping("/receitas/{id}/comentarios")
    public String comentar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth,
                           @RequestParam String texto, RedirectAttributes flash) {
        try { comentarios.comentar(id, auth.getId(), texto); flash.addFlashAttribute("sucesso", mensagens.get("msg.comentario.publicado")); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", mensagens.de(ex)); }
        return "redirect:/receitas/" + id + "#comentarios";
    }

    @PostMapping("/receitas/{id}/comentarios/{comentarioId}/excluir")
    public String excluir(@PathVariable Long id, @PathVariable Long comentarioId, @AuthenticationPrincipal UsuarioAutenticado auth,
                          RedirectAttributes flash) {
        if (comentarios.excluir(comentarioId, id, auth.getId())) flash.addFlashAttribute("sucesso", mensagens.get("msg.comentario.excluido"));
        else flash.addFlashAttribute("erro", mensagens.get("erro.comentario.nao-encontrado"));
        return "redirect:/receitas/" + id + "#comentarios";
    }
}
