package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.repository.*;
import br.com.cozinhaemdia.service.DisponibilidadeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DashboardController {
    private final ReceitaRepository receitas; private final DespensaRepository despensa; private final DisponibilidadeService disponibilidade;
    public DashboardController(ReceitaRepository receitas, DespensaRepository despensa, DisponibilidadeService disponibilidade) { this.receitas=receitas; this.despensa=despensa; this.disponibilidade=disponibilidade; }
    @GetMapping({"/", "/inicio"}) public String inicio(HttpSession session, Model model) {
        Long id=(Long)session.getAttribute("usuarioId");
        List<DisponibilidadeService.AnaliseReceita> analises=disponibilidade.analisar(id);
        model.addAttribute("nome", session.getAttribute("usuarioNome"));
        model.addAttribute("qtdReceitas", receitas.countByUsuarioId(id));
        model.addAttribute("qtdItens", despensa.countByUsuarioId(id));
        model.addAttribute("qtdDisponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.DISPONIVEL).count());
        model.addAttribute("qtdParciais", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.PARCIAL).count());
        model.addAttribute("destaques", analises.stream().limit(3).toList());
        return "dashboard";
    }
}
