package br.com.cozinhaemdia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Comentário de um usuário em uma receita própria ou compartilhada com ele. */
@Entity
@Table(name = "comentarios")
public class Comentario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id", nullable = false)
    private Receita receita;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @Column(nullable = false, length = 500)
    private String texto;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Comentario() {}
    public Comentario(Receita receita, Usuario autor, String texto) {
        this.receita = receita; this.autor = autor; this.texto = texto;
    }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public Usuario getAutor() { return autor; }
    public String getTexto() { return texto; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
