package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.DespensaService;
import br.com.cozinhaemdia.service.DisponibilidadeService;
import br.com.cozinhaemdia.service.ReceitaService;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DashboardController {
    private final ReceitaService receitas;
    private final DespensaService despensa;
    private final DisponibilidadeService disponibilidade;

    @Autowired
    public DashboardController(ReceitaService receitas, DespensaService despensa, DisponibilidadeService disponibilidade) {
        this.receitas = receitas; this.despensa = despensa; this.disponibilidade = disponibilidade;
    }

    @GetMapping({"/", "/inicio"}) public String inicio(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        Long id=auth.getId();
        List<DisponibilidadeService.AnaliseReceita> analises=disponibilidade.analisar(id);
        model.addAttribute("nome", auth.getNome());
        model.addAttribute("qtdReceitas", receitas.contar(id));
        model.addAttribute("qtdItens", despensa.contar(id));
        model.addAttribute("qtdDisponiveis", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.DISPONIVEL).count());
        model.addAttribute("qtdParciais", analises.stream().filter(a -> a.getStatus()==DisponibilidadeService.StatusDisponibilidade.PARCIAL).count());
        model.addAttribute("destaques", analises.stream().sorted(java.util.Comparator.comparing(DisponibilidadeService.AnaliseReceita::getStatus)).limit(6).toList());
        return "dashboard";
    }
}
