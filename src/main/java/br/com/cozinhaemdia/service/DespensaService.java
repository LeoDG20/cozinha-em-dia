package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DespensaService {
    private final DespensaRepository despensa;
    private final IngredienteRepository ingredientes;
    public DespensaService(DespensaRepository despensa, IngredienteRepository ingredientes) { this.despensa = despensa; this.ingredientes = ingredientes; }
    public List<Despensa> listar(Long usuarioId) { return despensa.findAllByUsuarioIdOrderByIngredienteNomeAsc(usuarioId); }
    @Transactional
    public void adicionar(Usuario usuario, String nome, Double quantidade, String unidade) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Informe o nome do ingrediente.");
        if (quantidade == null || quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        if (unidade == null || unidade.isBlank()) throw new IllegalArgumentException("Selecione uma unidade.");
        Ingrediente ing = ingredientes.findByNomeIgnoreCase(nome.trim()).orElseGet(() -> ingredientes.save(new Ingrediente(nome.trim())));
        Despensa existente = despensa.findByUsuarioIdAndIngredienteId(usuario.getId(), ing.getId()).orElse(null);
        if (existente == null) despensa.save(new Despensa(usuario, ing, quantidade, unidade.trim().toLowerCase()));
        else {
            if (!existente.getUnidade().equalsIgnoreCase(unidade.trim())) throw new IllegalArgumentException("Esse ingrediente já está cadastrado em '" + existente.getUnidade() + "'. Remova-o antes de usar outra unidade.");
            existente.setQuantidade(existente.getQuantidade() + quantidade);
            despensa.save(existente);
        }
    }
    @Transactional
    public void remover(Long itemId, Long usuarioId) {
        Despensa item = despensa.findByIdAndUsuarioId(itemId, usuarioId).orElseThrow(() -> new IllegalArgumentException("Item não encontrado na sua despensa."));
        despensa.delete(item);
    }
}
