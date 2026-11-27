package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

/** Contas de usuário: consulta e cadastro, com a validação dos dados informados. */
@Service
public class UsuarioService {
    /** Limites iguais aos das colunas nome e email da tabela usuarios. */
    public static final int TAMANHO_MAXIMO_NOME = 100;
    public static final int TAMANHO_MAXIMO_EMAIL = 160;
    public static final int TAMANHO_MINIMO_SENHA = 6;
    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.encoder = encoder;
    }

    /** Usuário pelo identificador; falha se ele não existir. */
    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new IllegalStateException("Usuário não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> encontrar(Long id) { return usuarios.findById(id); }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email.trim());
    }

    /** Valida os dados e cria a conta com a senha codificada em BCrypt. */
    @Transactional
    public Usuario cadastrar(String nome, String email, String senha, String confirmarSenha) {
        if (nome == null || nome.isBlank() || email == null || email.isBlank() || senha == null || senha.length() < TAMANHO_MINIMO_SENHA)
            throw new NegocioException("erro.cadastro.campos", "Preencha os campos e use uma senha com pelo menos 6 caracteres.");
        if (nome.trim().length() > TAMANHO_MAXIMO_NOME)
            throw new NegocioException("erro.cadastro.nome.longo", "O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.", TAMANHO_MAXIMO_NOME);
        if (!emailValido(email.trim())) throw new NegocioException("erro.cadastro.email.invalido", "Informe um e-mail válido, por exemplo voce@email.com.");
        if (!senha.equals(confirmarSenha)) throw new NegocioException("erro.cadastro.senhas", "As senhas não coincidem.");
        if (usuarios.existsByEmailIgnoreCase(email.trim())) throw new NegocioException("erro.cadastro.email.existe", "Já existe uma conta com esse e-mail.");
        return usuarios.save(new Usuario(nome.trim(), email.trim().toLowerCase(), encoder.encode(senha)));
    }

    private static boolean emailValido(String email) {
        return email.length() <= TAMANHO_MAXIMO_EMAIL && FORMATO_EMAIL.matcher(email).matches();
    }
}
