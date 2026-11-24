package br.com.cozinhaemdia.repository;

import br.com.cozinhaemdia.model.Amizade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmizadeRepository extends JpaRepository<Amizade, Long> {
    Optional<Amizade> findBySolicitanteIdAndDestinatarioId(Long solicitanteId, Long destinatarioId);
    List<Amizade> findAllByDestinatarioIdAndAceitaFalseOrderByCriadoEmDesc(Long destinatarioId);
    List<Amizade> findAllBySolicitanteIdAndAceitaFalseOrderByCriadoEmDesc(Long solicitanteId);
    long countByDestinatarioIdAndAceitaFalse(Long destinatarioId);

    @Query("select a from Amizade a where a.aceita = true and (a.solicitante.id = :id or a.destinatario.id = :id)")
    List<Amizade> aceitasDe(@Param("id") Long usuarioId);

    @Query("select count(a) > 0 from Amizade a where a.aceita = true and ((a.solicitante.id = :a and a.destinatario.id = :b) or (a.solicitante.id = :b and a.destinatario.id = :a))")
    boolean saoAmigos(@Param("a") Long a, @Param("b") Long b);
}
