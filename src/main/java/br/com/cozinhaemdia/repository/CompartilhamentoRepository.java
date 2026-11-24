package br.com.cozinhaemdia.repository;

import br.com.cozinhaemdia.model.Compartilhamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompartilhamentoRepository extends JpaRepository<Compartilhamento, Long> {
    List<Compartilhamento> findAllByDestinoIdOrderByCriadoEmDesc(Long destinoId);
    List<Compartilhamento> findAllByReceitaIdOrderByCriadoEmDesc(Long receitaId);
    Optional<Compartilhamento> findByReceitaIdAndDestinoId(Long receitaId, Long destinoId);
    List<Compartilhamento> findAllByReceitaUsuarioIdOrderByDestinoNomeAsc(Long donoId);
    boolean existsByReceitaIdAndDestinoId(Long receitaId, Long destinoId);
    long countByDestinoIdAndCopiadaFalse(Long destinoId);
}
