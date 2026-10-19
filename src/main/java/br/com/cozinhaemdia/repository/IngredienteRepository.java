package br.com.cozinhaemdia.repository;
import br.com.cozinhaemdia.model.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
    Optional<Ingrediente> findByNomeIgnoreCase(String nome);
}
