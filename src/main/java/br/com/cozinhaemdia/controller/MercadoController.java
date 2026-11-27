package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.DespensaService;
import br.com.cozinhaemdia.service.DisponibilidadeService;
import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;
import br.com.cozinhaemdia.service.ListaDeCompras;
import br.com.cozinhaemdia.service.ListaDeCompras.Compra;
import br.com.cozinhaemdia.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lista de mercado: a pessoa escolhe as receitas que quer fazer, vê o que falta comprar (já somado entre elas)
 * e, ao voltar do mercado, marca o que comprou para somar o item à despensa.
 */
@Controller
public class MercadoController {
    private final DisponibilidadeService disponibilidade;
    private final DespensaService despensa;
    private final UsuarioService usuarios;

    @Autowired
    public MercadoController(DisponibilidadeService disponibilidade, DespensaService despensa, UsuarioService usuarios) {
        this.disponibilidade = disponibilidade; this.despensa = despensa; this.usuarios = usuarios;
    }

    @GetMapping("/mercado")
    public String mercado(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam(required = false) List<Long> receitas,
                          @RequestParam(required = false) String f, Model model) {
        List<AnaliseReceita> analises = disponibilidade.analisar(auth.getId());
        Set<Long> escolhidas = new LinkedHashSet<>();
        if (f != null) { if (receitas != null) escolhidas.addAll(receitas); }
        else for (AnaliseReceita a : analises) if (a.getStatus() == StatusDisponibilidade.PARCIAL) escolhidas.add(a.getReceita().getId());

        List<AnaliseReceita> selecionadas = new ArrayList<>();
        for (AnaliseReceita a : analises) if (escolhidas.contains(a.getReceita().getId())) selecionadas.add(a);

        model.addAttribute("analises", analises);
        model.addAttribute("escolhidas", escolhidas);
        model.addAttribute("compras", ListaDeCompras.montar(selecionadas));
        return "mercado";
    }

    /** Soma à despensa os itens marcados como comprados. Cada valor chega no formato "quantidade|unidade|nome". */
    @PostMapping("/mercado/comprar")
    public String comprar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam(required = false) List<String> item,
                          @RequestParam(required = false) List<Long> receitas, RedirectAttributes flash) {
        if (receitas != null) flash.addAttribute("receitas", receitas);
        flash.addAttribute("f", "1");
        if (item == null || item.isEmpty()) {
            flash.addFlashAttribute("erro", "Marque ao menos um item para adicionar à despensa.");
            return "redirect:/mercado";
        }
        Usuario usuario = usuarios.buscar(auth.getId());
        int adicionados = 0;
        List<String> falhas = new ArrayList<>();
        for (String valor : item) {
            String[] partes = valor.split("\\|", 3);
            try {
                if (partes.length < 3) throw new IllegalArgumentException("Item inválido.");
                despensa.adicionar(usuario, partes[2], Double.valueOf(partes[0]), partes[1]);
                adicionados++;
            } catch (IllegalArgumentException ex) {
                falhas.add(partes.length == 3 ? partes[2] : valor);
            }
        }
        if (adicionados > 0) flash.addFlashAttribute("sucesso", adicionados == 1 ? "1 item foi somado à despensa." : adicionados + " itens foram somados à despensa.");
        if (!falhas.isEmpty()) flash.addFlashAttribute("erro", "Não foi possível adicionar: " + String.join(", ", falhas) + ". Confira a unidade na despensa.");
        return "redirect:/mercado";
    }
}
