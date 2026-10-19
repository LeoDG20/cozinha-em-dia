package br.com.cozinhaemdia.repository;
import br.com.cozinhaemdia.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ReceitaRepository extends JpaRepository<Receita, Long> {
    List<Receita> findAllByUsuarioIdOrderByNomeAsc(Long usuarioId);
    Optional<Receita> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioId(Long usuarioId);
}
