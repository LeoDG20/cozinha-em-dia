package br.com.cozinhaemdia.config;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dados de demonstração: conta de exemplo, receitas com fotografia e despensa inicial.
 * A carga é idempotente: receitas já existentes (pelo nome) não são duplicadas e a despensa só é preenchida quando está vazia.
 */
@Configuration
public class DataInitializer {
    /** As fotografias são arquivos de licença livre do Wikimedia Commons, citados no README. */
    private static final String BASE_FOTOS = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    @Bean
    CommandLineRunner inicializarDados(UsuarioRepository usuarios, IngredienteRepository ingredientes,
        ReceitaRepository receitas, ReceitaIngredienteRepository receitaIngredientes,
        DespensaRepository despensa, PasswordEncoder encoder) {
        return args -> {
            Usuario demo = usuarios.findByEmailIgnoreCase("demo@cozinha.local").orElse(null);
            if (demo == null) demo = usuarios.save(new Usuario("Conta demonstração", "demo@cozinha.local", encoder.encode("demo123")));
            else if ("INIT_DEMO123".equals(demo.getSenha())) { demo.setSenha(encoder.encode("demo123")); usuarios.save(demo); }

            Map<String, Ingrediente> mapa = new HashMap<>();
            for (Modelo m : catalogo()) {
                if (receitas.existsByUsuarioIdAndNomeIgnoreCase(demo.getId(), m.nome())) continue;
                criarReceita(demo, receitas, receitaIngredientes, ingredientes, mapa, m);
            }
            for (Receita r : receitas.findAllByUsuarioIdOrderByNomeAsc(demo.getId())) {
                if (r.getImagemUrl() != null || r.getImagemArquivo() != null) continue;
                for (Modelo m : catalogo()) if (m.nome().equalsIgnoreCase(r.getNome())) { r.setImagemUrl(foto(m.arquivoFoto())); receitas.save(r); }
            }
            if (despensa.findAllByUsuarioIdOrderByIngredienteNomeAsc(demo.getId()).isEmpty()) preencherDespensa(demo, ingredientes, despensa, mapa);
        };
    }

    private void preencherDespensa(Usuario usuario, IngredienteRepository ingredientes, DespensaRepository despensa, Map<String, Ingrediente> mapa) {
        Object[][] estoque = {
            {"Ovo", 6, "unidade"}, {"Queijo", 200, "g"}, {"Azeite", 100, "ml"}, {"Sal", 200, "g"}, {"Macarrão", 500, "g"},
            {"Alho", 6, "dente"}, {"Arroz", 1000, "g"}, {"Frango", 250, "g"}, {"Cebola", 2, "unidade"}, {"Leite", 500, "ml"},
            {"Batata", 4, "unidade"}, {"Tomate", 3, "unidade"}, {"Banana", 1, "unidade"}
        };
        for (Object[] e : estoque) {
            Ingrediente ing = ingrediente(ingredientes, mapa, (String) e[0]);
            despensa.save(new Despensa(usuario, ing, ((Number) e[1]).doubleValue(), (String) e[2]));
        }
    }

    private void criarReceita(Usuario u, ReceitaRepository receitas, ReceitaIngredienteRepository relacoes, IngredienteRepository ingredientes,
                              Map<String, Ingrediente> mapa, Modelo m) {
        Receita r = new Receita();
        r.setUsuario(u); r.setNome(m.nome()); r.setDescricao(m.descricao()); r.setModoPreparo(m.modoPreparo());
        r.setTempoPreparoMin(m.tempo()); r.setPorcoes(m.porcoes()); r.setImagemUrl(foto(m.arquivoFoto()));
        receitas.save(r);
        List<ReceitaIngrediente> linhas = new ArrayList<>();
        for (Item i : m.itens()) linhas.add(new ReceitaIngrediente(r, ingrediente(ingredientes, mapa, i.nome()), i.quantidade(), i.unidade()));
        relacoes.saveAll(linhas);
    }

    private Ingrediente ingrediente(IngredienteRepository repo, Map<String, Ingrediente> mapa, String nome) {
        return mapa.computeIfAbsent(nome.toLowerCase(Locale.ROOT), k -> repo.findByNomeIgnoreCase(nome).orElseGet(() -> repo.save(new Ingrediente(nome))));
    }

    private static String foto(String arquivo) {
        return BASE_FOTOS + URLEncoder.encode(arquivo.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20") + "?width=900";
    }

    private static Item item(String nome, double quantidade, String unidade) { return new Item(nome, quantidade, unidade); }

    private record Item(String nome, double quantidade, String unidade) {}
    private record Modelo(String nome, String descricao, String modoPreparo, int tempo, int porcoes, String arquivoFoto, Item... itens) {}

    private static List<Modelo> catalogo() {
        return List.of(
            new Modelo("Omelete de queijo", "Uma opção rápida para café da manhã ou jantar.",
                "1. Bata os ovos com uma pitada de sal.\n2. Aqueça o azeite em uma frigideira.\n3. Despeje os ovos, acrescente o queijo e cozinhe até firmar.",
                10, 1, "Omelet With Fixings.jpg",
                item("Ovo", 3, "unidade"), item("Queijo", 40, "g"), item("Azeite", 5, "ml"), item("Sal", 2, "g")),
            new Modelo("Macarrão alho e óleo", "Clássico simples para o dia a dia.",
                "1. Cozinhe o macarrão em água com sal.\n2. Doure o alho no azeite sem queimar.\n3. Misture o macarrão escorrido e sirva.",
                20, 2, "Spaghetti aglio e olio KB.jpg",
                item("Macarrão", 200, "g"), item("Alho", 2, "dente"), item("Azeite", 20, "ml"), item("Sal", 4, "g")),
            new Modelo("Frango com arroz", "Uma refeição completa com poucos ingredientes.",
                "1. Tempere o frango com alho e sal.\n2. Doure a cebola e o frango em um fio de azeite.\n3. Prepare o arroz e sirva junto.",
                35, 2, "Liat Portal for Foodie Disorder - Grilled chicken with rice and vegetables.jpg",
                item("Frango", 350, "g"), item("Arroz", 180, "g"), item("Alho", 1, "dente"), item("Cebola", 1, "unidade"), item("Azeite", 10, "ml"), item("Sal", 3, "g")),
            new Modelo("Panqueca simples", "Massa versátil para recheios doces ou salgados.",
                "1. Misture a farinha, o leite e os ovos até ficar homogêneo.\n2. Aqueça uma frigideira untada.\n3. Coloque uma concha de massa, doure dos dois lados e recheie.",
                25, 2, "Foodiesfeed.com pouring-honey-on-pancakes-with-walnuts.jpg",
                item("Farinha de trigo", 150, "g"), item("Leite", 250, "ml"), item("Ovo", 2, "unidade"), item("Sal", 2, "g")),
            new Modelo("Bolo de banana e aveia", "Uma receita para aproveitar bananas maduras.",
                "1. Amasse as bananas e misture os ovos e o leite.\n2. Incorpore a aveia e o fermento.\n3. Leve ao forno preaquecido a 180 °C até dourar.",
                35, 4, "Banana bread cut into slices.jpg",
                item("Banana", 3, "unidade"), item("Aveia", 120, "g"), item("Ovo", 2, "unidade"), item("Leite", 100, "ml"), item("Fermento químico", 1, "colher")),
            new Modelo("Salada de lentilha", "Uma opção fresca e simples para variar o cardápio.",
                "1. Cozinhe a lentilha até ficar macia.\n2. Misture com a cenoura ralada e o coentro picado.\n3. Tempere a gosto e sirva fria.",
                30, 2, "Lentil salad (29308018298).jpg",
                item("Lentilha", 200, "g"), item("Cenoura", 1, "unidade"), item("Coentro", 10, "g")),
            new Modelo("Pão de queijo", "Receita mineira de massa macia por dentro e crocante por fora.",
                "1. Ferva o leite com o óleo e o sal e despeje sobre o polvilho, mexendo até escaldar.\n2. Deixe amornar, junte os ovos e o queijo ralado e sove até a massa ficar lisa.\n3. Modele bolinhas com as mãos untadas e disponha em uma assadeira.\n4. Asse em forno a 200 °C por cerca de 25 minutos, até dourar.",
                40, 6, "Pao de queijo brasil.jpg",
                item("Polvilho", 250, "g"), item("Queijo ralado", 150, "g"), item("Leite", 150, "ml"), item("Óleo", 80, "ml"), item("Ovo", 2, "unidade"), item("Sal", 5, "g")),
            new Modelo("Brigadeiro", "Doce de festa preparado em uma única panela.",
                "1. Misture o leite condensado, o chocolate em pó e a manteiga em uma panela.\n2. Cozinhe em fogo baixo, mexendo sempre, até o fundo da panela aparecer.\n3. Deixe esfriar, modele as bolinhas e passe no granulado.",
                25, 20, "Brigadeiros.jpg",
                item("Leite condensado", 395, "g"), item("Chocolate em pó", 30, "g"), item("Manteiga", 10, "g")),
            new Modelo("Bolo de cenoura", "Bolo fofo com cobertura de chocolate, ideal para o lanche da tarde.",
                "1. Bata no liquidificador a cenoura, os ovos e o óleo.\n2. Misture com o açúcar e a farinha e acrescente o fermento por último.\n3. Asse em forma untada a 180 °C por cerca de 40 minutos.\n4. Cozinhe o chocolate em pó com o açúcar e o leite até engrossar e cubra o bolo.",
                60, 10, "Bolo de cenoura com chocolate e granulado.jpg",
                item("Cenoura", 3, "unidade"), item("Ovo", 3, "unidade"), item("Óleo", 120, "ml"), item("Açúcar", 300, "g"),
                item("Farinha de trigo", 300, "g"), item("Fermento químico", 1, "colher"), item("Chocolate em pó", 4, "colher"), item("Leite", 100, "ml")),
            new Modelo("Arroz e feijão", "A base da refeição brasileira, com tempero simples.",
                "1. Deixe o feijão de molho por algumas horas e cozinhe na pressão até amaciar.\n2. Refogue metade da cebola e do alho no óleo e junte ao feijão com sal.\n3. Refogue o restante da cebola e do alho, acrescente o arroz lavado e cubra com água quente.\n4. Cozinhe tampado até secar a água.",
                60, 4, "Prato com arroz e feijão, frango grelhado, batata frita, salada e ovo frito como adicionais.jpg",
                item("Arroz", 200, "g"), item("Feijão", 250, "g"), item("Cebola", 1, "unidade"), item("Alho", 3, "dente"), item("Óleo", 20, "ml"), item("Sal", 6, "g")),
            new Modelo("Lasanha de presunto e queijo", "Camadas de massa, presunto, queijo e molho branco.",
                "1. Prepare o molho branco com manteiga, farinha de trigo e leite, mexendo até engrossar.\n2. Cozinhe a massa conforme a embalagem.\n3. Monte camadas de massa, extrato de tomate, presunto, queijo e molho branco.\n4. Cubra com queijo e leve ao forno a 200 °C por 25 minutos.",
                60, 6, "Lasagna 2022.jpg",
                item("Macarrão", 250, "g"), item("Presunto", 200, "g"), item("Queijo", 200, "g"), item("Extrato de tomate", 3, "colher"),
                item("Leite", 500, "ml"), item("Farinha de trigo", 3, "colher"), item("Manteiga", 30, "g")),
            new Modelo("Sopa de legumes", "Prato leve e nutritivo, com legumes que costumam sobrar na geladeira.",
                "1. Pique a batata, a cenoura, o chuchu e a cebola em cubos.\n2. Refogue a cebola e o alho no azeite.\n3. Junte os legumes, cubra com água e cozinhe até ficarem macios.\n4. Tempere com sal e sirva quente.",
                40, 4, "Traditional Vegetable Soup from Argentina.jpg",
                item("Batata", 2, "unidade"), item("Cenoura", 2, "unidade"), item("Chuchu", 1, "unidade"), item("Cebola", 1, "unidade"),
                item("Alho", 2, "dente"), item("Azeite", 15, "ml"), item("Sal", 4, "g")),
            new Modelo("Purê de batata", "Acompanhamento cremoso para carnes e frango.",
                "1. Cozinhe as batatas descascadas em água com sal até ficarem macias.\n2. Escorra e amasse ainda quentes.\n3. Acrescente a manteiga e o leite aquecido e misture até obter um creme liso.",
                30, 4, "MashedPotatoes.jpg",
                item("Batata", 5, "unidade"), item("Leite", 150, "ml"), item("Manteiga", 30, "g"), item("Sal", 4, "g")),
            new Modelo("Farofa de ovo", "Acompanhamento rápido, ótimo com arroz e feijão.",
                "1. Doure a cebola na manteiga.\n2. Junte os ovos e mexa até cozinharem.\n3. Acrescente a farinha de mandioca aos poucos, tempere com sal e mexa até ficar soltinha.",
                15, 4, "Farofa de ovo.jpg",
                item("Farinha de mandioca", 200, "g"), item("Ovo", 3, "unidade"), item("Manteiga", 40, "g"), item("Cebola", 1, "unidade"), item("Sal", 3, "g")),
            new Modelo("Pudim de leite", "Sobremesa clássica com calda de caramelo.",
                "1. Derreta o açúcar até formar um caramelo e espalhe na forma.\n2. Bata no liquidificador o leite condensado, o leite e os ovos.\n3. Despeje na forma, cubra com papel alumínio e cozinhe em banho-maria por cerca de 50 minutos.\n4. Deixe esfriar e leve à geladeira antes de desenformar.",
                70, 8, "Pudim de leite (3544235183).jpg",
                item("Leite condensado", 395, "g"), item("Leite", 500, "ml"), item("Ovo", 3, "unidade"), item("Açúcar", 150, "g")),
            new Modelo("Strogonoff de carne", "Prato cremoso que combina bem com arroz e batata palha.",
                "1. Corte a carne em tiras e tempere com sal.\n2. Doure a carne com a cebola e junte o champignon.\n3. Acrescente o extrato de tomate e a mostarda e cozinhe por alguns minutos.\n4. Desligue o fogo, misture o creme de leite e sirva com arroz.",
                40, 4, "Beef Stroganoff-03.jpg",
                item("Filé mignon", 500, "g"), item("Cebola", 1, "unidade"), item("Champignon", 100, "g"), item("Extrato de tomate", 3, "colher"),
                item("Mostarda", 1, "colher"), item("Creme de leite", 200, "g"), item("Arroz", 200, "g"))
        );
    }
}
