package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Amizade;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.AmizadeRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de pedidos e de amizades entre usuários. */
class AmizadeServiceTest {
    private AmizadeRepository amizades;
    private UsuarioRepository usuarios;
    private AmizadeService service;
    private Usuario ana;
    private Usuario bruno;
    private Usuario carla;

    private static Usuario usuario(Long id, String nome) {
        Usuario u = new Usuario(nome, nome.toLowerCase() + "@teste.com", "hash");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static Amizade amizade(Long id, Usuario de, Usuario para, boolean aceita) {
        Amizade a = new Amizade(de, para);
        ReflectionTestUtils.setField(a, "id", id);
        a.setAceita(aceita);
        return a;
    }

    @BeforeEach
    void preparar() {
        amizades = mock(AmizadeRepository.class);
        usuarios = mock(UsuarioRepository.class);
        service = new AmizadeService(amizades, usuarios);
        ana = usuario(1L, "Ana");
        bruno = usuario(2L, "Bruno");
        carla = usuario(3L, "Carla");
        when(usuarios.findByEmailIgnoreCase("bruno@teste.com")).thenReturn(Optional.of(bruno));
        when(usuarios.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.of(ana));
        when(amizades.save(any(Amizade.class))).thenAnswer(c -> c.getArgument(0));
    }

    @Test
    void convidarCriaPedidoPendente() {
        Amizade a = service.convidar(ana, "  bruno@teste.com ");

        assertSame(ana, a.getSolicitante());
        assertSame(bruno, a.getDestinatario());
        assertFalse(a.isAceita());
    }

    @Test
    void convidarRecusaEmailVazioDesconhecidoEODoProprioUsuario() {
        assertThrows(IllegalArgumentException.class, () -> service.convidar(ana, " "));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.convidar(ana, "ninguem@teste.com")).getMessage().contains("Não há usuário"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.convidar(ana, "ana@teste.com")).getMessage().contains("a si mesmo"));
        verify(amizades, never()).save(any());
    }

    @Test
    void convidarRecusaQuemJaEAmigoEPedidoRepetido() {
        when(amizades.saoAmigos(1L, 2L)).thenReturn(true);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.convidar(ana, "bruno@teste.com")).getMessage().contains("já são amigos"));

        when(amizades.saoAmigos(1L, 2L)).thenReturn(false);
        when(amizades.findBySolicitanteIdAndDestinatarioId(1L, 2L)).thenReturn(Optional.of(amizade(9L, ana, bruno, false)));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.convidar(ana, "bruno@teste.com")).getMessage().contains("Aguarde"));
    }

    @Test
    void convidarQuemJaConvidouOUsuarioConfirmaAAmizadeNaHora() {
        Amizade pedidoDeBruno = amizade(5L, bruno, ana, false);
        when(amizades.findBySolicitanteIdAndDestinatarioId(2L, 1L)).thenReturn(Optional.of(pedidoDeBruno));

        Amizade a = service.convidar(ana, "bruno@teste.com");

        assertSame(pedidoDeBruno, a);
        assertTrue(a.isAceita());
    }

    @Test
    void somenteODestinatarioAceitaUmPedido() {
        when(amizades.findById(5L)).thenReturn(Optional.of(amizade(5L, bruno, ana, false)));

        assertThrows(IllegalArgumentException.class, () -> service.aceitar(5L, 2L));
        assertThrows(IllegalArgumentException.class, () -> service.aceitar(5L, 3L));
        assertTrue(service.aceitar(5L, 1L).isAceita());
    }

    @Test
    void naoAceitaPedidoInexistenteOuJaAceito() {
        assertThrows(IllegalArgumentException.class, () -> service.aceitar(99L, 1L));
        when(amizades.findById(6L)).thenReturn(Optional.of(amizade(6L, bruno, ana, true)));
        assertThrows(IllegalArgumentException.class, () -> service.aceitar(6L, 1L));
    }

    @Test
    void recusarOuCancelarVaiParaQuemParticipaEApenasEmPedidoPendente() {
        Amizade pendente = amizade(7L, bruno, ana, false);
        when(amizades.findById(7L)).thenReturn(Optional.of(pendente));
        when(amizades.findById(8L)).thenReturn(Optional.of(amizade(8L, bruno, ana, true)));

        assertThrows(IllegalArgumentException.class, () -> service.recusarOuCancelar(7L, 3L));
        assertThrows(IllegalArgumentException.class, () -> service.recusarOuCancelar(8L, 1L));
        service.recusarOuCancelar(7L, 1L);
        verify(amizades).delete(pendente);
    }

    @Test
    void desfazerSoValeParaAmizadeAceitaDeQuemParticipa() {
        Amizade aceita = amizade(8L, bruno, ana, true);
        when(amizades.findById(8L)).thenReturn(Optional.of(aceita));
        when(amizades.findById(9L)).thenReturn(Optional.of(amizade(9L, bruno, ana, false)));

        assertThrows(IllegalArgumentException.class, () -> service.desfazer(8L, 3L));
        assertThrows(IllegalArgumentException.class, () -> service.desfazer(9L, 1L));
        service.desfazer(8L, 2L);
        verify(amizades).delete(aceita);
    }

    @Test
    void amigosVemOrdenadosPeloNomeDaOutraPessoa() {
        when(amizades.aceitasDe(1L)).thenReturn(List.of(amizade(1L, ana, carla, true), amizade(2L, bruno, ana, true)));

        List<AmizadeService.Amigo> amigos = service.amigos(1L);

        assertEquals(List.of("Bruno", "Carla"), amigos.stream().map(a -> a.usuario().getNome()).toList());
        assertEquals(2L, amigos.get(0).amizadeId());
    }
}
