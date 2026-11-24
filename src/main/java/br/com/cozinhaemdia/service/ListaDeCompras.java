package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.ItemFaltante;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Monta a lista de mercado: o que falta nas receitas escolhidas, somado por ingrediente e unidade. */
public final class ListaDeCompras {
    private ListaDeCompras() {}

    /** Item da lista: o que comprar, quanto, e para quais receitas. */
    public record Compra(String nome, double quantidade, String unidade, List<String> receitas) {
        public String quantidadeTexto() { return ReceitaService.formatar(quantidade) + " " + unidade; }
        public String linhaTexto() { return quantidadeTexto() + " de " + nome; }
        /** Valor enviado pelo formulário ao marcar o item como comprado: quantidade, unidade e nome. */
        public String valorFormulario() { return quantidade + "|" + unidade + "|" + nome; }
    }

    /** Soma o que falta nas receitas que não estão prontas, ordenando pelos ingredientes usados em mais receitas. */
    public static List<Compra> montar(List<AnaliseReceita> analises) {
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
