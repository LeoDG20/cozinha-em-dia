package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.DisponibilidadeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DisponibilidadeController {
    private final DisponibilidadeService service;
    public DisponibilidadeController(DisponibilidadeService service) { this.service=service; }
    @GetMapping("/o-que-posso-fazer") public String analisar(HttpSession session, Model model) {
        List<DisponibilidadeService.AnaliseReceita> analises=service.analisar((Long)session.getAttribute("usuarioId"));
        model.addAttribute("analises", analises);
        model.addAttribute("disponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.DISPONIVEL).count());
        model.addAttribute("parciais", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.PARCIAL).count());
        model.addAttribute("indisponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.INDISPONIVEL).count());
        return "disponibilidade";
    }
}
