package br.com.cozinhaemdia.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda imagens e vídeos enviados pelos usuários em uma pasta do disco ({@code app.uploads.dir}).
 *
 * <p>O tipo do arquivo é identificado pelos primeiros bytes (e não pelo nome ou pelo tipo informado
 * pelo navegador), o nome gravado é sempre um UUID gerado pelo sistema e os acessos validam esse formato,
 * o que impede o envio de arquivos executáveis e a leitura de caminhos fora da pasta.</p>
 */
@Service
public class ArmazenamentoArquivos {
    public static final long TAMANHO_MAXIMO_IMAGEM = 5L * 1024 * 1024;
    public static final long TAMANHO_MAXIMO_VIDEO = 60L * 1024 * 1024;

    private static final Pattern NOME_VALIDO = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|gif|webp|mp4|webm)$");

    private final Path pasta;

    @Autowired
    public ArmazenamentoArquivos(@Value("${app.uploads.dir:uploads}") String diretorio) {
        this.pasta = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public static boolean vazio(MultipartFile arquivo) {
        return arquivo == null || arquivo.isEmpty();
    }

    public static boolean nomeValido(String nome) {
        return nome != null && NOME_VALIDO.matcher(nome).matches();
    }

    public void validarImagem(MultipartFile arquivo) {
        if (vazio(arquivo)) return;
        if (arquivo.getSize() > TAMANHO_MAXIMO_IMAGEM) throw new NegocioException("erro.arquivo.imagem.tamanho", "A imagem deve ter no máximo 5 MB.");
        String extensao = detectarExtensao(cabecalho(arquivo));
        if (extensao == null || extensao.equals("mp4") || extensao.equals("webm"))
            throw new NegocioException("erro.arquivo.imagem.formato", "Envie uma imagem nos formatos JPG, PNG, GIF ou WebP.");
    }

    public void validarVideo(MultipartFile arquivo) {
        if (vazio(arquivo)) return;
        if (arquivo.getSize() > TAMANHO_MAXIMO_VIDEO) throw new NegocioException("erro.arquivo.video.tamanho", "O vídeo deve ter no máximo 60 MB.");
        String extensao = detectarExtensao(cabecalho(arquivo));
        if (!"mp4".equals(extensao) && !"webm".equals(extensao))
            throw new NegocioException("erro.arquivo.video.formato", "Envie um vídeo nos formatos MP4 ou WebM.");
    }

    /** Valida e grava a imagem; devolve o nome do arquivo gravado. */
    public String salvarImagem(MultipartFile arquivo) {
        validarImagem(arquivo);
        return gravar(arquivo);
    }

    /** Valida e grava o vídeo; devolve o nome do arquivo gravado. */
    public String salvarVideo(MultipartFile arquivo) {
        validarVideo(arquivo);
        return gravar(arquivo);
    }

    public Optional<Resource> carregar(String nome) {
        if (!nomeValido(nome)) return Optional.empty();
        Path arquivo = pasta.resolve(nome).normalize();
        if (!arquivo.startsWith(pasta) || !Files.isRegularFile(arquivo)) return Optional.empty();
        return Optional.of(new FileSystemResource(arquivo));
    }

    public MediaType tipo(String nome) {
        if (nome == null) return MediaType.APPLICATION_OCTET_STREAM;
        if (nome.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        if (nome.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nome.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (nome.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (nome.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        if (nome.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** Duplica um arquivo já gravado (usado ao copiar uma receita compartilhada); devolve o novo nome. */
    public String copiar(String nome) {
        Resource origem = carregar(nome).orElse(null);
        if (origem == null) return null;
        String novoNome = UUID.randomUUID() + nome.substring(nome.lastIndexOf('.'));
        try (InputStream entrada = origem.getInputStream()) {
            Files.createDirectories(pasta);
            Files.copy(entrada, pasta.resolve(novoNome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível copiar o arquivo.", e);
        }
        return novoNome;
    }

    /** Apaga o arquivo, se existir; nomes inválidos ou nulos são ignorados. */
    public void remover(String nome) {
        if (!nomeValido(nome)) return;
        try {
            Files.deleteIfExists(pasta.resolve(nome).normalize());
        } catch (IOException ignorada) {
            // Um arquivo que não pôde ser apagado não deve impedir a operação principal.
        }
    }

    /** Extensão identificada pelos primeiros bytes, ou {@code null} se o formato não for aceito. */
    static String detectarExtensao(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (b.length >= 12 && ascii(b, 0, 4).equals("RIFF") && ascii(b, 8, 12).equals("WEBP")) return "webp";
        if (b.length >= 12 && ascii(b, 4, 8).equals("ftyp")) return "mp4";
        if (b.length >= 4 && (b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) return "webm";
        return null;
    }

    private static String ascii(byte[] b, int inicio, int fim) {
        return new String(b, inicio, fim - inicio, StandardCharsets.ISO_8859_1);
    }

    private static byte[] cabecalho(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return entrada.readNBytes(16);
        } catch (IOException e) {
            throw new NegocioException("erro.arquivo.leitura", "Não foi possível ler o arquivo enviado.");
        }
    }

    private String gravar(MultipartFile arquivo) {
        if (vazio(arquivo)) return null;
        String extensao = detectarExtensao(cabecalho(arquivo));
        String nome = UUID.randomUUID() + "." + extensao;
        try (InputStream entrada = arquivo.getInputStream()) {
            Files.createDirectories(pasta);
            Files.copy(entrada, pasta.resolve(nome));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar o arquivo.", e);
        }
        return nome;
    }
}
