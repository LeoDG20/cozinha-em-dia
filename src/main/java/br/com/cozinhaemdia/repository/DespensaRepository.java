package br.com.cozinhaemdia.repository;
import br.com.cozinhaemdia.model.Despensa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface DespensaRepository extends JpaRepository<Despensa, Long> {
    List<Despensa> findAllByUsuarioIdOrderByIngredienteNomeAsc(Long usuarioId);
    Optional<Despensa> findByUsuarioIdAndIngredienteId(Long usuarioId, Long ingredienteId);
    Optional<Despensa> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioId(Long usuarioId);
}
