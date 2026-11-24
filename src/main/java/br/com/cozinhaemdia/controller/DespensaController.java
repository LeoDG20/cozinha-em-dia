package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.DespensaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DespensaController {
    private static final List<String> UNIDADES = List.of("g", "kg", "ml", "l", "unidade", "dente", "colher", "colher de chá", "xícara");

    private final DespensaService despensaService;
    private final UsuarioRepository usuarios;
    private final CorretorIngredientes corretor;
    private final ReceitaIngredienteRepository receitaIngredientes;

    public DespensaController(DespensaService despensaService, UsuarioRepository usuarios, CorretorIngredientes corretor,
                              ReceitaIngredienteRepository receitaIngredientes) {
        this.despensaService = despensaService; this.usuarios = usuarios; this.corretor = corretor;
        this.receitaIngredientes = receitaIngredientes;
    }

    @GetMapping("/despensa")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        model.addAttribute("itens", despensaService.listar(auth.getId()));
        model.addAttribute("unidades", UNIDADES);
        model.addAttribute("sugestoes", corretor.nomesConhecidos());
        model.addAttribute("uso", usoPorIngrediente(auth.getId()));
        return "despensa";
    }

    @PostMapping("/despensa")
    public String adicionar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam String nome, @RequestParam Double quantidade,
                            @RequestParam String unidade, RedirectAttributes flash) {
        CorretorIngredientes.Correcao correcao = corretor.corrigir(nome);
        try { despensaService.adicionar(usuarios.findById(auth.getId()).orElseThrow(), correcao.corrigido(), quantidade, unidade); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); return "redirect:/despensa"; }
        flash.addFlashAttribute("sucesso", correcao.alterado()
            ? "Ingrediente adicionado à despensa. Corrigimos \"" + correcao.original() + "\" para \"" + correcao.corrigido() + "\"."
            : "Ingrediente adicionado à despensa.");
        return "redirect:/despensa";
    }

    @PostMapping("/despensa/editar")
    public String editar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, @RequestParam Double quantidade,
                         @RequestParam String unidade, RedirectAttributes flash) {
        try { despensaService.atualizar(itemId, auth.getId(), quantidade, unidade); flash.addFlashAttribute("sucesso", "Ingrediente atualizado."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }

    @PostMapping("/despensa/remover")
    public String remover(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam Long itemId, RedirectAttributes flash) {
        try { despensaService.remover(itemId, auth.getId()); flash.addFlashAttribute("sucesso", "Ingrediente removido."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("erro", ex.getMessage()); }
        return "redirect:/despensa";
    }

    private Map<Long, Long> usoPorIngrediente(Long usuarioId) {
        Map<Long, Long> uso = new HashMap<>();
        for (Object[] linha : receitaIngredientes.contarUsoPorIngrediente(usuarioId)) uso.put((Long) linha[0], ((Number) linha[1]).longValue());
        return uso;
    }
}
