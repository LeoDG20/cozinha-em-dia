package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import br.com.cozinhaemdia.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/** Telas de login e cadastro. A autenticação (POST /login) e o logout (POST /logout) são do Spring Security. */
@Controller
public class AuthController {
    /** Limites iguais aos das colunas nome e email da tabela usuarios. */
    private static final int TAMANHO_MAXIMO_NOME = 100;
    private static final int TAMANHO_MAXIMO_EMAIL = 160;
    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder) { this.usuarios = usuarios; this.encoder = encoder; }

    @GetMapping("/login") public String login() { return jaAutenticado() ? "redirect:/" : "login"; }
    @GetMapping("/cadastro") public String cadastro() { return jaAutenticado() ? "redirect:/" : "cadastro"; }

    @PostMapping("/cadastro") public String criarConta(@RequestParam String nome, @RequestParam String email, @RequestParam String senha, @RequestParam String confirmarSenha, Model model, HttpServletRequest request, HttpServletResponse response) {
        if (nome.isBlank() || email.isBlank() || senha.length() < 6) {
            model.addAttribute("erro", "Preencha os campos e use uma senha com pelo menos 6 caracteres."); return "cadastro";
        }
        if (nome.trim().length() > TAMANHO_MAXIMO_NOME) { model.addAttribute("erro", "O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres."); return "cadastro"; }
        if (!emailValido(email.trim())) { model.addAttribute("erro", "Informe um e-mail válido, por exemplo voce@email.com."); return "cadastro"; }
        if (!senha.equals(confirmarSenha)) { model.addAttribute("erro", "As senhas não coincidem."); return "cadastro"; }
        if (usuarios.existsByEmailIgnoreCase(email.trim())) { model.addAttribute("erro", "Já existe uma conta com esse e-mail."); return "cadastro"; }
        Usuario usuario = usuarios.save(new Usuario(nome.trim(), email.trim().toLowerCase(), encoder.encode(senha)));
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

    private static boolean emailValido(String email) {
        return email.length() <= TAMANHO_MAXIMO_EMAIL && FORMATO_EMAIL.matcher(email).matches();
    }

    private boolean jaAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
