package br.com.cozinhaemdia.security;

import br.com.cozinhaemdia.model.Usuario;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/** Usuário autenticado: carrega id e nome para os controllers e templates, sem nova consulta ao banco. */
public class UsuarioAutenticado extends User {
    private final Long id;
    private final String nome;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenha(), AuthorityUtils.createAuthorityList("ROLE_USER"));
        this.id = usuario.getId();
        this.nome = usuario.getNome();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
