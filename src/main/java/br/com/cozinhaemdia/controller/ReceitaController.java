package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Compartilhamento;
import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CompartilhamentoService;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.DisponibilidadeService;
import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;
import br.com.cozinhaemdia.service.PreparoService;
import br.com.cozinhaemdia.service.PreparoService.Etapa;
import br.com.cozinhaemdia.service.PreparoService.Midia;
import br.com.cozinhaemdia.service.ReceitaService;
import br.com.cozinhaemdia.service.RegistroReceitaService;
import br.com.cozinhaemdia.service.RegistroReceitaService.Dados;
import br.com.cozinhaemdia.service.RegistroReceitaService.Resultado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class ReceitaController {
    /** Dados de texto do formulário, devolvidos à tela quando há erro para que nada precise ser digitado de novo. */
    public record Formulario(String nome, String descricao, Integer tempo, Integer porcoes, String imagemUrl, String videoUrl) {}
    public record LinhaIngrediente(String nome, String quantidade, String unidade) {}
    public record LinhaEtapa(String texto, String foto) {}
    /** Etapa exibida na página da receita. */
    public record Passo(int ordem, String texto, String foto) {}

    private final ReceitaService receitaService;
    private final RegistroReceitaService registro;
    private final DisponibilidadeService disponibilidade;
    private final CompartilhamentoService compartilhamentos;
    private final CorretorIngredientes corretor;
    private final UsuarioRepository usuarios;

    public ReceitaController(ReceitaService receitaService, RegistroReceitaService registro, DisponibilidadeService disponibilidade,
                             CompartilhamentoService compartilhamentos, CorretorIngredientes corretor, UsuarioRepository usuarios) {
        this.receitaService = receitaService; this.registro = registro; this.disponibilidade = disponibilidade;
        this.compartilhamentos = compartilhamentos; this.corretor = corretor; this.usuarios = usuarios;
    }

    @GetMapping("/receitas")
    public String listar(@AuthenticationPrincipal UsuarioAutenticado auth, @RequestParam(required = false) String q,
                         @RequestParam(required = false) String situacao, Model model) {
        List<AnaliseReceita> todas = new ArrayList<>(disponibilidade.analisar(auth.getId()));
        todas.sort((a, b) -> a.getReceita().getNome().compareToIgnoreCase(b.getReceita().getNome()));
        String consulta = q == null ? "" : q.trim();
        StatusDisponibilidade filtro = statusDe(situacao);
        List<AnaliseReceita> exibidas = new ArrayList<>();
        for (AnaliseReceita a : todas) {
            if (filtro != null && a.getStatus() != filtro) continue;
            if (!consulta.isEmpty() && !corresponde(consulta, a.getReceita())) continue;
            exibidas.add(a);
        }
        model.addAttribute("analises", exibidas);
        model.addAttribute("total", todas.size());
        model.addAttribute("q", consulta);
        model.addAttribute("situacao", filtro == null ? "" : situacao.toLowerCase());
        model.addAttribute("sugestao", exibidas.isEmpty() && !consulta.isEmpty() ? sugestaoDeBusca(consulta) : null);
        return "receitas";
    }

    @GetMapping("/receitas/nova")
    public String nova(Model model) {
        prepararFormulario(model, null, new Formulario("", "", null, null, "", ""), List.of(new LinhaIngrediente("", "", "")), List.of(new LinhaEtapa("", "")));
        return "receita-form";
    }

    @PostMapping("/receitas")
    public String criar(@AuthenticationPrincipal UsuarioAutenticado auth, HttpServletRequest request,
                        @RequestParam(required = false) String nome, @RequestParam(required = false) String descricao,
                        @RequestParam(required = false) String tempoPreparoMin, @RequestParam(required = false) String porcoes,
                        @RequestParam(required = false) String imagemUrl, @RequestParam(required = false) String videoUrl,
                        Model model, RedirectAttributes flash) {
        Formulario form = new Formulario(nome, descricao, inteiro(tempoPreparoMin), inteiro(porcoes), imagemUrl, videoUrl);
        List<LinhaIngrediente> ingredientes = linhasIngredientes(request);
        List<LinhaEtapa> etapasForm = linhasEtapas(request);
        try {
            Resultado r = registro.criar(usuario(auth), dados(form, ingredientes), midia(request, form), etapas(request));
            avisar(flash, "Receita cadastrada com sucesso.", r);
            return "redirect:/receitas/" + r.receita().getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            prepararFormulario(model, null, form, ingredientes, etapasForm);
            return "receita-form";
        }
    }

    @GetMapping("/receitas/{id}")
    public String detalhe(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, Model model, RedirectAttributes flash) {
        Receita receita = compartilhamentos.visivelPara(id, auth.getId()).orElse(null);
        if (receita == null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        boolean dono = receita.getUsuario().getId().equals(auth.getId());
        model.addAttribute("receita", receita);
        model.addAttribute("itens", receitaService.ingredientesDaReceita(id));
        model.addAttribute("analise", disponibilidade.analisarReceita(receita, auth.getId()));
        model.addAttribute("passos", passos(receita));
        model.addAttribute("dono", dono);
        if (dono) {
            model.addAttribute("destinatarios", compartilhamentos.enviadasDa(id));
        } else {
            Optional<Compartilhamento> recebido = compartilhamentos.recebidas(auth.getId()).stream().filter(c -> c.getReceita().getId().equals(id)).findFirst();
            model.addAttribute("recebido", recebido.orElse(null));
        }
        return "receita-detalhe";
    }

    @GetMapping("/receitas/{id}/editar")
    public String editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, Model model, RedirectAttributes flash) {
        Receita receita = receitaService.buscar(id, auth.getId()).orElse(null);
        if (receita == null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        List<LinhaIngrediente> ingredientes = new ArrayList<>();
        for (ReceitaIngrediente item : receitaService.ingredientesDaReceita(id))
            ingredientes.add(new LinhaIngrediente(item.getIngrediente().getNome(), ReceitaService.formatar(item.getQuantidade()), item.getUnidade()));
        List<LinhaEtapa> etapas = new ArrayList<>();
        for (Passo p : passos(receita)) etapas.add(new LinhaEtapa(p.texto(), p.foto() == null ? "" : p.foto()));
        if (etapas.isEmpty()) etapas.add(new LinhaEtapa("", ""));
        Formulario form = new Formulario(receita.getNome(), receita.getDescricao(), receita.getTempoPreparoMin(), receita.getPorcoes(),
                receita.getImagemUrl() == null ? "" : receita.getImagemUrl(), receita.getVideoUrl() == null ? "" : receita.getVideoUrl());
        prepararFormulario(model, receita, form, ingredientes, etapas);
        return "receita-form";
    }

    @PostMapping("/receitas/{id}/editar")
    public String atualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, HttpServletRequest request,
                            @RequestParam(required = false) String nome, @RequestParam(required = false) String descricao,
                            @RequestParam(required = false) String tempoPreparoMin, @RequestParam(required = false) String porcoes,
                            @RequestParam(required = false) String imagemUrl, @RequestParam(required = false) String videoUrl,
                            Model model, RedirectAttributes flash) {
        Receita receita = receitaService.buscar(id, auth.getId()).orElse(null);
        if (receita == null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        Formulario form = new Formulario(nome, descricao, inteiro(tempoPreparoMin), inteiro(porcoes), imagemUrl, videoUrl);
        List<LinhaIngrediente> ingredientes = linhasIngredientes(request);
        List<LinhaEtapa> etapasForm = linhasEtapas(request);
        try {
            Resultado r = registro.atualizar(receita, usuario(auth), dados(form, ingredientes), midia(request, form), etapas(request));
            avisar(flash, "Receita atualizada.", r);
            return "redirect:/receitas/" + id;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            prepararFormulario(model, receita, form, ingredientes, etapasForm);
            return "receita-form";
        }
    }

    @PostMapping("/receitas/{id}/excluir")
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        Receita receita = receitaService.buscar(id, auth.getId()).orElse(null);
        if (receita != null) { registro.excluir(receita); flash.addFlashAttribute("sucesso", "Receita excluída."); }
        return "redirect:/receitas";
    }

    @PostMapping("/receitas/{id}/compartilhar")
    public String compartilhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth,
                               @RequestParam String email, @RequestParam(required = false) String mensagem, RedirectAttributes flash) {
        Receita receita = receitaService.buscar(id, auth.getId()).orElse(null);
        if (receita == null) { flash.addFlashAttribute("erro", "Receita não encontrada."); return "redirect:/receitas"; }
        try {
            Compartilhamento c = compartilhamentos.compartilhar(receita, usuario(auth), email, mensagem);
            flash.addFlashAttribute("sucesso", "Receita compartilhada com " + c.getDestino().getNome() + ".");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/receitas/" + id;
    }

    @PostMapping("/receitas/{id}/salvar-copia")
    public String salvarCopia(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado auth, RedirectAttributes flash) {
        try {
            Receita copia = compartilhamentos.salvarCopia(id, usuario(auth));
            flash.addFlashAttribute("sucesso", "A receita foi salva na sua conta. Agora você pode editá-la.");
            return "redirect:/receitas/" + copia.getId();
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/compartilhadas";
        }
    }

    private void prepararFormulario(Model model, Receita receita, Formulario form, List<LinhaIngrediente> ingredientes, List<LinhaEtapa> etapas) {
        model.addAttribute("edicao", receita != null);
        model.addAttribute("receita", receita);
        model.addAttribute("formAction", receita == null ? "/receitas" : "/receitas/" + receita.getId() + "/editar");
        model.addAttribute("form", form);
        model.addAttribute("linhasIngredientes", ingredientes.isEmpty() ? List.of(new LinhaIngrediente("", "", "")) : ingredientes);
        model.addAttribute("linhasEtapas", etapas.isEmpty() ? List.of(new LinhaEtapa("", "")) : etapas);
        model.addAttribute("sugestoes", corretor.nomesConhecidos());
    }

    private static void avisar(RedirectAttributes flash, String mensagem, Resultado r) {
        flash.addFlashAttribute("sucesso", mensagem);
        if (!r.avisos().isEmpty()) flash.addFlashAttribute("avisos", r.avisos());
    }

    private static Dados dados(Formulario f, List<LinhaIngrediente> ingredientes) {
        List<String> nomes = new ArrayList<>(), quantidades = new ArrayList<>(), unidades = new ArrayList<>();
        for (LinhaIngrediente l : ingredientes) { nomes.add(l.nome()); quantidades.add(l.quantidade()); unidades.add(l.unidade()); }
        return new Dados(f.nome(), f.descricao(), f.tempo(), f.porcoes(), nomes, quantidades, unidades);
    }

    private static Midia midia(HttpServletRequest request, Formulario f) {
        return new Midia(arquivo(request, "imagem"), f.imagemUrl(), "on".equals(request.getParameter("removerImagem")) || "true".equals(request.getParameter("removerImagem")),
                f.videoUrl(), arquivo(request, "video"), "on".equals(request.getParameter("removerVideo")) || "true".equals(request.getParameter("removerVideo")));
    }

    /** Cada linha de etapa tem uma chave própria (etapaChave) que identifica seus campos: texto, foto nova e foto atual. */
    private static List<Etapa> etapas(HttpServletRequest request) {
        List<Etapa> etapas = new ArrayList<>();
        for (String chave : chaves(request)) {
            String atual = request.getParameter("etapaFotoAtual_" + chave);
            etapas.add(new Etapa(request.getParameter("etapaTexto_" + chave), arquivo(request, "etapaFoto_" + chave),
                    atual == null || atual.isBlank() ? null : atual.trim()));
        }
        return etapas;
    }

    private static List<String> chaves(HttpServletRequest request) {
        List<String> chaves = new ArrayList<>();
        for (String c : valores(request, "etapaChave")) if (c.matches("\\w{1,20}")) chaves.add(c);
        return chaves;
    }

    private static List<LinhaIngrediente> linhasIngredientes(HttpServletRequest request) {
        List<String> nomes = valores(request, "ingNome"), quantidades = valores(request, "ingQtd"), unidades = valores(request, "ingUn");
        List<LinhaIngrediente> linhas = new ArrayList<>();
        for (int i = 0; i < Math.max(nomes.size(), Math.max(quantidades.size(), unidades.size())); i++)
            linhas.add(new LinhaIngrediente(item(nomes, i), item(quantidades, i), item(unidades, i)));
        return linhas;
    }

    private static List<LinhaEtapa> linhasEtapas(HttpServletRequest request) {
        List<LinhaEtapa> linhas = new ArrayList<>();
        for (String chave : chaves(request)) {
            String texto = request.getParameter("etapaTexto_" + chave), atual = request.getParameter("etapaFotoAtual_" + chave);
            linhas.add(new LinhaEtapa(texto == null ? "" : texto, atual == null ? "" : atual));
        }
        return linhas;
    }

    private List<Passo> passos(Receita receita) {
        List<Passo> passos = new ArrayList<>();
        if (!receita.getEtapas().isEmpty()) {
            for (EtapaReceita e : receita.getEtapas()) passos.add(new Passo(e.getOrdem(), e.getDescricao(), e.getImagemArquivo()));
        } else {
            int ordem = 0;
            for (String texto : PreparoService.dividirModoPreparo(receita.getModoPreparo())) passos.add(new Passo(++ordem, texto, null));
        }
        return passos;
    }

    private boolean corresponde(String consulta, Receita receita) {
        if (CorretorIngredientes.correspondeBusca(consulta, receita.getNome())) return true;
        if (receita.getDescricao() != null && CorretorIngredientes.correspondeBusca(consulta, receita.getDescricao())) return true;
        for (ReceitaIngrediente item : receitaService.ingredientesDaReceita(receita.getId()))
            if (CorretorIngredientes.correspondeBusca(consulta, item.getIngrediente().getNome())) return true;
        return false;
    }

    /** Quando a busca não encontra nada, sugere a grafia mais provável do termo digitado. */
    private String sugestaoDeBusca(String consulta) {
        CorretorIngredientes.Correcao c = corretor.corrigir(consulta);
        return c.alterado() ? c.corrigido() : null;
    }

    private static StatusDisponibilidade statusDe(String situacao) {
        if (situacao == null) return null;
        return switch (situacao.toLowerCase()) {
            case "disponivel" -> StatusDisponibilidade.DISPONIVEL;
            case "parcial" -> StatusDisponibilidade.PARCIAL;
            case "indisponivel" -> StatusDisponibilidade.INDISPONIVEL;
            default -> null;
        };
    }

    private static Integer inteiro(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try { return Integer.valueOf(texto.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static List<String> valores(HttpServletRequest request, String nome) {
        String[] v = request.getParameterValues(nome);
        return v == null ? List.of() : Arrays.asList(v);
    }

    private static String item(List<String> lista, int indice) {
        return indice < lista.size() && lista.get(indice) != null ? lista.get(indice) : "";
    }

    private static MultipartFile arquivo(HttpServletRequest request, String nome) {
        MultipartHttpServletRequest m = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        return m == null ? null : m.getFile(nome);
    }

    private static List<MultipartFile> arquivos(HttpServletRequest request, String nome) {
        MultipartHttpServletRequest m = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        return m == null ? List.of() : m.getFiles(nome);
    }

    private Usuario usuario(UsuarioAutenticado a) { return usuarios.findById(a.getId()).orElseThrow(); }
}
