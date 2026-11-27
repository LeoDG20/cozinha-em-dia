package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Compartilhamento;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.CompartilhamentoRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Compartilhamento de receitas entre amigos: envio, consulta pelo destinatário e cópia para a conta dele. */
@Service
public class CompartilhamentoService {
    private final CompartilhamentoRepository compartilhamentos;
    private final ReceitaRepository receitas;
    private final UsuarioRepository usuarios;
    private final CopiaReceitaService copias;
    private final AmizadeService amizades;

    @Autowired
    public CompartilhamentoService(CompartilhamentoRepository compartilhamentos, ReceitaRepository receitas,
                                   UsuarioRepository usuarios, CopiaReceitaService copias, AmizadeService amizades) {
        this.compartilhamentos = compartilhamentos; this.receitas = receitas;
        this.usuarios = usuarios; this.copias = copias; this.amizades = amizades;
    }

    /** Compartilha a receita com um amigo (identificado pelo e-mail). Só o dono pode compartilhar e só com amigos. */
    @Transactional
    public Compartilhamento compartilhar(Receita receita, Usuario origem, String email, String mensagem) {
        if (!receita.getUsuario().getId().equals(origem.getId())) throw new IllegalArgumentException("Você só pode compartilhar as suas próprias receitas.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Informe o e-mail da pessoa que receberá a receita.");
        Usuario destino = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Não há usuário cadastrado com esse e-mail. Peça para a pessoa criar uma conta primeiro."));
        if (destino.getId().equals(origem.getId())) throw new IllegalArgumentException("Você não pode compartilhar uma receita com você mesmo.");
        if (!amizades.saoAmigos(origem.getId(), destino.getId()))
            throw new IllegalArgumentException(destino.getNome() + " ainda não é seu amigo. Envie um pedido na página Amigos para poder compartilhar receitas.");
        if (compartilhamentos.existsByReceitaIdAndDestinoId(receita.getId(), destino.getId()))
            throw new IllegalArgumentException("Esta receita já foi compartilhada com " + destino.getNome() + ".");
        String nota = mensagem == null ? "" : mensagem.trim();
        if (nota.length() > 200) throw new IllegalArgumentException("A mensagem deve ter no máximo 200 caracteres.");
        return compartilhamentos.save(new Compartilhamento(receita, destino, nota.isEmpty() ? null : nota));
    }

    @Transactional(readOnly = true)
    public List<Compartilhamento> recebidas(Long usuarioId) {
        return compartilhamentos.findAllByDestinoIdOrderByCriadoEmDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Compartilhamento> enviadasDa(Long receitaId) {
        return compartilhamentos.findAllByReceitaIdOrderByCriadoEmDesc(receitaId);
    }

    /** Nomes das pessoas com quem o usuário compartilhou cada uma de suas receitas, indexados pelo id da receita. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> nomesDosDestinatarios(Long donoId) {
        Map<Long, List<String>> mapa = new LinkedHashMap<>();
        for (Compartilhamento c : compartilhamentos.findAllByReceitaUsuarioIdOrderByDestinoNomeAsc(donoId))
            mapa.computeIfAbsent(c.getReceita().getId(), k -> new java.util.ArrayList<>()).add(c.getDestino().getNome());
        return mapa;
    }

    @Transactional(readOnly = true)
    public long novasRecebidas(Long usuarioId) {
        return compartilhamentos.countByDestinoIdAndCopiadaFalse(usuarioId);
    }

    /** A receita, se pertencer ao usuário ou tiver sido compartilhada com ele. */
    @Transactional(readOnly = true)
    public Optional<Receita> visivelPara(Long receitaId, Long usuarioId) {
        Optional<Receita> propria = receitas.findByIdAndUsuarioId(receitaId, usuarioId);
        if (propria.isPresent()) return propria;
        if (compartilhamentos.existsByReceitaIdAndDestinoId(receitaId, usuarioId)) return receitas.findById(receitaId);
        return Optional.empty();
    }

    /** Desfaz um compartilhamento; pode ser feito pelo destinatário (dispensar) ou pelo dono da receita (revogar). */
    @Transactional
    public boolean remover(Long compartilhamentoId, Long usuarioId) {
        Compartilhamento c = compartilhamentos.findById(compartilhamentoId).orElse(null);
        if (c == null) return false;
        boolean destinatario = c.getDestino().getId().equals(usuarioId);
        boolean dono = c.getReceita().getUsuario().getId().equals(usuarioId);
        if (!destinatario && !dono) return false;
        compartilhamentos.delete(c);
        return true;
    }

    /** Salva, na conta do destinatário, uma cópia independente da receita compartilhada (ingredientes, etapas e arquivos). */
    @Transactional
    public Receita salvarCopia(Long receitaId, Usuario destinatario) {
        Compartilhamento c = compartilhamentos.findByReceitaIdAndDestinoId(receitaId, destinatario.getId())
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada entre as compartilhadas com você."));
        if (c.isCopiada()) throw new IllegalArgumentException("Você já salvou esta receita na sua conta.");
        Receita salva = copias.copiar(c.getReceita(), destinatario);

        c.setCopiada(true);
        compartilhamentos.save(c);
        return salva;
    }
}
