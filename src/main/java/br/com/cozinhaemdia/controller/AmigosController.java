package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Amizade;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.AmizadeService;
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
    private final UsuarioRepository usuarios;

    public AmigosController(AmizadeService amizades, UsuarioRepository usuarios) { this.amizades = amizades; this.usuarios = usuarios; }

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
            Amizade a = amizades.convidar(usuarios.findById(auth.getId()).orElseThrow(), email);
            flash.addFlashAttribute("sucesso", a.isAceita()
                    ? "Vocês agora são amigos: " + a.outro(auth.getId()).getNome() + " já tinha enviado um pedido."
                    : "Pedido enviado para " + a.getDestinatario().getNome() + ". A amizade começa quando a pessoa aceitar.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/aceitar")
    public String aceitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try {
            Amizade a = amizades.aceitar(id, auth.getId());
            flash.addFlashAttribute("sucesso", "Agora você e " + a.getSolicitante().getNome() + " são amigos.");
        } catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/recusar")
    public String recusar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try { amizades.recusarOuCancelar(id, auth.getId()); flash.addFlashAttribute("sucesso", "Pedido removido."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/amigos";
    }

    @PostMapping("/amigos/{id}/desfazer")
    public String desfazer(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try { amizades.desfazer(id, auth.getId()); flash.addFlashAttribute("sucesso", "Amizade desfeita."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/amigos";
    }
}
