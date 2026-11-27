package br.com.cozinhaemdia.security;

import br.com.cozinhaemdia.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carrega o usuário pelo e-mail, na tabela "usuarios", para o Spring Security. */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioService usuarios;

    @Autowired
    public CustomUserDetailsService(UsuarioService usuarios) { this.usuarios = usuarios; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarios.buscarPorEmail(email)
            .map(UsuarioAutenticado::new)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
