package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Compartilhamento;
import br.com.cozinhaemdia.model.EtapaReceita;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.model.Usuario;
import br.com.cozinhaemdia.repository.CompartilhamentoRepository;
import br.com.cozinhaemdia.repository.ReceitaIngredienteRepository;
import br.com.cozinhaemdia.repository.ReceitaRepository;
import br.com.cozinhaemdia.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Compartilhamento de receitas entre usuários: envio por e-mail, consulta pelo destinatário e cópia para a conta dele. */
@Service
public class CompartilhamentoService {
    private final CompartilhamentoRepository compartilhamentos;
    private final ReceitaRepository receitas;
    private final ReceitaIngredienteRepository receitaIngredientes;
    private final UsuarioRepository usuarios;
    private final ArmazenamentoArquivos arquivos;

    public CompartilhamentoService(CompartilhamentoRepository compartilhamentos, ReceitaRepository receitas,
                                   ReceitaIngredienteRepository receitaIngredientes, UsuarioRepository usuarios,
                                   ArmazenamentoArquivos arquivos) {
        this.compartilhamentos = compartilhamentos; this.receitas = receitas;
        this.receitaIngredientes = receitaIngredientes; this.usuarios = usuarios; this.arquivos = arquivos;
    }

    /** Compartilha a receita com o usuário que tem o e-mail informado. Só o dono pode compartilhar. */
    @Transactional
    public Compartilhamento compartilhar(Receita receita, Usuario origem, String email, String mensagem) {
        if (!receita.getUsuario().getId().equals(origem.getId())) throw new IllegalArgumentException("Você só pode compartilhar as suas próprias receitas.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Informe o e-mail da pessoa que receberá a receita.");
        Usuario destino = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Não há usuário cadastrado com esse e-mail. Peça para a pessoa criar uma conta primeiro."));
        if (destino.getId().equals(origem.getId())) throw new IllegalArgumentException("Você não pode compartilhar uma receita com você mesmo.");
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
        Receita origem = c.getReceita();

        Receita copia = new Receita();
        copia.setUsuario(destinatario);
        copia.setNome(origem.getNome());
        copia.setDescricao(origem.getDescricao());
        copia.setModoPreparo(origem.getModoPreparo());
        copia.setTempoPreparoMin(origem.getTempoPreparoMin());
        copia.setPorcoes(origem.getPorcoes());
        copia.setImagemUrl(origem.getImagemUrl());
        copia.setImagemArquivo(origem.getImagemArquivo() == null ? null : arquivos.copiar(origem.getImagemArquivo()));
        copia.setVideoUrl(origem.getVideoUrl());
        copia.setVideoArquivo(origem.getVideoArquivo() == null ? null : arquivos.copiar(origem.getVideoArquivo()));
        for (EtapaReceita etapa : origem.getEtapas()) {
            String foto = etapa.getImagemArquivo() == null ? null : arquivos.copiar(etapa.getImagemArquivo());
            copia.getEtapas().add(new EtapaReceita(copia, etapa.getOrdem(), etapa.getDescricao(), foto));
        }
        Receita salva = receitas.save(copia);

        List<ReceitaIngrediente> itens = new ArrayList<>();
        for (ReceitaIngrediente item : receitaIngredientes.findAllByReceitaIdOrderByIdAsc(origem.getId()))
            itens.add(new ReceitaIngrediente(salva, item.getIngrediente(), item.getQuantidade(), item.getUnidade()));
        receitaIngredientes.saveAll(itens);

        c.setCopiada(true);
        compartilhamentos.save(c);
        return salva;
    }
}
