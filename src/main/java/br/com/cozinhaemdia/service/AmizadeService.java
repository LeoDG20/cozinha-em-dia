package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Amizade;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.AmizadeRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Amizades entre usuários. A pessoa envia um pedido pelo e-mail e a outra aceita ou recusa.
 * Receitas só podem ser compartilhadas entre amigos.
 */
@Service
public class AmizadeService {
    /** Amigo com o identificador da amizade, necessário para desfazê-la. */
    public record Amigo(Long amizadeId, Usuario usuario) {}

    private final AmizadeRepository amizades;
    private final UsuarioRepository usuarios;

    @Autowired
    public AmizadeService(AmizadeRepository amizades, UsuarioRepository usuarios) {
        this.amizades = amizades; this.usuarios = usuarios;
    }

    /**
     * Envia um pedido de amizade. Se a outra pessoa já tinha enviado um pedido para o usuário, a amizade é confirmada
     * na hora. Devolve a amizade resultante (pendente ou aceita).
     */
    @Transactional
    public Amizade convidar(Usuario eu, String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Informe o e-mail do amigo.");
        Usuario outro = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Não há usuário cadastrado com esse e-mail. Peça para a pessoa criar uma conta primeiro."));
        if (outro.getId().equals(eu.getId())) throw new IllegalArgumentException("Você não pode adicionar a si mesmo.");
        if (amizades.saoAmigos(eu.getId(), outro.getId())) throw new IllegalArgumentException("Vocês já são amigos.");
        if (amizades.findBySolicitanteIdAndDestinatarioId(eu.getId(), outro.getId()).isPresent())
            throw new IllegalArgumentException("Você já enviou um pedido para " + outro.getNome() + ". Aguarde a resposta.");
        Optional<Amizade> inversa = amizades.findBySolicitanteIdAndDestinatarioId(outro.getId(), eu.getId());
        if (inversa.isPresent()) {
            Amizade pedido = inversa.get();
            pedido.setAceita(true);
            return amizades.save(pedido);
        }
        return amizades.save(new Amizade(eu, outro));
    }

    /** Aceita um pedido recebido. */
    @Transactional
    public Amizade aceitar(Long amizadeId, Long usuarioId) {
        Amizade a = amizades.findById(amizadeId).orElse(null);
        if (a == null || a.isAceita() || !a.getDestinatario().getId().equals(usuarioId))
            throw new IllegalArgumentException("Pedido de amizade não encontrado.");
        a.setAceita(true);
        return amizades.save(a);
    }

    /** Recusa um pedido recebido ou cancela um pedido enviado. Só vale para pedidos ainda pendentes. */
    @Transactional
    public void recusarOuCancelar(Long amizadeId, Long usuarioId) {
        Amizade a = amizades.findById(amizadeId).orElse(null);
        if (a == null || a.isAceita() || !participa(a, usuarioId)) throw new IllegalArgumentException("Pedido de amizade não encontrado.");
        amizades.delete(a);
    }

    /** Desfaz uma amizade já aceita. As receitas já compartilhadas continuam com quem as recebeu. */
    @Transactional
    public void desfazer(Long amizadeId, Long usuarioId) {
        Amizade a = amizades.findById(amizadeId).orElse(null);
        if (a == null || !a.isAceita() || !participa(a, usuarioId)) throw new IllegalArgumentException("Amizade não encontrada.");
        amizades.delete(a);
    }

    @Transactional(readOnly = true)
    public List<Amigo> amigos(Long usuarioId) {
        return amizades.aceitasDe(usuarioId).stream()
                .map(a -> new Amigo(a.getId(), a.outro(usuarioId)))
                .sorted(Comparator.comparing((Amigo x) -> x.usuario().getNome().toLowerCase()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Amizade> pedidosRecebidos(Long usuarioId) {
        return amizades.findAllByDestinatarioIdAndAceitaFalseOrderByCriadoEmDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Amizade> pedidosEnviados(Long usuarioId) {
        return amizades.findAllBySolicitanteIdAndAceitaFalseOrderByCriadoEmDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public long quantidadeDePedidosRecebidos(Long usuarioId) {
        return amizades.countByDestinatarioIdAndAceitaFalse(usuarioId);
    }

    @Transactional(readOnly = true)
    public boolean saoAmigos(Long a, Long b) {
        return amizades.saoAmigos(a, b);
    }

    private static boolean participa(Amizade a, Long usuarioId) {
        return a.getSolicitante().getId().equals(usuarioId) || a.getDestinatario().getId().equals(usuarioId);
    }
}
