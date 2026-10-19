package br.com.cozinhaemdia.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receitas")
public class Receita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(length = 500)
    private String descricao;
    @Column(name = "modo_preparo", nullable = false, columnDefinition = "TEXT")
    private String modoPreparo;
    @Column(name = "tempo_preparo_min")
    private Integer tempoPreparoMin;
    private Integer porcoes;
    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReceitaIngrediente> ingredientes = new ArrayList<>();

    public Receita() {}
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getModoPreparo() { return modoPreparo; }
    public void setModoPreparo(String modoPreparo) { this.modoPreparo = modoPreparo; }
    public Integer getTempoPreparoMin() { return tempoPreparoMin; }
    public void setTempoPreparoMin(Integer tempoPreparoMin) { this.tempoPreparoMin = tempoPreparoMin; }
    public Integer getPorcoes() { return porcoes; }
    public void setPorcoes(Integer porcoes) { this.porcoes = porcoes; }
    public List<ReceitaIngrediente> getIngredientes() { return ingredientes; }
}
