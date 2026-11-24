package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.service.PreparoService.Etapa;
import br.com.cozinhaemdia.service.PreparoService.Midia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Grava uma receita completa do formulário (dados, ingredientes, etapas, imagem e vídeos) em uma única transação.
 * Antes de gravar, valida os arquivos e corrige nomes de ingredientes digitados com erro.
 */
@Service
public class RegistroReceitaService {
    /** Campos de texto do formulário. Os ingredientes chegam em linhas separadas, como no formulário. */
    public record Dados(String nome, String descricao, Integer tempoPreparoMin, Integer porcoes,
                        List<String> ingredienteNomes, List<String> ingredienteQuantidades, List<String> ingredienteUnidades) {}

    /** Receita gravada e avisos sobre correções automáticas feitas nos ingredientes. */
    public record Resultado(Receita receita, List<String> avisos) {}

    private final ReceitaService receitas;
    private final PreparoService preparo;
    private final CorretorIngredientes corretor;

    public RegistroReceitaService(ReceitaService receitas, PreparoService preparo, CorretorIngredientes corretor) {
        this.receitas = receitas; this.preparo = preparo; this.corretor = corretor;
    }

    @Transactional
    public Resultado criar(Usuario usuario, Dados dados, Midia midia, List<Etapa> etapas) {
        preparo.validar(midia, etapas);
        CorretorIngredientes.TextoCorrigido ingredientes = corretor.corrigirLinhas(montarIngredientes(dados));
        Receita receita = receitas.criar(usuario, dados.nome(), dados.descricao(), PreparoService.montarModoPreparo(etapas),
                dados.tempoPreparoMin(), dados.porcoes(), ingredientes.texto());
        preparo.aplicar(receita, midia, etapas);
        return new Resultado(receita, ingredientes.avisos());
    }

    @Transactional
    public Resultado atualizar(Receita receita, Usuario usuario, Dados dados, Midia midia, List<Etapa> etapas) {
        preparo.validar(midia, etapas);
        CorretorIngredientes.TextoCorrigido ingredientes = corretor.corrigirLinhas(montarIngredientes(dados));
        Receita salva = receitas.atualizar(receita, usuario, dados.nome(), dados.descricao(), PreparoService.montarModoPreparo(etapas),
                dados.tempoPreparoMin(), dados.porcoes(), ingredientes.texto());
        preparo.aplicar(salva, midia, etapas);
        return new Resultado(salva, ingredientes.avisos());
    }

    /** Remove a receita e, em seguida, os arquivos de mídia que pertenciam a ela. */
    @Transactional
    public void excluir(Receita receita) {
        List<String> arquivos = new ArrayList<>();
        if (receita.getImagemArquivo() != null) arquivos.add(receita.getImagemArquivo());
        if (receita.getVideoArquivo() != null) arquivos.add(receita.getVideoArquivo());
        receita.getEtapas().forEach(e -> { if (e.getImagemArquivo() != null) arquivos.add(e.getImagemArquivo()); });
        receitas.excluir(receita);
        arquivos.forEach(preparo::removerArquivo);
    }

    /** Converte as linhas do formulário no formato "Nome | quantidade | unidade" ignorando linhas totalmente vazias. */
    static String montarIngredientes(Dados dados) {
        StringBuilder texto = new StringBuilder();
        int linhas = Math.max(dados.ingredienteNomes().size(), Math.max(dados.ingredienteQuantidades().size(), dados.ingredienteUnidades().size()));
        for (int i = 0; i < linhas; i++) {
            String nome = item(dados.ingredienteNomes(), i), qtd = item(dados.ingredienteQuantidades(), i), un = item(dados.ingredienteUnidades(), i);
            if (nome.isEmpty() && qtd.isEmpty() && un.isEmpty()) continue;
            if (texto.length() > 0) texto.append('\n');
            texto.append(nome.replace("|", " ")).append(" | ").append(qtd).append(" | ").append(un);
        }
        return texto.toString();
    }

    private static String item(List<String> lista, int indice) {
        return indice < lista.size() && lista.get(indice) != null ? lista.get(indice).trim() : "";
    }
}
