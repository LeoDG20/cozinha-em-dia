package br.com.cozinhaemdia.model;

import jakarta.persistence.*;

/** Uma etapa do modo de preparo, com texto e, opcionalmente, uma foto. */
@Entity
@Table(name = "etapas_receita")
public class EtapaReceita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receita_id", nullable = false)
    private Receita receita;
    @Column(nullable = false)
    private int ordem;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;
    @Column(name = "imagem_arquivo", length = 80)
    private String imagemArquivo;

    public EtapaReceita() {}
    public EtapaReceita(Receita receita, int ordem, String descricao, String imagemArquivo) {
        this.receita = receita; this.ordem = ordem; this.descricao = descricao; this.imagemArquivo = imagemArquivo;
    }
    public Long getId() { return id; }
    public Receita getReceita() { return receita; }
    public void setReceita(Receita receita) { this.receita = receita; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getImagemArquivo() { return imagemArquivo; }
    public void setImagemArquivo(String imagemArquivo) { this.imagemArquivo = imagemArquivo; }
}
