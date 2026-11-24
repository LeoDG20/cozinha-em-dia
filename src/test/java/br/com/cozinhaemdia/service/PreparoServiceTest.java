package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.service.PreparoService.Etapa;
import br.com.cozinhaemdia.service.PreparoService.Midia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da imagem, dos vídeos e das etapas com foto de uma receita. */
class PreparoServiceTest {
    @TempDir Path pasta;
    ArmazenamentoArquivos armazenamento;
    PreparoService service;
    Receita receita;

    @BeforeEach
    void preparar() {
        armazenamento = new ArmazenamentoArquivos(pasta.toString());
        service = new PreparoService(armazenamento);
        receita = new Receita();
    }

    private static MockMultipartFile arquivo(byte[] conteudo) {
        return new MockMultipartFile("f", "arquivo", "application/octet-stream", conteudo);
    }

    private static Midia midia(MockMultipartFile imagem, String imagemUrl, boolean removerImagem, String videoUrl, MockMultipartFile video, boolean removerVideo) {
        return new Midia(imagem, imagemUrl, removerImagem, videoUrl, video, removerVideo);
    }

    @Test
    void montaModoDePreparoNumeradoIgnorandoEtapasVazias() {
        String texto = PreparoService.montarModoPreparo(List.of(
                new Etapa("  Misture os ingredientes ", null, null), new Etapa("   ", null, null), new Etapa("Asse por 30 minutos", null, null)));

        assertEquals("1. Misture os ingredientes\n2. Asse por 30 minutos", texto);
    }

    @Test
    void divideModoDePreparoAntigoEmEtapasSemNumeracao() {
        List<String> etapas = PreparoService.dividirModoPreparo("1. Corte a cebola\n2) Refogue\n\n3 - Sirva\nDescanse 5 minutos");

        assertEquals(List.of("Corte a cebola", "Refogue", "Sirva", "Descanse 5 minutos"), etapas);
    }

    @Test
    void gravaImagemDeCapaEVideoProprio() {
        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, "https://youtu.be/dQw4w9WgXcQ", arquivo(ArmazenamentoArquivosTest.MP4), false), List.of());

        assertTrue(receita.isTemImagem());
        assertTrue(receita.getImagemArquivo().endsWith(".jpg"));
        assertTrue(receita.getVideoArquivo().endsWith(".mp4"));
        assertEquals("dQw4w9WgXcQ", receita.getVideoYoutubeId());
        assertTrue(Files.exists(pasta.resolve(receita.getImagemArquivo())));
    }

    @Test
    void novaImagemSubstituiAAnteriorEApagaOArquivoAntigo() {
        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, null, null, false), List.of());
        String antiga = receita.getImagemArquivo();

        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.PNG), null, false, null, null, false), List.of());

        assertTrue(receita.getImagemArquivo().endsWith(".png"));
        assertFalse(Files.exists(pasta.resolve(antiga)));
    }

    @Test
    void enderecoDeImagemSubstituiArquivoEremoverLimpaTudo() {
        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, null, null, false), List.of());
        String arquivo = receita.getImagemArquivo();

        service.aplicar(receita, midia(null, "https://exemplo.com/bolo.jpg", false, null, null, false), List.of());
        assertNull(receita.getImagemArquivo());
        assertEquals("https://exemplo.com/bolo.jpg", receita.getImagemUrl());
        assertFalse(Files.exists(pasta.resolve(arquivo)));

        service.aplicar(receita, midia(null, "https://exemplo.com/bolo.jpg", true, null, null, false), List.of());
        assertFalse(receita.isTemImagem());
    }

    @Test
    void semNovosDadosMantemAImagemExistente() {
        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, null, null, false), List.of());
        String arquivo = receita.getImagemArquivo();

        service.aplicar(receita, midia(null, "", false, null, null, false), List.of());

        assertEquals(arquivo, receita.getImagemArquivo());
    }

    @Test
    void removerVideoApagaOArquivoELimpaOLinkQuandoOCampoFicaVazio() {
        service.aplicar(receita, midia(null, null, false, "https://youtu.be/dQw4w9WgXcQ", arquivo(ArmazenamentoArquivosTest.WEBM), false), List.of());
        String video = receita.getVideoArquivo();

        service.aplicar(receita, midia(null, null, false, "", null, true), List.of());

        assertNull(receita.getVideoArquivo());
        assertNull(receita.getVideoUrl());
        assertFalse(Files.exists(pasta.resolve(video)));
    }

    @Test
    void recusaLinkQueNaoEhDoYoutubeEEnderecoDeImagemInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.aplicar(receita, midia(null, null, false, "https://vimeo.com/1", null, false), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.aplicar(receita, midia(null, "javascript:alert(1)", false, null, null, false), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.aplicar(receita, midia(null, "ftp://servidor/foto.jpg", false, null, null, false), List.of()));
        assertNull(receita.getVideoUrl());
    }

    @Test
    void etapasGuardamTextoOrdemEFoto() {
        service.aplicar(receita, Midia.vazia(), List.of(
                new Etapa("Corte os legumes", arquivo(ArmazenamentoArquivosTest.JPEG), null),
                new Etapa("   ", null, null),
                new Etapa("Leve ao fogo", null, null)));

        List<EtapaReceita> etapas = receita.getEtapas();
        assertEquals(2, etapas.size());
        assertEquals(1, etapas.get(0).getOrdem());
        assertEquals("Corte os legumes", etapas.get(0).getDescricao());
        assertNotNull(etapas.get(0).getImagemArquivo());
        assertEquals(2, etapas.get(1).getOrdem());
        assertNull(etapas.get(1).getImagemArquivo());
    }

    @Test
    void editarMantemFotoExistenteEApagaAsQueSaem() {
        service.aplicar(receita, Midia.vazia(), List.of(
                new Etapa("Passo 1", arquivo(ArmazenamentoArquivosTest.JPEG), null),
                new Etapa("Passo 2", arquivo(ArmazenamentoArquivosTest.PNG), null)));
        String foto1 = receita.getEtapas().get(0).getImagemArquivo();
        String foto2 = receita.getEtapas().get(1).getImagemArquivo();

        service.aplicar(receita, Midia.vazia(), List.of(new Etapa("Passo 1 revisado", null, foto1)));

        assertEquals(1, receita.getEtapas().size());
        assertEquals(foto1, receita.getEtapas().get(0).getImagemArquivo());
        assertTrue(Files.exists(pasta.resolve(foto1)));
        assertFalse(Files.exists(pasta.resolve(foto2)));
    }

    @Test
    void naoPermiteReaproveitarArquivoDeOutraReceitaPeloFormulario() {
        Receita outra = new Receita();
        service.aplicar(outra, Midia.vazia(), List.of(new Etapa("Privado", arquivo(ArmazenamentoArquivosTest.JPEG), null)));
        String alheia = outra.getEtapas().get(0).getImagemArquivo();

        service.aplicar(receita, Midia.vazia(), List.of(new Etapa("Tentativa", null, alheia)));

        assertNull(receita.getEtapas().get(0).getImagemArquivo());
        assertTrue(Files.exists(pasta.resolve(alheia)));
    }

    @Test
    void removerArquivosApagaTudoDaReceita() {
        service.aplicar(receita, midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, null, arquivo(ArmazenamentoArquivosTest.MP4), false),
                List.of(new Etapa("Passo", arquivo(ArmazenamentoArquivosTest.PNG), null)));

        service.removerArquivos(receita);

        try (var arquivos = Files.list(pasta)) {
            assertEquals(0, arquivos.count());
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void validacaoImpedeGravacaoParcial() {
        MockMultipartFile invalido = arquivo("nao sou imagem".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.aplicar(receita,
                midia(arquivo(ArmazenamentoArquivosTest.JPEG), null, false, null, null, false),
                List.of(new Etapa("Passo", invalido, null))));

        assertFalse(receita.isTemImagem());
        try (var arquivos = Files.list(pasta)) {
            assertEquals(0, arquivos.count());
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }
}
