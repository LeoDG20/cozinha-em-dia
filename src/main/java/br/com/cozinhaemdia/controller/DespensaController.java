package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.DespensaService;
import br.com.cozinhaemdia.service.DisponibilidadeService;
import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.ItemFaltante;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class DespensaController {
    private static final List<String> UNIDADES = List.of("g", "kg", "ml", "l", "unidade", "dente", "colher", "xícara");

    /** Item da lista de compras: o que falta para completar receitas, somado entre todas elas. */
    public record Compra(String nome, double quantidade, String unidade, List<String> receitas) {
        public String quantidadeTexto() { return br.com.cozinhaemdia.service.ReceitaService.formatar(quantidade) + " " + unidade; }
        public String linhaTexto() { return quantidadeTexto() + " de " + nome; }
    }

    private final DespensaService despensaService;
    private final UsuarioRepository usuarios;
    private final CorretorIngredientes corretor;
    private final ReceitaIngredienteRepository receitaIngredientes;
    private final DisponibilidadeService disponibilidade;

    public DespensaController(DespensaService despensaService, UsuarioRepository usuarios, CorretorIngredientes corretor,
                              ReceitaIngredienteRepository receitaIngredientes, DisponibilidadeService disponibilidade) {
        this.despensaService = despensaService; this.usuarios = usuarios; this.corretor = corretor;
        this.receitaIngredientes = receitaIngredientes; this.disponibilidade = disponibilidade;
    }

    @GetMapping("/despensa")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, Model model) {
        model.addAttribute("itens", despensaService.listar(auth.getId()));
        model.addAttribute("unidades", UNIDADES);
        model.addAttribute("sugestoes", corretor.nomesConhecidos());
        model.addAttribute("uso", usoPorIngrediente(auth.getId()));
        List<Compra> compras = listaDeCompras(disponibilidade.analisar(auth.getId()));
        model.addAttribute("compras", compras);
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

    /** Soma o que falta nas receitas quase prontas (e nas indisponíveis com poucos itens), agrupando por ingrediente e unidade. */
    static List<Compra> listaDeCompras(List<AnaliseReceita> analises) {
        Map<String, double[]> somas = new LinkedHashMap<>();
        Map<String, String[]> rotulos = new HashMap<>();
        Map<String, List<String>> origem = new HashMap<>();
        for (AnaliseReceita a : analises) {
            if (a.getStatus() == StatusDisponibilidade.DISPONIVEL) continue;
            for (ItemFaltante f : a.getFaltantes()) {
                String chave = f.getNome().toLowerCase(Locale.ROOT) + "|" + f.getUnidade();
                somas.computeIfAbsent(chave, k -> new double[1])[0] += f.getQuantidadeFaltante();
                rotulos.putIfAbsent(chave, new String[]{f.getNome(), f.getUnidade()});
                List<String> nomes = origem.computeIfAbsent(chave, k -> new ArrayList<>());
                if (!nomes.contains(a.getReceita().getNome())) nomes.add(a.getReceita().getNome());
            }
        }
        List<Compra> compras = new ArrayList<>();
        for (Map.Entry<String, double[]> e : somas.entrySet())
            compras.add(new Compra(rotulos.get(e.getKey())[0], e.getValue()[0], rotulos.get(e.getKey())[1], origem.get(e.getKey())));
        compras.sort((x, y) -> Integer.compare(y.receitas().size(), x.receitas().size()) != 0
                ? Integer.compare(y.receitas().size(), x.receitas().size()) : x.nome().compareToIgnoreCase(y.nome()));
        return compras;
    }
}
