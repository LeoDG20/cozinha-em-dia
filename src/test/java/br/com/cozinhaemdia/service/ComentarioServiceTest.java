package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Comentario;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.ComentarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de permissão e validação dos comentários. */
class ComentarioServiceTest {
    private ComentarioRepository repositorio;
    private CompartilhamentoService compartilhamentos;
    private UsuarioService usuarios;
    private ComentarioService service;
    private Usuario dono;
    private Usuario amigo;
    private Receita receita;

    private static Usuario usuario(Long id, String nome) {
        Usuario u = new Usuario(nome, nome.toLowerCase() + "@teste.com", "hash");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @BeforeEach
    void preparar() {
        repositorio = mock(ComentarioRepository.class);
        compartilhamentos = mock(CompartilhamentoService.class);
        usuarios = mock(UsuarioService.class);
        service = new ComentarioService(repositorio, compartilhamentos, usuarios);
        dono = usuario(1L, "Dona");
        amigo = usuario(2L, "Amigo");
        receita = new Receita();
        ReflectionTestUtils.setField(receita, "id", 10L);
        receita.setUsuario(dono);
        when(usuarios.buscar(1L)).thenReturn(dono);
        when(usuarios.buscar(2L)).thenReturn(amigo);
        when(repositorio.save(any(Comentario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void comentarGuardaTextoSemEspacosNasPontas() {
        when(compartilhamentos.visivelPara(10L, 2L)).thenReturn(Optional.of(receita));
        Comentario c = service.comentar(10L, 2L, "  Muito boa!  ");
        assertEquals("Muito boa!", c.getTexto());
        assertEquals(amigo, c.getAutor());
        assertEquals(receita, c.getReceita());
    }

    @Test
    void comentarRecusaTextoVazioELongo() {
        when(compartilhamentos.visivelPara(10L, 2L)).thenReturn(Optional.of(receita));
        assertThrows(IllegalArgumentException.class, () -> service.comentar(10L, 2L, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.comentar(10L, 2L, null));
        assertThrows(IllegalArgumentException.class, () -> service.comentar(10L, 2L, "x".repeat(ComentarioService.TAMANHO_MAXIMO + 1)));
        service.comentar(10L, 2L, "x".repeat(ComentarioService.TAMANHO_MAXIMO));
    }

    @Test
    void comentarExigeAcessoAReceita() {
        when(compartilhamentos.visivelPara(10L, 3L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.comentar(10L, 3L, "Oi"));
        verify(repositorio, never()).save(any());
    }

    @Test
    void listarDevolveVazioSemAcesso() {
        when(compartilhamentos.visivelPara(10L, 3L)).thenReturn(Optional.empty());
        assertTrue(service.listar(10L, 3L).isEmpty());
        verify(repositorio, never()).findAllByReceitaIdOrderByCriadoEmAscIdAsc(any());
    }

    @Test
    void excluirPermitidoAoAutorEAoDonoApenas() {
        Comentario c = new Comentario(receita, amigo, "texto");
        ReflectionTestUtils.setField(c, "id", 5L);
        when(repositorio.findById(5L)).thenReturn(Optional.of(c));

        assertFalse(service.excluir(5L, 10L, 3L));
        assertFalse(service.excluir(5L, 99L, 1L));
        verify(repositorio, never()).delete(any());
        assertTrue(service.excluir(5L, 10L, 2L));
        assertTrue(service.excluir(5L, 10L, 1L));
        assertFalse(service.excluir(6L, 10L, 1L));
    }

    @Test
    void listarEntregaOsComentariosDaReceita() {
        when(compartilhamentos.visivelPara(10L, 1L)).thenReturn(Optional.of(receita));
        when(repositorio.findAllByReceitaIdOrderByCriadoEmAscIdAsc(10L)).thenReturn(List.of(new Comentario(receita, amigo, "a")));
        assertEquals(1, service.listar(10L, 1L).size());
    }
}
