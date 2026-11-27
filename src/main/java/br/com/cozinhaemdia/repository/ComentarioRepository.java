package br.com.cozinhaemdia.repository;

import br.com.cozinhaemdia.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findAllByReceitaIdOrderByCriadoEmAscIdAsc(Long receitaId);
    long countByReceitaId(Long receitaId);
}
