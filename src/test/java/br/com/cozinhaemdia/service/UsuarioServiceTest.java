package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cadastro e consulta de usuários. */
class UsuarioServiceTest {
    private UsuarioRepository repositorio;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        service = new UsuarioService(repositorio, encoder);
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cadastrarNormalizaEmailECodificaSenha() {
        Usuario u = service.cadastrar("  Ana  ", " Ana@Teste.COM ", "segredo", "segredo");
        assertEquals("Ana", u.getNome());
        assertEquals("ana@teste.com", u.getEmail());
        assertTrue(encoder.matches("segredo", u.getSenha()));
    }

    @Test
    void cadastrarRecusaCamposVaziosESenhaCurta() {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("", "a@b.com", "segredo", "segredo"));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Ana", "", "segredo", "segredo"));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Ana", "a@b.com", "123", "123"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void cadastrarRecusaEmailInvalidoENomeLongo() {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Ana", "sem-arroba", "segredo", "segredo"));
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("x".repeat(101), "a@b.com", "segredo", "segredo"));
    }

    @Test
    void cadastrarRecusaSenhasDiferentesEEmailRepetido() {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Ana", "a@b.com", "segredo", "outra123"));
        when(repositorio.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Ana", "a@b.com", "segredo", "segredo"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void buscarFalhaQuandoNaoExiste() {
        when(repositorio.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.buscar(9L));
        assertTrue(service.encontrar(9L).isEmpty());
    }
}
