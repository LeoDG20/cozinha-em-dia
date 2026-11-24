package br.com.cozinhaemdia.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Conversão entre unidades de medida.
 *
 * <p>Massa tem base em gramas e volume tem base em mililitros. As medidas caseiras entram como volume:
 * xícara = 240 ml, colher (de sopa) = 15 ml e colher de chá = 5 ml. Para passar de massa para volume (por exemplo,
 * "2 xícaras de farinha" para gramas) é preciso conhecer o ingrediente, porque cada um tem uma densidade diferente;
 * os valores da tabela são aproximados e vêm de equivalências usadas em livros de receitas.
 * "Unidade" e "dente" não têm equivalência fixa e só são comparáveis com elas mesmas.</p>
 */
public final class ConversorUnidades {
    private enum Grandeza { MASSA, VOLUME }

    private record Unidade(String simbolo, Grandeza grandeza, double fatorParaBase) {}

    /** Unidades oferecidas no conversor e nos formulários, na ordem de exibição. */
    public static final List<String> UNIDADES_CONVERSIVEIS = List.of("g", "kg", "ml", "l", "xícara", "colher", "colher de chá");

    private static final Map<String, Unidade> UNIDADES = Map.ofEntries(
            Map.entry("mg", new Unidade("mg", Grandeza.MASSA, 0.001)),
            Map.entry("g", new Unidade("g", Grandeza.MASSA, 1)),
            Map.entry("kg", new Unidade("kg", Grandeza.MASSA, 1000)),
            Map.entry("ml", new Unidade("ml", Grandeza.VOLUME, 1)),
            Map.entry("cl", new Unidade("cl", Grandeza.VOLUME, 10)),
            Map.entry("l", new Unidade("l", Grandeza.VOLUME, 1000)),
            Map.entry("xícara", new Unidade("xícara", Grandeza.VOLUME, 240)),
            Map.entry("colher", new Unidade("colher", Grandeza.VOLUME, 15)),
            Map.entry("colher de chá", new Unidade("colher de chá", Grandeza.VOLUME, 5)));

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
            Map.entry("colheres", "colher"), Map.entry("colher de sopa", "colher"), Map.entry("colheres de sopa", "colher"),
            Map.entry("colher sopa", "colher"), Map.entry("cs", "colher"),
            Map.entry("colher de cha", "colher de chá"), Map.entry("colheres de chá", "colher de chá"),
            Map.entry("colheres de cha", "colher de chá"), Map.entry("colher cha", "colher de chá"), Map.entry("cc", "colher de chá"),
            Map.entry("xicara", "xícara"), Map.entry("xicaras", "xícara"), Map.entry("xícaras", "xícara"));

    /** Gramas por mililitro de ingredientes comuns (aproximação). Chaves sem acento, da mais específica para a mais geral. */
    private static final Map<String, Double> DENSIDADES = new LinkedHashMap<>();
    static {
        DENSIDADES.put("farinha de trigo", 0.50);
        DENSIDADES.put("farinha de rosca", 0.46);
        DENSIDADES.put("farinha de mandioca", 0.58);
        DENSIDADES.put("farinha", 0.50);
        DENSIDADES.put("acucar mascavo", 0.72);
        DENSIDADES.put("acucar de confeiteiro", 0.55);
        DENSIDADES.put("acucar", 0.75);
        DENSIDADES.put("amido de milho", 0.54);
        DENSIDADES.put("maisena", 0.54);
        DENSIDADES.put("polvilho", 0.50);
        DENSIDADES.put("chocolate em po", 0.38);
        DENSIDADES.put("cacau", 0.38);
        DENSIDADES.put("aveia", 0.33);
        DENSIDADES.put("arroz", 0.75);
        DENSIDADES.put("queijo ralado", 0.42);
        DENSIDADES.put("manteiga", 0.91);
        DENSIDADES.put("margarina", 0.91);
        DENSIDADES.put("oleo", 0.92);
        DENSIDADES.put("azeite", 0.91);
        DENSIDADES.put("leite condensado", 1.30);
        DENSIDADES.put("creme de leite", 1.00);
        DENSIDADES.put("leite", 1.03);
        DENSIDADES.put("agua", 1.00);
        DENSIDADES.put("mel", 1.42);
        DENSIDADES.put("sal", 1.20);
    }

    private ConversorUnidades() {}

    /** Devolve o símbolo padronizado da unidade, em minúsculas e sem sinônimos. */
    public static String normalizar(String unidade) {
        if (unidade == null) return "";
        String chave = unidade.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").replace(".", "");
        return SINONIMOS.getOrDefault(chave, chave);
    }

    /** Indica se é possível converter valores entre as duas unidades (ou se são a mesma). */
    public static boolean saoCompativeis(String origem, String destino) {
        return converter(1, origem, destino).isPresent();
    }

    /** Converte o valor de uma unidade para outra da mesma grandeza; vazio quando as unidades são incompatíveis. */
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

    /**
     * Como {@link #converter(double, String, String)}, mas também passa entre massa e volume quando a densidade do
     * ingrediente é conhecida (por exemplo, xícara de farinha de trigo para gramas).
     */
    public static Optional<Double> converter(double valor, String origem, String destino, String ingrediente) {
        Optional<Double> direta = converter(valor, origem, destino);
        if (direta.isPresent()) return direta;
        Unidade a = UNIDADES.get(normalizar(origem));
        Unidade b = UNIDADES.get(normalizar(destino));
        if (a == null || b == null || a.grandeza() == b.grandeza()) return Optional.empty();
        Optional<Double> densidade = densidade(ingrediente);
        if (densidade.isEmpty()) return Optional.empty();
        double base = valor * a.fatorParaBase();
        double convertido = a.grandeza() == Grandeza.VOLUME ? base * densidade.get() : base / densidade.get();
        return Optional.of(convertido / b.fatorParaBase());
    }

    /** Gramas por mililitro do ingrediente, quando conhecido. */
    public static Optional<Double> densidade(String ingrediente) {
        if (ingrediente == null || ingrediente.isBlank()) return Optional.empty();
        String nome = Normalizer.normalize(ingrediente, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<String, Double> e : DENSIDADES.entrySet())
            if (java.util.regex.Pattern.compile("(?<![a-z])" + e.getKey() + "(?![a-z])").matcher(nome).find()) return Optional.of(e.getValue());
        return Optional.empty();
    }

    /** Indica se o conversor sabe a densidade do ingrediente. */
    public static boolean temDensidade(String ingrediente) {
        return densidade(ingrediente).isPresent();
    }
}
