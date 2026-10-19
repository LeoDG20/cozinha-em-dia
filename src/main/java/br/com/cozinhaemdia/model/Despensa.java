package br.com.cozinhaemdia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "despensa", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "ingrediente_id"}))
public class Despensa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;
    @Column(nullable = false)
    private Double quantidade;
    @Column(nullable = false, length = 20)
    private String unidade;
    public Despensa() {}
    public Despensa(Usuario usuario, Ingrediente ingrediente, Double quantidade, String unidade) {
        this.usuario = usuario; this.ingrediente = ingrediente; this.quantidade = quantidade; this.unidade = unidade;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
}
