package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder) { this.usuarios = usuarios; this.encoder = encoder; }

    @GetMapping("/login") public String login() { return "login"; }
    @PostMapping("/login") public String autenticar(@RequestParam String email, @RequestParam String senha, HttpSession session, Model model) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (usuario == null || !encoder.matches(senha, usuario.getSenha())) {
            model.addAttribute("erro", "E-mail ou senha inválidos."); return "login";
        }
        session.setAttribute("usuarioId", usuario.getId()); session.setAttribute("usuarioNome", usuario.getNome());
        return "redirect:/";
    }
    @GetMapping("/cadastro") public String cadastro() { return "cadastro"; }
    @PostMapping("/cadastro") public String criarConta(@RequestParam String nome, @RequestParam String email, @RequestParam String senha, @RequestParam String confirmarSenha, Model model, HttpSession session) {
        if (nome.isBlank() || email.isBlank() || senha.length() < 6) {
            model.addAttribute("erro", "Preencha os campos e use uma senha com pelo menos 6 caracteres."); return "cadastro";
        }
        if (!senha.equals(confirmarSenha)) { model.addAttribute("erro", "As senhas não coincidem."); return "cadastro"; }
        if (usuarios.existsByEmailIgnoreCase(email.trim())) { model.addAttribute("erro", "Já existe uma conta com esse e-mail."); return "cadastro"; }
        Usuario usuario = usuarios.save(new Usuario(nome.trim(), email.trim().toLowerCase(), encoder.encode(senha)));
        session.setAttribute("usuarioId", usuario.getId()); session.setAttribute("usuarioNome", usuario.getNome());
        return "redirect:/";
    }
    @PostMapping("/logout") public String sair(HttpSession session) { session.invalidate(); return "redirect:/login"; }
}
