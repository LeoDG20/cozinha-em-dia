package br.com.cozinhaemdia.repository;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ReceitaIngredienteRepository extends JpaRepository<ReceitaIngrediente, Long> {
    List<ReceitaIngrediente> findAllByReceitaIdOrderByIdAsc(Long receitaId);
    void deleteAllByReceitaId(Long receitaId);

    /** Quantas receitas do usuário usam cada ingrediente (pares id do ingrediente e contagem). */
    @Query("select ri.ingrediente.id, count(distinct ri.receita.id) from ReceitaIngrediente ri where ri.receita.usuario.id = :usuarioId group by ri.ingrediente.id")
    List<Object[]> contarUsoPorIngrediente(@Param("usuarioId") Long usuarioId);
}
