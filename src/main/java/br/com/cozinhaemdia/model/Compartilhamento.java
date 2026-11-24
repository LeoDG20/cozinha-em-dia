package br.com.cozinhaemdia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Receita compartilhada pelo dono com outro usuário, que pode consultá-la e salvar uma cópia. */
@Entity
@Table(name = "compartilhamentos", uniqueConstraints = @UniqueConstraint(columnNames = {"receita_id", "destino_id"}))
public class Compartilhamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id", nullable = false)
    private Receita receita;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_id", nullable = false)
    private Usuario destino;
    @Column(length = 200)
    private String mensagem;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(nullable = false)
    private boolean copiada;

    public Compartilhamento() {}
    public Compartilhamento(Receita receita, Usuario destino, String mensagem) {
        this.receita = receita; this.destino = destino; this.mensagem = mensagem;
    }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public Usuario getDestino() { return destino; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public boolean isCopiada() { return copiada; }
    public void setCopiada(boolean copiada) { this.copiada = copiada; }
}
