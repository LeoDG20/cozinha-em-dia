package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.DisponibilidadeService;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DisponibilidadeController {
    private final DisponibilidadeService service;
    public DisponibilidadeController(DisponibilidadeService service) { this.service=service; }
    @GetMapping("/o-que-posso-fazer") public String analisar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        List<DisponibilidadeService.AnaliseReceita> analises=service.analisar(auth.getId());
        model.addAttribute("analises", analises);
        model.addAttribute("disponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.DISPONIVEL).count());
        model.addAttribute("parciais", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.PARCIAL).count());
        model.addAttribute("indisponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.INDISPONIVEL).count());
        return "disponibilidade";
    }
}
