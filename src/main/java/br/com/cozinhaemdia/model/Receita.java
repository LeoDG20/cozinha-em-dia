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
    @Column(name = "imagem_arquivo", length = 80)
    private String imagemArquivo;
    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;
    @Column(name = "video_url", length = 300)
    private String videoUrl;
    @Column(name = "video_arquivo", length = 80)
    private String videoArquivo;
    @Column(name = "origem_nome", length = 120)
    private String origemNome;
    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReceitaIngrediente> ingredientes = new ArrayList<>();
    @OneToMany(mappedBy = "receita", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<EtapaReceita> etapas = new ArrayList<>();
    @OneToMany(mappedBy = "receita", cascade = CascadeType.REMOVE)
    private List<Compartilhamento> compartilhamentos = new ArrayList<>();

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
    public List<EtapaReceita> getEtapas() { return etapas; }
    public String getImagemArquivo() { return imagemArquivo; }
    public void setImagemArquivo(String imagemArquivo) { this.imagemArquivo = imagemArquivo; }
    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getOrigemNome() { return origemNome; }
    public void setOrigemNome(String origemNome) { this.origemNome = origemNome; }
    @Transient public boolean isAdaptada() { return origemNome != null && !origemNome.isBlank(); }
    public String getVideoArquivo() { return videoArquivo; }
    public void setVideoArquivo(String videoArquivo) { this.videoArquivo = videoArquivo; }
    @Transient public boolean isTemImagem() { return (imagemArquivo != null && !imagemArquivo.isBlank()) || (imagemUrl != null && !imagemUrl.isBlank()); }
    /** Endereço da imagem de capa: arquivo enviado (servido em /midia) ou endereço externo informado. */
    @Transient public String getCapaUrl() {
        if (imagemArquivo != null && !imagemArquivo.isBlank()) return "/midia/" + imagemArquivo;
        return imagemUrl != null && !imagemUrl.isBlank() ? imagemUrl : null;
    }
    @Transient public String getEmoji() { return br.com.cozinhaemdia.service.AparenciaReceita.emoji(nome); }
    @Transient public int getTom() { return br.com.cozinhaemdia.service.AparenciaReceita.tom(nome); }
    @Transient public String getVideoEmbedUrl() {
        String id = getVideoYoutubeId();
        return id == null ? null : br.com.cozinhaemdia.service.YoutubeUtil.enderecoIncorporado(id);
    }
    @Transient public String getVideoYoutubeId() { return br.com.cozinhaemdia.service.YoutubeUtil.extrairId(videoUrl).orElse(null); }
}
