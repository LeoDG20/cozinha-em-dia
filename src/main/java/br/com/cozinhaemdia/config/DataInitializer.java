package br.com.cozinhaemdia.config;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner inicializarDados(UsuarioRepository usuarios, IngredienteRepository ingredientes,
        ReceitaRepository receitas, ReceitaIngredienteRepository receitaIngredientes,
        DespensaRepository despensa, PasswordEncoder encoder) {
        return args -> {
            Usuario demo = usuarios.findByEmailIgnoreCase("demo@cozinha.local").orElse(null);
            if (demo == null) demo = usuarios.save(new Usuario("Conta demonstração", "demo@cozinha.local", encoder.encode("demo123")));
            else if ("INIT_DEMO123".equals(demo.getSenha())) { demo.setSenha(encoder.encode("demo123")); usuarios.save(demo); }
            if (receitas.countByUsuarioId(demo.getId()) == 0) preencherExemplo(demo, ingredientes, receitas, receitaIngredientes, despensa);
        };
    }

    private void preencherExemplo(Usuario usuario, IngredienteRepository ingredientes, ReceitaRepository receitas,
                                  ReceitaIngredienteRepository receitaIngredientes, DespensaRepository despensa) {
        Map<String, Ingrediente> mapa = new LinkedHashMap<>();
        String[] nomes = {"Ovo", "Queijo", "Azeite", "Sal", "Macarrão", "Alho", "Arroz", "Frango", "Cebola", "Leite", "Batata", "Tomate", "Banana", "Farinha de trigo", "Aveia", "Fermento químico", "Cenoura", "Manteiga", "Lentilha", "Coentro"};
        for (String nome : nomes) mapa.put(nome.toLowerCase(Locale.ROOT), ingredientes.findByNomeIgnoreCase(nome).orElseGet(() -> ingredientes.save(new Ingrediente(nome))));
        adicionarEstoque(usuario, mapa, despensa, "Ovo", 6, "unidade");
        adicionarEstoque(usuario, mapa, despensa, "Queijo", 200, "g");
        adicionarEstoque(usuario, mapa, despensa, "Azeite", 100, "ml");
        adicionarEstoque(usuario, mapa, despensa, "Sal", 200, "g");
        adicionarEstoque(usuario, mapa, despensa, "Macarrão", 500, "g");
        adicionarEstoque(usuario, mapa, despensa, "Alho", 6, "dente");
        adicionarEstoque(usuario, mapa, despensa, "Arroz", 1000, "g");
        adicionarEstoque(usuario, mapa, despensa, "Frango", 250, "g");
        adicionarEstoque(usuario, mapa, despensa, "Cebola", 2, "unidade");
        adicionarEstoque(usuario, mapa, despensa, "Leite", 500, "ml");
        adicionarEstoque(usuario, mapa, despensa, "Batata", 4, "unidade");
        adicionarEstoque(usuario, mapa, despensa, "Tomate", 3, "unidade");
        adicionarEstoque(usuario, mapa, despensa, "Banana", 1, "unidade");

        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Omelete de queijo", "Uma opção rápida para café da manhã ou jantar.", "1. Bata os ovos com uma pitada de sal.\n2. Aqueça o azeite em uma frigideira.\n3. Despeje os ovos, acrescente o queijo e cozinhe até firmar.", 10, 1,
            item("Ovo",3,"unidade"), item("Queijo",40,"g"), item("Azeite",5,"ml"), item("Sal",2,"g"));
        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Macarrão alho e óleo", "Clássico simples para o dia a dia.", "1. Cozinhe o macarrão em água com sal.\n2. Doure o alho no azeite sem queimar.\n3. Misture o macarrão escorrido e sirva.", 20, 2,
            item("Macarrão",200,"g"), item("Alho",2,"dente"), item("Azeite",20,"ml"), item("Sal",4,"g"));
        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Frango com arroz", "Uma refeição completa com poucos ingredientes.", "1. Tempere o frango com alho e sal.\n2. Doure a cebola e o frango em um fio de azeite.\n3. Prepare o arroz e sirva junto.", 35, 2,
            item("Frango",350,"g"), item("Arroz",180,"g"), item("Alho",1,"dente"), item("Cebola",1,"unidade"), item("Azeite",10,"ml"), item("Sal",3,"g"));
        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Panqueca simples", "Massa versátil para recheios doces ou salgados.", "1. Misture a farinha, o leite e os ovos até ficar homogêneo.\n2. Aqueça uma frigideira untada.\n3. Coloque uma concha de massa, doure dos dois lados e recheie.", 25, 2,
            item("Farinha de trigo",150,"g"), item("Leite",250,"ml"), item("Ovo",2,"unidade"), item("Sal",2,"g"));
        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Bolo de banana e aveia", "Uma receita para aproveitar bananas maduras.", "1. Amasse as bananas e misture os ovos e o leite.\n2. Incorpore a aveia e o fermento.\n3. Leve ao forno preaquecido a 180 °C até dourar.", 35, 4,
            item("Banana",3,"unidade"), item("Aveia",120,"g"), item("Ovo",2,"unidade"), item("Leite",100,"ml"), item("Fermento químico",1,"colher"));
        criarReceita(usuario, receitas, receitaIngredientes, mapa, "Salada de lentilha", "Uma opção fresca e simples para variar o cardápio.", "1. Cozinhe a lentilha até ficar macia.\n2. Misture com a cenoura ralada e o coentro picado.\n3. Tempere a gosto e sirva fria.", 30, 2,
            item("Lentilha",200,"g"), item("Cenoura",1,"unidade"), item("Coentro",10,"g"));
    }
    private Item item(String nome, double quantidade, String unidade) { return new Item(nome, quantidade, unidade); }
    private void adicionarEstoque(Usuario u, Map<String, Ingrediente> mapa, DespensaRepository repo, String nome, double qtd, String unidade) {
        repo.save(new Despensa(u, mapa.get(nome.toLowerCase(Locale.ROOT)), qtd, unidade));
    }
    private void criarReceita(Usuario u, ReceitaRepository receitas, ReceitaIngredienteRepository relacoes, Map<String, Ingrediente> mapa,
                              String nome, String descricao, String modo, int tempo, int porcoes, Item... itens) {
        Receita r = new Receita(); r.setUsuario(u); r.setNome(nome); r.setDescricao(descricao); r.setModoPreparo(modo); r.setTempoPreparoMin(tempo); r.setPorcoes(porcoes);
        receitas.save(r); List<ReceitaIngrediente> linhas = new ArrayList<>();
        for (Item i : itens) linhas.add(new ReceitaIngrediente(r, mapa.get(i.nome().toLowerCase(Locale.ROOT)), i.quantidade(), i.unidade()));
        relacoes.saveAll(linhas);
    }
    private record Item(String nome, double quantidade, String unidade) {}
}
