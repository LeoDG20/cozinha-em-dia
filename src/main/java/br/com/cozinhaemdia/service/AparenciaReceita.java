package br.com.cozinhaemdia.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aparência padrão de uma receita sem foto: um emoji escolhido pelo nome e um tom de cor estável.
 * Assim, mesmo sem imagem, cada receita ganha uma capa reconhecível.
 */
public final class AparenciaReceita {
    private static final Map<String, String> EMOJIS = new LinkedHashMap<>();
    static {
        String[][] pares = {
                {"bolo", "🍰"}, {"torta", "🥧"}, {"pudim", "🍮"}, {"brigadeiro", "🍫"}, {"chocolate", "🍫"}, {"sorvete", "🍨"},
                {"panqueca", "🥞"}, {"crepe", "🥞"}, {"omelete", "🍳"}, {"ovo", "🍳"},
                {"salada", "🥗"}, {"sopa", "🍲"}, {"caldo", "🍲"}, {"feijoada", "🍲"}, {"risoto", "🍚"}, {"arroz", "🍚"},
                {"macarrao", "🍝"}, {"espaguete", "🍝"}, {"lasanha", "🍝"}, {"massa", "🍝"}, {"nhoque", "🍝"},
                {"pizza", "🍕"}, {"hamburguer", "🍔"}, {"sanduiche", "🥪"}, {"pao", "🍞"},
                {"frango", "🍗"}, {"carne", "🥩"}, {"bife", "🥩"}, {"churrasco", "🥩"}, {"peixe", "🐟"}, {"salmao", "🐟"}, {"camarao", "🍤"},
                {"lentilha", "🥣"}, {"feijao", "🫘"}, {"grao", "🫘"}, {"suco", "🥤"}, {"vitamina", "🥤"}, {"cafe", "☕"},
                {"banana", "🍌"}, {"maca", "🍎"}, {"morango", "🍓"}, {"limao", "🍋"}, {"batata", "🥔"}, {"tomate", "🍅"},
                {"cenoura", "🥕"}, {"milho", "🌽"}, {"cogumelo", "🍄"}, {"queijo", "🧀"}
        };
        for (String[] par : pares) EMOJIS.put(par[0], par[1]);
    }

    private AparenciaReceita() {}

    /** Emoji que representa a receita; usa um prato genérico quando o nome não lembra nenhum alimento conhecido. */
    public static String emoji(String nome) {
        String n = CorretorIngredientes.normalizar(nome);
        for (Map.Entry<String, String> entrada : EMOJIS.entrySet()) if (n.contains(entrada.getKey())) return entrada.getValue();
        return "🍽️";
    }

    /** Número de 1 a 6 que escolhe a cor de fundo da capa; o mesmo nome sempre gera o mesmo tom. */
    public static int tom(String nome) {
        String n = CorretorIngredientes.normalizar(nome);
        return (Math.abs(n.hashCode()) % 6) + 1;
    }
}
