package br.com.cozinhaemdia.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Conversão entre unidades de medida da mesma grandeza.
 *
 * <p>Há duas grandezas convertíveis: massa (base em gramas) e volume (base em mililitros).
 * Unidades sem equivalência fixa, como "unidade", "dente" e "colher", só são comparáveis
 * com elas mesmas.</p>
 */
public final class ConversorUnidades {
    private enum Grandeza { MASSA, VOLUME, OUTRA }

    private record Unidade(String simbolo, Grandeza grandeza, double fatorParaBase) {}

    private static final Map<String, Unidade> UNIDADES = Map.ofEntries(
            Map.entry("mg", new Unidade("mg", Grandeza.MASSA, 0.001)),
            Map.entry("g", new Unidade("g", Grandeza.MASSA, 1)),
            Map.entry("kg", new Unidade("kg", Grandeza.MASSA, 1000)),
            Map.entry("ml", new Unidade("ml", Grandeza.VOLUME, 1)),
            Map.entry("cl", new Unidade("cl", Grandeza.VOLUME, 10)),
            Map.entry("l", new Unidade("l", Grandeza.VOLUME, 1000)));

    private static final Map<String, String> SINONIMOS = Map.ofEntries(
            Map.entry("grama", "g"), Map.entry("gramas", "g"), Map.entry("gr", "g"),
            Map.entry("miligrama", "mg"), Map.entry("miligramas", "mg"),
            Map.entry("quilo", "kg"), Map.entry("quilos", "kg"),
            Map.entry("quilograma", "kg"), Map.entry("quilogramas", "kg"),
            Map.entry("mililitro", "ml"), Map.entry("mililitros", "ml"),
            Map.entry("centilitro", "cl"), Map.entry("centilitros", "cl"),
            Map.entry("litro", "l"), Map.entry("litros", "l"), Map.entry("lt", "l"),
            Map.entry("un", "unidade"), Map.entry("und", "unidade"), Map.entry("unid", "unidade"),
            Map.entry("unidades", "unidade"), Map.entry("dentes", "dente"),
            Map.entry("colheres", "colher"), Map.entry("xicara", "xícara"),
            Map.entry("xicaras", "xícara"), Map.entry("xícaras", "xícara"));

    private ConversorUnidades() {}

    /** Devolve o símbolo padronizado da unidade, em minúsculas e sem sinônimos. */
    public static String normalizar(String unidade) {
        if (unidade == null) return "";
        String chave = unidade.trim().toLowerCase(Locale.ROOT);
        return SINONIMOS.getOrDefault(chave, chave);
    }

    /** Indica se é possível converter valores entre as duas unidades (ou se são a mesma). */
    public static boolean saoCompativeis(String origem, String destino) {
        return converter(1, origem, destino).isPresent();
    }

    /** Converte o valor de uma unidade para outra; vazio quando as unidades são incompatíveis. */
    public static Optional<Double> converter(double valor, String origem, String destino) {
        String de = normalizar(origem);
        String para = normalizar(destino);
        if (de.isEmpty() || para.isEmpty()) return Optional.empty();
        if (de.equals(para)) return Optional.of(valor);
        Unidade a = UNIDADES.get(de);
        Unidade b = UNIDADES.get(para);
        if (a == null || b == null || a.grandeza() != b.grandeza()) return Optional.empty();
        return Optional.of(valor * a.fatorParaBase() / b.fatorParaBase());
    }
}
