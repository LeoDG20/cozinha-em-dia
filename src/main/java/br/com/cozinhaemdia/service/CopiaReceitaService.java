package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Cria cópias independentes de uma receita (campos, ingredientes, etapas e arquivos de mídia).
 * É usada tanto ao salvar uma receita compartilhada quanto ao criar uma versão adaptada.
 */
@Service
public class CopiaReceitaService {
    static final int LIMITE_NOME = 120;

    private final ReceitaRepository receitas;
    private final ReceitaIngredienteRepository receitaIngredientes;
    private final ArmazenamentoArquivos arquivos;

    public CopiaReceitaService(ReceitaRepository receitas, ReceitaIngredienteRepository receitaIngredientes, ArmazenamentoArquivos arquivos) {
        this.receitas = receitas; this.receitaIngredientes = receitaIngredientes; this.arquivos = arquivos;
    }

    /** Copia a receita para a conta do destinatário mantendo o nome original. */
    @Transactional
    public Receita copiar(Receita origem, Usuario destinatario) {
        return copiar(origem, destinatario, origem.getNome());
    }

    /**
     * Cria uma versão adaptada da receita, com o nome informado, na conta do usuário.
     * Tudo é copiado para que a pessoa altere apenas o que desejar; a receita de origem não é modificada.
     */
    @Transactional
    public Receita adaptar(Receita origem, Usuario usuario, String novoNome) {
        String nome = novoNome == null ? "" : novoNome.trim().replaceAll("\\s+", " ");
        if (nome.isEmpty()) throw new IllegalArgumentException("Informe um nome para a versão adaptada.");
        if (nome.length() > LIMITE_NOME) throw new IllegalArgumentException("O nome da receita deve ter no máximo " + LIMITE_NOME + " caracteres.");
        if (receitas.existsByUsuarioIdAndNomeIgnoreCase(usuario.getId(), nome))
            throw new IllegalArgumentException("Você já tem uma receita chamada \"" + nome + "\". Escolha outro nome para a versão adaptada.");
        Receita copia = copiar(origem, usuario, nome);
        copia.setOrigemNome(origem.getNome());
        return receitas.save(copia);
    }

    private Receita copiar(Receita origem, Usuario destinatario, String nome) {
        Receita copia = new Receita();
        copia.setUsuario(destinatario);
        copia.setNome(nome);
        copia.setDescricao(origem.getDescricao());
        copia.setModoPreparo(origem.getModoPreparo());
        copia.setTempoPreparoMin(origem.getTempoPreparoMin());
        copia.setPorcoes(origem.getPorcoes());
        copia.setImagemUrl(origem.getImagemUrl());
        copia.setImagemArquivo(origem.getImagemArquivo() == null ? null : arquivos.copiar(origem.getImagemArquivo()));
        copia.setVideoUrl(origem.getVideoUrl());
        copia.setVideoArquivo(origem.getVideoArquivo() == null ? null : arquivos.copiar(origem.getVideoArquivo()));
        for (EtapaReceita etapa : origem.getEtapas()) {
            String foto = etapa.getImagemArquivo() == null ? null : arquivos.copiar(etapa.getImagemArquivo());
            copia.getEtapas().add(new EtapaReceita(copia, etapa.getOrdem(), etapa.getDescricao(), foto));
        }
        Receita salva = receitas.save(copia);

        List<ReceitaIngrediente> itens = new ArrayList<>();
        for (ReceitaIngrediente item : receitaIngredientes.findAllByReceitaIdOrderByIdAsc(origem.getId()))
            itens.add(new ReceitaIngrediente(salva, item.getIngrediente(), item.getQuantidade(), item.getUnidade()));
        receitaIngredientes.saveAll(itens);
        return salva;
    }
}
