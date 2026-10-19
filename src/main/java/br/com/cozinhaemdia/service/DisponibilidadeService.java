package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.DespensaRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DisponibilidadeService {
    private final ReceitaService receitaService;
    private final DespensaRepository despensaRepository;
    public DisponibilidadeService(ReceitaService receitaService, DespensaRepository despensaRepository) { this.receitaService = receitaService; this.despensaRepository = despensaRepository; }

    public List<AnaliseReceita> analisar(Long usuarioId) {
        Map<Long, Despensa> estoque = new HashMap<>();
        for (Despensa item : despensaRepository.findAllByUsuarioIdOrderByIngredienteNomeAsc(usuarioId)) estoque.put(item.getIngrediente().getId(), item);
        List<AnaliseReceita> resultado = new ArrayList<>();
        for (Receita receita : receitaService.listar(usuarioId)) {
            List<ReceitaIngrediente> itens = receitaService.ingredientesDaReceita(receita.getId());
            List<ItemFaltante> faltantes = new ArrayList<>();
            int itensComEstoque = 0, itensOk = 0;
            for (ReceitaIngrediente requerido : itens) {
                Despensa disponivel = estoque.get(requerido.getIngrediente().getId());
                double qtdDisponivel = 0;
                if (disponivel != null && disponivel.getUnidade().equalsIgnoreCase(requerido.getUnidade())) {
                    qtdDisponivel = Math.max(0, disponivel.getQuantidade());
                    if (qtdDisponivel > 0) itensComEstoque++;
                }
                if (qtdDisponivel + 0.000001 >= requerido.getQuantidade()) itensOk++;
                else faltantes.add(new ItemFaltante(requerido.getIngrediente().getNome(), Math.max(0, requerido.getQuantidade() - qtdDisponivel), requerido.getUnidade(), qtdDisponivel));
            }
            StatusDisponibilidade status;
            if (faltantes.isEmpty()) status = StatusDisponibilidade.DISPONIVEL;
            else if (itensComEstoque == 0) status = StatusDisponibilidade.INDISPONIVEL;
            else status = StatusDisponibilidade.PARCIAL;
            resultado.add(new AnaliseReceita(receita, status, faltantes, itensOk, itens.size(), itensComEstoque));
        }
        resultado.sort(Comparator.comparing((AnaliseReceita a) -> a.getStatus().ordinal()).thenComparing(a -> a.getReceita().getNome()));
        return resultado;
    }
    public enum StatusDisponibilidade {
        DISPONIVEL("Pode preparar", "available"), PARCIAL("Faltam ingredientes", "partial"), INDISPONIVEL("Não disponível", "unavailable");
        private final String rotulo; private final String classe;
        StatusDisponibilidade(String rotulo, String classe) { this.rotulo = rotulo; this.classe = classe; }
        public String getRotulo() { return rotulo; }
        public String getClasse() { return classe; }
    }
    public static class ItemFaltante {
        private final String nome; private final double quantidadeFaltante; private final String unidade; private final double quantidadeDisponivel;
        public ItemFaltante(String nome, double quantidadeFaltante, String unidade, double quantidadeDisponivel) { this.nome=nome; this.quantidadeFaltante=quantidadeFaltante; this.unidade=unidade; this.quantidadeDisponivel=quantidadeDisponivel; }
        public String getNome() { return nome; }
        public double getQuantidadeFaltante() { return quantidadeFaltante; }
        public String getUnidade() { return unidade; }
        public double getQuantidadeDisponivel() { return quantidadeDisponivel; }
    }
    public static class AnaliseReceita {
        private final Receita receita; private final StatusDisponibilidade status; private final List<ItemFaltante> faltantes; private final int itensOk; private final int totalItens; private final int itensComEstoque;
        public AnaliseReceita(Receita receita, StatusDisponibilidade status, List<ItemFaltante> faltantes, int itensOk, int totalItens, int itensComEstoque) { this.receita=receita; this.status=status; this.faltantes=faltantes; this.itensOk=itensOk; this.totalItens=totalItens; this.itensComEstoque=itensComEstoque; }
        public Receita getReceita() { return receita; }
        public StatusDisponibilidade getStatus() { return status; }
        public List<ItemFaltante> getFaltantes() { return faltantes; }
        public int getItensOk() { return itensOk; }
        public int getTotalItens() { return totalItens; }
        public int getItensComEstoque() { return itensComEstoque; }
    }
}
