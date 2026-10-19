package br.com.cozinhaemdia.repository;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReceitaIngredienteRepository extends JpaRepository<ReceitaIngrediente, Long> {
    List<ReceitaIngrediente> findAllByReceitaIdOrderByIdAsc(Long receitaId);
    void deleteAllByReceitaId(Long receitaId);
}
