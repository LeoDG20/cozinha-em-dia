package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Receita;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Material de apoio de uma receita: imagem de capa, vídeos (YouTube ou arquivo próprio) e etapas com foto. */
@Service
public class PreparoService {
    /** Dados do envio de mídia feito no formulário da receita. */
    public record Midia(MultipartFile imagem, String imagemUrl, boolean removerImagem,
                        String videoUrl, MultipartFile video, boolean removerVideo) {
        public static Midia vazia() { return new Midia(null, null, false, null, null, false); }
    }

    /** Uma linha de etapa do formulário: texto, nova foto enviada e foto já existente a manter. */
    public record Etapa(String texto, MultipartFile foto, String fotoAtual) {}

    private final ArmazenamentoArquivos arquivos;

    public PreparoService(ArmazenamentoArquivos arquivos) { this.arquivos = arquivos; }

    /** Texto numerado do modo de preparo, a partir das etapas não vazias. */
    public static String montarModoPreparo(List<Etapa> etapas) {
        StringBuilder texto = new StringBuilder();
        int numero = 0;
        for (Etapa etapa : etapas) {
            String t = limpar(etapa.texto());
            if (t.isEmpty()) continue;
            if (numero++ > 0) texto.append('\n');
            texto.append(numero).append(". ").append(t);
        }
        return texto.toString();
    }

    /** Separa um modo de preparo em texto corrido nas linhas das etapas, removendo a numeração inicial. */
    public static List<String> dividirModoPreparo(String modoPreparo) {
        List<String> etapas = new ArrayList<>();
        if (modoPreparo == null) return etapas;
        for (String linha : modoPreparo.split("\\R")) {
            String t = linha.trim().replaceFirst("^\\d+\\s*[.)\\-:]\\s*", "");
            if (!t.isEmpty()) etapas.add(t);
        }
        return etapas;
    }

    /** Valida tudo o que será gravado antes de salvar a receita, para não deixar a gravação pela metade. */
    public void validar(Midia midia, List<Etapa> etapas) {
        arquivos.validarImagem(midia.imagem());
        arquivos.validarVideo(midia.video());
        String url = limpar(midia.imagemUrl());
        if (!url.isEmpty() && !urlHttpValida(url)) throw new IllegalArgumentException("Informe um endereço de imagem que comece com http:// ou https://.");
        String youtube = limpar(midia.videoUrl());
        if (!youtube.isEmpty() && YoutubeUtil.extrairId(youtube).isEmpty()) throw new IllegalArgumentException("Informe um link válido do YouTube, por exemplo https://www.youtube.com/watch?v=...");
        for (Etapa etapa : etapas) arquivos.validarImagem(etapa.foto());
    }

    /** Aplica imagem, vídeos e etapas à receita, apagando os arquivos que deixaram de ser usados. */
    @Transactional
    public void aplicar(Receita receita, Midia midia, List<Etapa> etapas) {
        validar(midia, etapas);
        aplicarImagem(receita, midia);
        aplicarVideo(receita, midia);
        aplicarEtapas(receita, etapas);
    }

    /** Apaga do disco todos os arquivos da receita (usado ao excluí-la). */
    public void removerArquivos(Receita receita) {
        arquivos.remover(receita.getImagemArquivo());
        arquivos.remover(receita.getVideoArquivo());
        for (EtapaReceita etapa : receita.getEtapas()) arquivos.remover(etapa.getImagemArquivo());
    }

    /** Apaga um arquivo isolado do disco, se existir. */
    public void removerArquivo(String nome) { arquivos.remover(nome); }

    private void aplicarImagem(Receita receita, Midia midia) {
        String url = limpar(midia.imagemUrl());
        if (midia.removerImagem()) {
            arquivos.remover(receita.getImagemArquivo());
            receita.setImagemArquivo(null);
            receita.setImagemUrl(null);
        } else if (!ArmazenamentoArquivos.vazio(midia.imagem())) {
            String novo = arquivos.salvarImagem(midia.imagem());
            arquivos.remover(receita.getImagemArquivo());
            receita.setImagemArquivo(novo);
            receita.setImagemUrl(null);
        } else if (!url.isEmpty() && !url.equals(receita.getImagemUrl())) {
            arquivos.remover(receita.getImagemArquivo());
            receita.setImagemArquivo(null);
            receita.setImagemUrl(url);
        } else if (url.isEmpty() && receita.getImagemUrl() != null) {
            receita.setImagemUrl(null);
        }
    }

    private void aplicarVideo(Receita receita, Midia midia) {
        String youtube = limpar(midia.videoUrl());
        receita.setVideoUrl(youtube.isEmpty() ? null : youtube);
        if (midia.removerVideo()) {
            arquivos.remover(receita.getVideoArquivo());
            receita.setVideoArquivo(null);
        } else if (!ArmazenamentoArquivos.vazio(midia.video())) {
            String novo = arquivos.salvarVideo(midia.video());
            arquivos.remover(receita.getVideoArquivo());
            receita.setVideoArquivo(novo);
        }
    }

    private void aplicarEtapas(Receita receita, List<Etapa> etapas) {
        Set<String> anteriores = new HashSet<>();
        for (EtapaReceita e : receita.getEtapas()) if (e.getImagemArquivo() != null) anteriores.add(e.getImagemArquivo());

        List<EtapaReceita> novas = new ArrayList<>();
        Set<String> mantidas = new HashSet<>();
        int ordem = 0;
        for (Etapa etapa : etapas) {
            String texto = limpar(etapa.texto());
            if (texto.isEmpty()) continue;
            String foto = null;
            if (!ArmazenamentoArquivos.vazio(etapa.foto())) foto = arquivos.salvarImagem(etapa.foto());
            // Só é possível manter uma foto que já pertencia a uma etapa desta receita.
            else if (etapa.fotoAtual() != null && anteriores.contains(etapa.fotoAtual())) foto = etapa.fotoAtual();
            if (foto != null) mantidas.add(foto);
            novas.add(new EtapaReceita(receita, ++ordem, texto, foto));
        }
        receita.getEtapas().clear();
        receita.getEtapas().addAll(novas);
        for (String arquivo : anteriores) if (!mantidas.contains(arquivo)) arquivos.remover(arquivo);
    }

    private static boolean urlHttpValida(String url) {
        if (url.length() > 500) return false;
        try {
            java.net.URI uri = java.net.URI.create(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String limpar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
