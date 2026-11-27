package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.repository.IngredienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Corrige nomes de ingredientes digitados com erro (por exemplo, "farna" para "Farinha").
 *
 * <p>O vocabulário é formado pelos ingredientes já cadastrados no banco e por uma lista de
 * ingredientes comuns ({@code ingredientes-comuns.txt}). Cada palavra desconhecida é comparada
 * com as palavras do vocabulário pela distância de edição (inserções, remoções, substituições e
 * trocas de letras vizinhas). A correção só é aplicada quando existe uma única palavra mais
 * próxima, de modo que casos ambíguos permanecem como o usuário digitou.</p>
 */
@Component
public class CorretorIngredientes {
    /** Resultado da correção de um nome. {@code alterado} é verdadeiro só quando houve correção de erro de digitação. */
    public record Correcao(String original, String corrigido, boolean alterado) {}

    /** Situação de um nome digitado: se já é conhecido, a correção automática provável e outras sugestões ordenadas por semelhança. */
    public record Verificacao(String original, boolean conhecido, String corrigido, boolean alterado, List<String> sugestoes) {}

    /** Texto de ingredientes (uma linha por item) após a correção, com os avisos para o usuário. */
    public record TextoCorrigido(String texto, List<String> avisos) {}

    /** Nota mínima (de 0 a 1) para um nome ser oferecido como "parecido". */
    private static final double LIMIAR_SEMELHANCA = 0.62;
    private static final Set<String> LIGACOES = Set.of("de", "do", "da", "dos", "das", "e", "com", "em", "ao", "para", "sem", "a", "o");

    private final IngredienteRepository ingredientes;
    private final List<String> comuns;

    @Autowired
    public CorretorIngredientes(IngredienteRepository ingredientes) {
        this.ingredientes = ingredientes;
        this.comuns = carregarComuns();
    }

    /** Corrige o nome digitado. Se já for um nome conhecido, devolve a grafia cadastrada (acentos e maiúsculas). */
    public Correcao corrigir(String digitado) {
        String original = digitado == null ? "" : digitado.trim().replaceAll("\\s+", " ");
        if (original.isEmpty()) return new Correcao(original, original, false);

        Map<String, String> conhecidos = mapaConhecidos();
        String chave = normalizar(original);
        if (conhecidos.containsKey(chave)) return new Correcao(original, conhecidos.get(chave), false);

        Map<String, Integer> pesos = new LinkedHashMap<>();
        Map<String, String> palavras = mapaPalavras(conhecidos.values(), pesos);
        List<String> saida = new ArrayList<>();
        boolean mudou = false;
        for (String token : original.split(" ")) {
            String t = normalizar(token);
            String melhor = null;
            if (!LIGACOES.contains(t) && t.length() > 3 && !palavras.containsKey(t) && t.chars().allMatch(Character::isLetter)) {
                melhor = palavraMaisProxima(t, pesos);
            }
            if (melhor != null) { saida.add(palavras.get(melhor)); mudou = true; }
            else saida.add(token.toLowerCase(Locale.ROOT));
        }
        if (!mudou) return new Correcao(original, original, false);

        String montado = String.join(" ", saida);
        String canonico = conhecidos.get(normalizar(montado));
        String corrigido = canonico != null ? canonico : capitalizar(montado);
        return new Correcao(original, corrigido, true);
    }

    /** Corrige o primeiro campo de cada linha no formato {@code Nome | quantidade | unidade}. */
    public TextoCorrigido corrigirLinhas(String texto) {
        List<String> avisos = new ArrayList<>();
        if (texto == null || texto.isBlank()) return new TextoCorrigido(texto == null ? "" : texto, avisos);
        List<String> linhas = new ArrayList<>();
        for (String linha : texto.split("\\R", -1)) {
            String[] partes = linha.split("\\|", -1);
            if (partes.length == 3 && !partes[0].isBlank()) {
                Correcao c = corrigir(partes[0]);
                if (c.alterado()) avisos.add("\"" + c.original() + "\" foi corrigido para \"" + c.corrigido() + "\".");
                linhas.add(c.corrigido() + " | " + partes[1].trim() + " | " + partes[2].trim());
            } else {
                linhas.add(linha);
            }
        }
        return new TextoCorrigido(String.join("\n", linhas), avisos);
    }

    /** Todos os nomes conhecidos (cadastrados e comuns), em ordem alfabética. */
    public List<String> nomesConhecidos() {
        List<String> nomes = new ArrayList<>(mapaConhecidos().values());
        nomes.sort((a, b) -> normalizar(a).compareTo(normalizar(b)));
        return nomes;
    }

    /** Sugestões para o texto digitado: começam com ele, depois contêm, depois são parecidos (pelo som e pela escrita). */
    public List<String> sugerir(String termo, int maximo) {
        String t = normalizar(termo == null ? "" : termo);
        List<String> nomes = nomesConhecidos();
        if (t.isEmpty()) return nomes.subList(0, Math.min(maximo, nomes.size()));
        List<String> inicio = new ArrayList<>(), contem = new ArrayList<>();
        for (String nome : nomes) {
            String n = normalizar(nome);
            if (n.startsWith(t)) inicio.add(nome);
            else if (n.contains(t)) contem.add(nome);
        }
        List<String> resultado = new ArrayList<>(inicio);
        resultado.addAll(contem);
        for (String nome : parecidos(termo, nomes, maximo)) if (!resultado.contains(nome)) resultado.add(nome);
        return resultado.subList(0, Math.min(maximo, resultado.size()));
    }

    /** Nomes que se parecem com o termo, do mais para o menos semelhante, mesmo quando o início da palavra está errado. */
    public List<String> parecidos(String termo, int maximo) {
        return parecidos(termo == null ? "" : termo, nomesConhecidos(), maximo);
    }

    /**
     * Analisa um nome digitado. Se for desconhecido, devolve a correção automática (quando há uma única opção clara)
     * e uma lista de alternativas para que a pessoa escolha ("Você quis dizer...?").
     */
    public Verificacao verificar(String digitado) {
        String original = digitado == null ? "" : digitado.trim().replaceAll("\\s+", " ");
        if (original.isEmpty()) return new Verificacao(original, true, original, false, List.of());
        Correcao c = corrigir(original);
        if (mapaConhecidos().containsKey(normalizar(original))) return new Verificacao(original, true, c.corrigido(), false, List.of());
        List<String> alternativas = new ArrayList<>();
        if (c.alterado()) alternativas.add(c.corrigido());
        for (String nome : parecidos(original, nomesConhecidos(), 5))
            if (!alternativas.contains(nome)) alternativas.add(nome);
        return new Verificacao(original, false, c.corrigido(), c.alterado(), alternativas.subList(0, Math.min(4, alternativas.size())));
    }

    /** Entre os textos candidatos, os que mais se parecem com o termo digitado (por escrita e som), do mais ao menos parecido. */
    public static List<String> maisParecidos(String termo, List<String> candidatos, int maximo) {
        return parecidos(termo == null ? "" : termo, candidatos, maximo);
    }

    private static List<String> parecidos(String termo, List<String> nomes, int maximo) {
        if (normalizar(termo).isEmpty()) return List.of();
        record Pontuado(String nome, double nota) {}
        List<Pontuado> lista = new ArrayList<>();
        for (String nome : nomes) {
            double nota = semelhanca(termo, nome);
            if (nota >= LIMIAR_SEMELHANCA) lista.add(new Pontuado(nome, nota));
        }
        lista.sort((a, b) -> Double.compare(b.nota(), a.nota()) != 0 ? Double.compare(b.nota(), a.nota()) : Integer.compare(a.nome().length(), b.nome().length()));
        List<String> saida = new ArrayList<>();
        for (Pontuado p : lista) { if (saida.size() >= maximo) break; saida.add(p.nome()); }
        return saida;
    }

    /**
     * Nota de 0 a 1 para a semelhança entre o termo digitado e um nome (ambos normalizados). Cada palavra digitada é
     * comparada com as palavras do nome por três critérios: escrita (distância de edição), som (chave fonética do
     * português) e trechos de três letras em comum; vale o melhor critério, e a nota final é a média das palavras.
     */
    static double semelhanca(String termo, String nome) {
        String[] digitadas = normalizar(termo).split(" "), palavrasNome = normalizar(nome).split(" ");
        String[] sonsDigitados = paraSom(termo).split(" "), sonsNome = paraSom(nome).split(" ");
        double soma = 0;
        for (int i = 0; i < digitadas.length; i++) {
            String q = digitadas[i];
            if (LIGACOES.contains(q)) { soma += (" " + normalizar(nome) + " ").contains(" " + q + " ") ? 1 : 0.7; continue; }
            double melhor = 0;
            for (int j = 0; j < palavrasNome.length; j++) {
                if (LIGACOES.contains(palavrasNome[j])) continue;
                melhor = Math.max(melhor, semelhancaDePalavras(q, palavrasNome[j], sonsDigitados[i], sonsNome[j]));
            }
            soma += melhor;
        }
        return soma / digitadas.length;
    }

    /** Texto normalizado preservando o som do cedilha (ç soa como ss), que a normalização comum apagaria. */
    private static String paraSom(String texto) {
        return normalizar(texto == null ? "" : texto.toLowerCase(Locale.ROOT).replace("ç", "ss"));
    }

    private static double semelhancaDePalavras(String q, String p, String somQ, String somP) {
        if (q.equals(p)) return 1.0;
        if (q.length() <= 2 || p.length() <= 2) return 0;
        int maior = Math.max(q.length(), p.length());
        double escrita = 1.0 - (double) distancia(q, p) / maior;
        String fq = chaveFonetica(somQ), fp = chaveFonetica(somP);
        double som = fq.equals(fp) ? 0.95 : 1.0 - (double) distancia(fq, fp) / Math.max(1, Math.max(fq.length(), fp.length())) - 0.05;
        double trechos = diceDeTrigramas(q, p) * 0.9;
        // Um prefixo digitado de uma palavra longa conta como boa correspondência ("cenou" para "cenoura").
        double prefixo = q.length() >= 3 && p.startsWith(q) ? 0.85 : 0;
        return Math.max(Math.max(escrita, som), Math.max(trechos, prefixo));
    }

    private static double diceDeTrigramas(String a, String b) {
        Set<String> ta = trigramas(a), tb = trigramas(b);
        if (ta.isEmpty() || tb.isEmpty()) return 0;
        int comuns = 0;
        for (String t : ta) if (tb.contains(t)) comuns++;
        return 2.0 * comuns / (ta.size() + tb.size());
    }

    private static Set<String> trigramas(String palavra) {
        String x = "^" + palavra + "$";
        Set<String> conjunto = new HashSet<>();
        for (int i = 0; i + 3 <= x.length(); i++) conjunto.add(x.substring(i, i + 3));
        return conjunto;
    }

    /**
     * Chave aproximada da pronúncia de uma palavra normalizada em português: agrupa grafias que soam parecido
     * (ch/x, ss/c/ç/s/z, g/j antes de e e i, qu/k, h mudo, m/n finais, letras dobradas).
     */
    static String chaveFonetica(String palavra) {
        String s = palavra;
        s = s.replace("ph", "f").replace("th", "t").replace("ck", "k").replace("sh", "x").replace("ch", "x");
        s = s.replaceAll("qu(?=[ei])", "k").replaceAll("gu(?=[ei])", "g").replace("qu", "ku");
        s = s.replaceAll("sc(?=[ei])", "s").replaceAll("c(?=[ei])", "s").replace("c", "k");
        s = s.replaceAll("g(?=[ei])", "j").replace("z", "s").replace("h", "").replace("w", "v").replace("y", "i");
        s = s.replaceAll("[mn]$", "").replaceAll("m(?=[bp])", "n");
        s = s.replaceAll("(.)\\1+", "$1");
        s = s.replaceAll("e$", "i").replaceAll("o$", "u").replaceAll("l(?=[^aeiou]|$)", "u");
        return s;
    }

    /**
     * Verifica se todas as palavras da consulta aparecem no texto, tolerando erros de digitação.
     * Palavras curtas (até 3 letras) precisam coincidir exatamente.
     */
    public static boolean correspondeBusca(String consulta, String texto) {
        String c = normalizar(consulta == null ? "" : consulta);
        if (c.isEmpty()) return true;
        String t = normalizar(texto == null ? "" : texto);
        String[] palavrasTexto = t.split(" ");
        for (String q : c.split(" ")) {
            if (t.contains(q)) continue;
            int limite = limite(q.length());
            boolean achou = false;
            if (limite > 0) {
                for (String p : palavrasTexto) {
                    if (!p.isEmpty() && p.charAt(0) == q.charAt(0) && Math.abs(p.length() - q.length()) <= limite && distancia(q, p) <= limite) { achou = true; break; }
                }
            }
            if (!achou) return false;
        }
        return true;
    }

    /** Minúsculas, sem acentos e sem espaços repetidos. */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[-_]+", " ").replaceAll("\\s+", " ").trim();
    }

    /** Distância de Damerau-Levenshtein restrita: inserção, remoção, substituição e troca de letras vizinhas. */
    public static int distancia(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + custo);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1))
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
            }
        }
        return d[n][m];
    }

    /** Quantidade de erros tolerada conforme o tamanho da palavra. */
    static int limite(int tamanho) {
        if (tamanho <= 3) return 0;
        if (tamanho <= 4) return 1;
        return 2;
    }

    /**
     * Palavra do vocabulário mais próxima da digitada. Em caso de empate na distância, vence a palavra que
     * aparece em mais ingredientes (por exemplo, "farinha" sobre "farofa"); se o empate persistir, não corrige.
     */
    private static String palavraMaisProxima(String palavra, Map<String, Integer> pesos) {
        int limite = limite(palavra.length());
        int melhorDistancia = Integer.MAX_VALUE;
        List<String> melhores = new ArrayList<>();
        for (String candidata : pesos.keySet()) {
            if (candidata.charAt(0) != palavra.charAt(0) || Math.abs(candidata.length() - palavra.length()) > limite) continue;
            int d = distancia(palavra, candidata);
            if (d > limite) continue;
            if (d < melhorDistancia) { melhorDistancia = d; melhores.clear(); melhores.add(candidata); }
            else if (d == melhorDistancia) melhores.add(candidata);
        }
        if (melhores.size() <= 1) return melhores.isEmpty() ? null : melhores.get(0);
        int maiorPeso = melhores.stream().mapToInt(pesos::get).max().orElse(0);
        List<String> populares = melhores.stream().filter(m -> pesos.get(m) == maiorPeso).toList();
        return populares.size() == 1 ? populares.get(0) : null;
    }

    /** Nome normalizado para a grafia canônica; os ingredientes cadastrados têm prioridade sobre a lista comum. */
    private Map<String, String> mapaConhecidos() {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (Ingrediente ing : ingredientes.findAll()) mapa.putIfAbsent(normalizar(ing.getNome()), ing.getNome());
        for (String nome : comuns) mapa.putIfAbsent(normalizar(nome), nome);
        return mapa;
    }

    /** Palavra normalizada para a palavra com acento, a partir dos nomes conhecidos. */
    private static Map<String, String> mapaPalavras(Iterable<String> nomes, Map<String, Integer> pesos) {
        Map<String, String> palavras = new LinkedHashMap<>();
        for (String nome : nomes) {
            Set<String> destaNome = new HashSet<>();
            for (String p : nome.toLowerCase(Locale.ROOT).split("[\\s-]+")) {
                String n = normalizar(p);
                if (n.length() < 3 || LIGACOES.contains(n)) continue;
                palavras.putIfAbsent(n, p);
                if (destaNome.add(n)) pesos.merge(n, 1, Integer::sum);
            }
        }
        return palavras;
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static List<String> carregarComuns() {
        List<String> nomes = new ArrayList<>();
        try (InputStream in = CorretorIngredientes.class.getResourceAsStream("/ingredientes-comuns.txt")) {
            if (in == null) return nomes;
            try (BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitor.readLine()) != null) if (!linha.isBlank()) nomes.add(linha.trim());
            }
        } catch (IOException ignorada) {
            // Sem a lista comum, o corretor usa apenas os ingredientes cadastrados.
        }
        return nomes;
    }
}
