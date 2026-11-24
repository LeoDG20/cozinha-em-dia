package br.com.cozinhaemdia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do armazenamento de imagens e vídeos: tipo pelo conteúdo, nomes seguros e limites de tamanho. */
class ArmazenamentoArquivosTest {
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    static final byte[] MP4 = {0, 0, 0, 24, 'f', 't', 'y', 'p', 'm', 'p', '4', '2', 0, 0, 0, 0};
    static final byte[] WEBM = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 1, 0, 0, 0};

    @TempDir Path pasta;
    ArmazenamentoArquivos armazenamento;

    @BeforeEach
    void preparar() {
        armazenamento = new ArmazenamentoArquivos(pasta.toString());
    }

    private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("f", nome, "application/octet-stream", conteudo);
    }

    @Test
    void gravaImagemComNomeGeradoEExtensaoPeloConteudo() throws Exception {
        String nome = armazenamento.salvarImagem(arquivo("foto-do-bolo.exe", JPEG));

        assertTrue(ArmazenamentoArquivos.nomeValido(nome), nome);
        assertTrue(nome.endsWith(".jpg"));
        assertTrue(Files.exists(pasta.resolve(nome)));
        assertEquals(JPEG.length, Files.size(pasta.resolve(nome)));
    }

    @Test
    void reconheceOsFormatosAceitos() {
        assertTrue(armazenamento.salvarImagem(arquivo("a", PNG)).endsWith(".png"));
        assertTrue(armazenamento.salvarVideo(arquivo("a", MP4)).endsWith(".mp4"));
        assertTrue(armazenamento.salvarVideo(arquivo("a", WEBM)).endsWith(".webm"));
    }

    @Test
    void recusaArquivoQueNaoEhImagemMesmoComNomeDeImagem() {
        MockMultipartFile falso = arquivo("foto.jpg", "MZ executavel disfarcado".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> armazenamento.salvarImagem(falso));

        assertTrue(erro.getMessage().contains("JPG"));
    }

    @Test
    void recusaVideoNoCampoDeImagemEImagemNoCampoDeVideo() {
        assertThrows(IllegalArgumentException.class, () -> armazenamento.salvarImagem(arquivo("v.mp4", MP4)));
        assertThrows(IllegalArgumentException.class, () -> armazenamento.salvarVideo(arquivo("i.jpg", JPEG)));
    }

    @Test
    void recusaArquivosAcimaDoLimite() {
        byte[] grande = new byte[(int) ArmazenamentoArquivos.TAMANHO_MAXIMO_IMAGEM + 1];
        System.arraycopy(JPEG, 0, grande, 0, JPEG.length);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> armazenamento.salvarImagem(arquivo("g.jpg", grande)));

        assertTrue(erro.getMessage().contains("5 MB"));
    }

    @Test
    void arquivoVazioEhIgnorado() {
        assertEquals(null, armazenamento.salvarImagem(new MockMultipartFile("f", "", "image/jpeg", new byte[0])));
        assertEquals(null, armazenamento.salvarImagem(null));
    }

    @Test
    void carregarSoAceitaNomesGeradosPeloSistema() throws Exception {
        String nome = armazenamento.salvarImagem(arquivo("a", PNG));
        Files.writeString(pasta.resolve("segredo.txt"), "x");

        assertTrue(armazenamento.carregar(nome).isPresent());
        assertTrue(armazenamento.carregar("segredo.txt").isEmpty());
        assertTrue(armazenamento.carregar("../segredo.txt").isEmpty());
        assertTrue(armazenamento.carregar("..%2Fsegredo.txt").isEmpty());
        assertTrue(armazenamento.carregar(null).isEmpty());
        assertTrue(armazenamento.carregar("00000000-0000-0000-0000-000000000000.png").isEmpty());
    }

    @Test
    void removerApagaOArquivoEIgnoraNomesInvalidos() throws Exception {
        String nome = armazenamento.salvarImagem(arquivo("a", PNG));
        Path fora = Files.writeString(pasta.resolve("nao-apagar.txt"), "x");

        armazenamento.remover("nao-apagar.txt");
        armazenamento.remover(null);
        armazenamento.remover(nome);

        assertTrue(Files.exists(fora));
        assertFalse(Files.exists(pasta.resolve(nome)));
    }

    @Test
    void copiarCriaUmArquivoIndependente() throws Exception {
        String original = armazenamento.salvarImagem(arquivo("a", JPEG));

        String copia = armazenamento.copiar(original);
        armazenamento.remover(original);

        assertNotEquals(original, copia);
        assertTrue(Files.exists(pasta.resolve(copia)));
        assertEquals(null, armazenamento.copiar("inexistente"));
    }

    @Test
    void tipoDeConteudoAcompanhaAExtensao() {
        assertEquals("image/jpeg", armazenamento.tipo("a.jpg").toString());
        assertEquals("video/mp4", armazenamento.tipo("a.mp4").toString());
        assertEquals("video/webm", armazenamento.tipo("a.webm").toString());
        assertEquals("image/webp", armazenamento.tipo("a.webp").toString());
    }
}
