package br.com.cozinhaemdia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "receita_ingredientes", uniqueConstraints = @UniqueConstraint(columnNames = {"receita_id", "ingrediente_id"}))
public class ReceitaIngrediente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receita_id", nullable = false)
    private Receita receita;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;
    @Column(nullable = false)
    private Double quantidade;
    @Column(nullable = false, length = 20)
    private String unidade;
    public ReceitaIngrediente() {}
    public ReceitaIngrediente(Receita receita, Ingrediente ingrediente, Double quantidade, String unidade) {
        this.receita = receita; this.ingrediente = ingrediente; this.quantidade = quantidade; this.unidade = unidade;
    }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public void setReceita(Receita receita) { this.receita = receita; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
}
