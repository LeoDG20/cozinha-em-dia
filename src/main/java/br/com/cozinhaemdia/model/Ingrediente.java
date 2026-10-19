package br.com.cozinhaemdia.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ingredientes")
public class Ingrediente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String nome;
    public Ingrediente() {}
    public Ingrediente(String nome) { this.nome = nome; }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
