package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.CompartilhamentoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Dados da barra de navegação presentes em todas as páginas autenticadas. */
@ControllerAdvice
public class NavegacaoAdvice {
    private final CompartilhamentoService compartilhamentos;

    public NavegacaoAdvice(CompartilhamentoService compartilhamentos) { this.compartilhamentos = compartilhamentos; }

    /** Quantidade de receitas compartilhadas com o usuário que ainda não foram salvas na conta dele. */
    @ModelAttribute("novasCompartilhadas")
    public long novasCompartilhadas() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) return compartilhamentos.novasRecebidas(u.getId());
        return 0;
    }
}
