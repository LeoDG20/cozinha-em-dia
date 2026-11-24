package br.com.cozinhaemdia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Pedido de amizade entre dois usuários. Enquanto não aceito, fica pendente para o destinatário. */
@Entity
@Table(name = "amizades", uniqueConstraints = @UniqueConstraint(columnNames = {"solicitante_id", "destinatario_id"}))
public class Amizade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;
    @Column(nullable = false)
    private boolean aceita;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Amizade() {}
    public Amizade(Usuario solicitante, Usuario destinatario) { this.solicitante = solicitante; this.destinatario = destinatario; }

    public Long getId() { return id; }
    public Usuario getSolicitante() { return solicitante; }
    public Usuario getDestinatario() { return destinatario; }
    public boolean isAceita() { return aceita; }
    public void setAceita(boolean aceita) { this.aceita = aceita; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    /** A outra pessoa da amizade, do ponto de vista do usuário informado. */
    public Usuario outro(Long usuarioId) { return solicitante.getId().equals(usuarioId) ? destinatario : solicitante; }
}
