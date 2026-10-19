package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ReceitaService {
    private final ReceitaRepository receitas;
    private final ReceitaIngredienteRepository receitaIngredientes;
    private final IngredienteRepository ingredientes;
    public ReceitaService(ReceitaRepository receitas, ReceitaIngredienteRepository receitaIngredientes, IngredienteRepository ingredientes) {
        this.receitas = receitas; this.receitaIngredientes = receitaIngredientes; this.ingredientes = ingredientes;
    }
    public List<Receita> listar(Long usuarioId) { return receitas.findAllByUsuarioIdOrderByNomeAsc(usuarioId); }
    public Optional<Receita> buscar(Long id, Long usuarioId) { return receitas.findByIdAndUsuarioId(id, usuarioId); }

    @Transactional
    public Receita criar(Usuario usuario, String nome, String descricao, String modoPreparo, Integer tempo, Integer porcoes, String textoIngredientes) {
        Receita r = new Receita(); preencher(r, usuario, nome, descricao, modoPreparo, tempo, porcoes);
        List<LinhaIngrediente> linhas = interpretarIngredientes(textoIngredientes);
        validarReceita(r, linhas);
        Receita salva = receitas.save(r);
        salvarIngredientes(salva, linhas);
        return salva;
    }
    @Transactional
    public Receita atualizar(Receita r, Usuario usuario, String nome, String descricao, String modoPreparo, Integer tempo, Integer porcoes, String textoIngredientes) {
        List<LinhaIngrediente> linhas = interpretarIngredientes(textoIngredientes);
        preencher(r, usuario, nome, descricao, modoPreparo, tempo, porcoes);
        validarReceita(r, linhas);
        Receita salva = receitas.save(r);
        receitaIngredientes.deleteAllByReceitaId(salva.getId());
        receitaIngredientes.flush();
        salvarIngredientes(salva, linhas);
        return salva;
    }
    @Transactional
    public void excluir(Receita r) {
        receitaIngredientes.deleteAllByReceitaId(r.getId());
        receitas.delete(r);
    }
    @Transactional(readOnly = true)
    public List<ReceitaIngrediente> ingredientesDaReceita(Long receitaId) { return receitaIngredientes.findAllByReceitaIdOrderByIdAsc(receitaId); }
    public String textoIngredientes(Receita r) {
        List<ReceitaIngrediente> itens = ingredientesDaReceita(r.getId());
        StringJoiner joiner = new StringJoiner("\n");
        for (ReceitaIngrediente item : itens) joiner.add(item.getIngrediente().getNome() + " | " + formatar(item.getQuantidade()) + " | " + item.getUnidade());
        return joiner.toString();
    }
    private void preencher(Receita r, Usuario usuario, String nome, String descricao, String modo, Integer tempo, Integer porcoes) {
        r.setUsuario(usuario); r.setNome(nome == null ? "" : nome.trim()); r.setDescricao(descricao == null ? "" : descricao.trim());
        r.setModoPreparo(modo == null ? "" : modo.trim()); r.setTempoPreparoMin(tempo); r.setPorcoes(porcoes);
    }
    private void validarReceita(Receita r, List<LinhaIngrediente> linhas) {
        if (r.getNome().isBlank()) throw new IllegalArgumentException("Informe o nome da receita.");
        if (r.getModoPreparo().isBlank()) throw new IllegalArgumentException("Informe o modo de preparo.");
        if (linhas.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um ingrediente.");
        Set<String> nomes = new HashSet<>();
        for (LinhaIngrediente l : linhas) if (!nomes.add(l.nome().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("O ingrediente '"+l.nome()+"' aparece mais de uma vez. Agrupe as quantidades em uma linha.");
    }
    private List<LinhaIngrediente> interpretarIngredientes(String texto) {
        List<LinhaIngrediente> linhas = new ArrayList<>();
        if (texto == null || texto.isBlank()) return linhas;
        int numero = 0;
        for (String linha : texto.split("\\R")) {
            numero++;
            if (linha.isBlank()) continue;
            String[] partes = linha.split("\\|", -1);
            if (partes.length != 3) throw new IllegalArgumentException("Linha " + numero + ": use Nome do ingrediente | quantidade | unidade.");
            String nome = partes[0].trim(), unidade = partes[2].trim().toLowerCase(Locale.ROOT);
            if (nome.isBlank() || unidade.isBlank()) throw new IllegalArgumentException("Linha " + numero + ": preencha nome, quantidade e unidade.");
            double qtd;
            try { qtd = Double.parseDouble(partes[1].trim().replace(',', '.')); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Linha " + numero + ": quantidade inválida."); }
            if (qtd <= 0) throw new IllegalArgumentException("Linha " + numero + ": a quantidade deve ser maior que zero.");
            linhas.add(new LinhaIngrediente(nome, qtd, unidade));
        }
        return linhas;
    }
    private void salvarIngredientes(Receita r, List<LinhaIngrediente> linhas) {
        List<ReceitaIngrediente> itens = new ArrayList<>();
        for (LinhaIngrediente linha : linhas) {
            Ingrediente ing = ingredientes.findByNomeIgnoreCase(linha.nome()).orElseGet(() -> ingredientes.save(new Ingrediente(linha.nome())));
            itens.add(new ReceitaIngrediente(r, ing, linha.quantidade(), linha.unidade()));
        }
        receitaIngredientes.saveAll(itens);
    }
    public static String formatar(Double valor) {
        if (valor == null) return "0";
        if (Math.abs(valor - Math.rint(valor)) < 0.00001) return String.valueOf((long)Math.rint(valor));
        return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", valor).replaceAll("0+$", "").replaceAll(",$", "");
    }
    private record LinhaIngrediente(String nome, double quantidade, String unidade) {}
}
