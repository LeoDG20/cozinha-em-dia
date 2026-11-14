package br.com.cozinhaemdia.security;

import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carrega o usuário pelo e-mail, na tabela "usuarios", para o Spring Security. */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;

    public CustomUserDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarios.findByEmailIgnoreCase(email.trim())
            .map(UsuarioAutenticado::new)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
