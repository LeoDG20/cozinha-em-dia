package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.*;
import br.com.cozinhaemdia.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DespensaService {
    private final DespensaRepository despensa;
    private final IngredienteRepository ingredientes;
    @Autowired
    public DespensaService(DespensaRepository despensa, IngredienteRepository ingredientes) { this.despensa = despensa; this.ingredientes = ingredientes; }
    public List<Despensa> listar(Long usuarioId) { return despensa.findAllByUsuarioIdOrderByIngredienteNomeAsc(usuarioId); }
    public long contar(Long usuarioId) { return despensa.countByUsuarioId(usuarioId); }
    @Transactional
    public void adicionar(Usuario usuario, String nome, Double quantidade, String unidade) {
        if (nome == null || nome.isBlank()) throw new NegocioException("erro.despensa.nome.vazio", "Informe o nome do ingrediente.");
        if (quantidade == null || quantidade <= 0) throw new NegocioException("erro.quantidade.positiva", "A quantidade deve ser maior que zero.");
        if (unidade == null || unidade.isBlank()) throw new NegocioException("erro.unidade.vazia", "Selecione uma unidade.");
        Ingrediente ing = ingredientes.findByNomeIgnoreCase(nome.trim()).orElseGet(() -> ingredientes.save(new Ingrediente(nome.trim())));
        Despensa existente = despensa.findByUsuarioIdAndIngredienteId(usuario.getId(), ing.getId()).orElse(null);
        if (existente == null) despensa.save(new Despensa(usuario, ing, quantidade, ConversorUnidades.normalizar(unidade)));
        else {
            // A quantidade informada é convertida para a unidade já cadastrada (por exemplo, 1 kg somado a 200 g).
            double somar = ConversorUnidades.converter(quantidade, unidade, existente.getUnidade(), ing.getNome()).orElseThrow(() ->
                    new NegocioException("erro.despensa.unidade.incompativel", "Esse ingrediente já está cadastrado em '" + existente.getUnidade() + "', que não é compatível com '" + unidade.trim() + "'. Use uma unidade equivalente ou ajuste o item pela edição.", existente.getUnidade(), unidade.trim()));
            existente.setQuantidade(existente.getQuantidade() + somar);
            despensa.save(existente);
        }
    }
    @Transactional
    public void atualizar(Long itemId, Long usuarioId, Double quantidade, String unidade) {
        if (quantidade == null || quantidade <= 0) throw new NegocioException("erro.quantidade.positiva", "A quantidade deve ser maior que zero.");
        if (unidade == null || unidade.isBlank()) throw new NegocioException("erro.unidade.vazia", "Selecione uma unidade.");
        Despensa item = despensa.findByIdAndUsuarioId(itemId, usuarioId).orElseThrow(() -> new NegocioException("erro.despensa.item.nao-encontrado", "Item não encontrado na sua despensa."));
        item.setQuantidade(quantidade);
        item.setUnidade(ConversorUnidades.normalizar(unidade));
        despensa.save(item);
    }
    @Transactional
    public void remover(Long itemId, Long usuarioId) {
        Despensa item = despensa.findByIdAndUsuarioId(itemId, usuarioId).orElseThrow(() -> new NegocioException("erro.despensa.item.nao-encontrado", "Item não encontrado na sua despensa."));
        despensa.delete(item);
    }
}
