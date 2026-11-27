package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import br.com.cozinhaemdia.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Telas de login e cadastro. A autenticação (POST /login) e o logout (POST /logout) são do Spring Security. */
@Controller
public class AuthController {
    private final UsuarioService usuarios;

    @Autowired
    public AuthController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping("/login") public String login() { return jaAutenticado() ? "redirect:/" : "login"; }
    @GetMapping("/cadastro") public String cadastro() { return jaAutenticado() ? "redirect:/" : "cadastro"; }

    @PostMapping("/cadastro") public String criarConta(@RequestParam String nome, @RequestParam String email, @RequestParam String senha, @RequestParam String confirmarSenha, Model model, HttpServletRequest request, HttpServletResponse response) {
        Usuario usuario;
        try { usuario = usuarios.cadastrar(nome, email, senha, confirmarSenha); }
        catch (IllegalArgumentException ex) { model.addAttribute("erro", ex.getMessage()); return "cadastro"; }
        autenticar(usuario, request, response);
        return "redirect:/";
    }

    /** Após o cadastro, o usuário é autenticado automaticamente. */
    private void autenticar(Usuario usuario, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(contexto);
        new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
    }

    private boolean jaAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
