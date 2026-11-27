package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.config.Mensagens;
import br.com.cozinhaemdia.model.Amizade;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.AmizadeService;
import br.com.cozinhaemdia.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Amigos: enviar pedidos, aceitar ou recusar, e desfazer amizades. */
@Controller
public class AmigosController {
    private final AmizadeService amizades;
    private final UsuarioService usuarios;
    private final Mensagens mensagens;

    @Autowired
    public AmigosController(AmizadeService amizades, UsuarioService usuarios, Mensagens mensagens) {
        this.amizades = amizades; this.usuarios = usuarios; this.mensagens = mensagens;
    }

    @GetMapping("/amigos")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        model.addAttribute("amigos", amizades.amigos(auth.getId()));
        model.addAttribute("recebidos", amizades.pedidosRecebidos(auth.getId()));
        model.addAttribute("enviados", amizades.pedidosEnviados(auth.getId()));
        return "amigos";
    }

    @PostMapping("/amigos")
    public String convidar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam String email, RedirectAttributes flash) {
        try {
            Amizade a = amizades.convidar(usuarios.buscar(auth.getId()), email);
            flash.addFlashAttribute("sucesso", a.isAceita()
                    ? mensagens.get("msg.amigos.pedido-cruzado", a.outro(auth.getId()).getNome())
                    : mensagens.get("msg.amigos.pedido-enviado", a.getDestinatario().getNome()));
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("erro", mensagens.de(ex));
        }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/aceitar")
    public String aceitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try {
            Amizade a = amizades.aceitar(id, auth.getId());
            flash.addFlashAttribute("sucesso", mensagens.get("msg.amigos.aceito", a.getSolicitante().getNome()));
        } catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", mensagens.de(ex)); }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/recusar")
    public String recusar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try { amizades.recusarOuCancelar(id, auth.getId()); flash.addFlashAttribute("sucesso", mensagens.get("msg.amigos.pedido-removido")); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", mensagens.de(ex)); }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/desfazer")
    public String desfazer(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try { amizades.desfazer(id, auth.getId()); flash.addFlashAttribute("sucesso", mensagens.get("msg.amigos.desfeita")); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", mensagens.de(ex)); }
        return "redirect:/amigos";
    }
}
