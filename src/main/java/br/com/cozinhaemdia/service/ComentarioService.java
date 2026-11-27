package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Comentario;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ComentarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comentários em receitas. Quem pode ver a receita (o dono e os amigos com quem ela foi compartilhada)
 * pode ler e escrever comentários; o autor apaga o próprio comentário e o dono apaga qualquer um.
 */
@Service
public class ComentarioService {
    public static final int TAMANHO_MAXIMO = 500;

    private final ComentarioRepository comentarios;
    private final CompartilhamentoService compartilhamentos;
    private final UsuarioService usuarios;

    @Autowired
    public ComentarioService(ComentarioRepository comentarios, CompartilhamentoService compartilhamentos, UsuarioService usuarios) {
        this.comentarios = comentarios; this.compartilhamentos = compartilhamentos; this.usuarios = usuarios;
    }

    /** Comentários da receita, do mais antigo ao mais novo; vazio se o usuário não pode ver a receita. */
    @Transactional(readOnly = true)
    public List<Comentario> listar(Long receitaId, Long usuarioId) {
        if (compartilhamentos.visivelPara(receitaId, usuarioId).isEmpty()) return List.of();
        List<Comentario> lista = comentarios.findAllByReceitaIdOrderByCriadoEmAscIdAsc(receitaId);
        lista.forEach(c -> c.getAutor().getNome());
        return lista;
    }

    @Transactional
    public Comentario comentar(Long receitaId, Long autorId, String texto) {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.isEmpty()) throw new IllegalArgumentException("Escreva o comentário antes de enviar.");
        if (limpo.length() > TAMANHO_MAXIMO) throw new IllegalArgumentException("O comentário deve ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        Receita receita = compartilhamentos.visivelPara(receitaId, autorId)
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada."));
        Usuario autor = usuarios.buscar(autorId);
        return comentarios.save(new Comentario(receita, autor, limpo));
    }

    /** Remove o comentário se o usuário for o autor ou o dono da receita; devolve se algo foi removido. */
    @Transactional
    public boolean excluir(Long comentarioId, Long receitaId, Long usuarioId) {
        Comentario c = comentarios.findById(comentarioId).orElse(null);
        if (c == null || !c.getReceita().getId().equals(receitaId)) return false;
        boolean autor = c.getAutor().getId().equals(usuarioId);
        boolean dono = c.getReceita().getUsuario().getId().equals(usuarioId);
        if (!autor && !dono) return false;
        comentarios.delete(c);
        return true;
    }
}
