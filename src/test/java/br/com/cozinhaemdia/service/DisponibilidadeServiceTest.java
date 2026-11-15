package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.model.Despensa;
import br.com.cozinhaemdia.model.Ingrediente;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.model.ReceitaIngrediente;
import br.com.cozinhaemdia.repository.DespensaRepository;
import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.ItemFaltante;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testes unitários da comparação entre receitas e despensa, sem banco de dados. */
class DisponibilidadeServiceTest {
    private static final Long USUARIO_ID = 1L;

    private ReceitaService receitaService;
    private DespensaRepository despensaRepository;
    private DisponibilidadeService service;

    private final Ingrediente ovo = ingrediente(1L, "Ovo");
    private final Ingrediente leite = ingrediente(2L, "Leite");
    private final Ingrediente farinha = ingrediente(3L, "Farinha");

    @BeforeEach
    void preparar() {
        receitaService = mock(ReceitaService.class);
        despensaRepository = mock(DespensaRepository.class);
        service = new DisponibilidadeService(receitaService, despensaRepository);
    }

    @Test
    void receitaComTodosOsIngredientesFicaDisponivel() {
        Receita omelete = receita(10L, "Omelete", item(ovo, 2, "unidade"), item(leite, 50, "ml"));
        cenario(List.of(omelete), estoque(ovo, 6, "unidade"), estoque(leite, 200, "ml"));

        AnaliseReceita analise = service.analisar(USUARIO_ID).get(0);

        assertEquals(StatusDisponibilidade.DISPONIVEL, analise.getStatus());
        assertTrue(analise.getFaltantes().isEmpty());
        assertEquals(2, analise.getItensOk());
        assertEquals(2, analise.getTotalItens());
    }

    @Test
    void quantidadeInsuficienteDeixaReceitaParcialComOQueFalta() {
        Receita bolo = receita(10L, "Bolo", item(ovo, 3, "unidade"), item(farinha, 200, "g"));
        cenario(List.of(bolo), estoque(ovo, 1, "unidade"), estoque(farinha, 500, "g"));

        AnaliseReceita analise = service.analisar(USUARIO_ID).get(0);

        assertEquals(StatusDisponibilidade.PARCIAL, analise.getStatus());
        assertEquals(1, analise.getFaltantes().size());
        ItemFaltante faltante = analise.getFaltantes().get(0);
        assertEquals("Ovo", faltante.getNome());
        assertEquals(2, faltante.getQuantidadeFaltante(), 0.0001);
        assertEquals(1, faltante.getQuantidadeDisponivel(), 0.0001);
        assertEquals(1, analise.getItensOk());
    }

    @Test
    void receitaSemNenhumIngredienteNaDespensaFicaIndisponivel() {
        Receita bolo = receita(10L, "Bolo", item(ovo, 3, "unidade"), item(farinha, 200, "g"));
        cenario(List.of(bolo));

        AnaliseReceita analise = service.analisar(USUARIO_ID).get(0);

        assertEquals(StatusDisponibilidade.INDISPONIVEL, analise.getStatus());
        assertEquals(2, analise.getFaltantes().size());
        assertEquals(0, analise.getItensComEstoque());
    }

    @Test
    void unidadeDiferenteNaoEhConsideradaComoEstoque() {
        Receita mingau = receita(10L, "Mingau", item(leite, 300, "ml"));
        cenario(List.of(mingau), estoque(leite, 1, "l"));

        AnaliseReceita analise = service.analisar(USUARIO_ID).get(0);

        assertEquals(StatusDisponibilidade.INDISPONIVEL, analise.getStatus());
        assertEquals(300, analise.getFaltantes().get(0).getQuantidadeFaltante(), 0.0001);
    }

    @Test
    void comparacaoDeUnidadeIgnoraMaiusculas() {
        Receita mingau = receita(10L, "Mingau", item(leite, 300, "ml"));
        cenario(List.of(mingau), estoque(leite, 300, "ML"));

        assertEquals(StatusDisponibilidade.DISPONIVEL, service.analisar(USUARIO_ID).get(0).getStatus());
    }

    @Test
    void resultadoOrdenadoPorStatusEDepoisPorNome() {
        Receita indisponivel = receita(10L, "Arroz doce", item(farinha, 100, "g"));
        Receita parcialB = receita(11L, "Panqueca", item(ovo, 4, "unidade"));
        Receita parcialA = receita(12L, "Bolo", item(ovo, 4, "unidade"));
        Receita disponivel = receita(13L, "Vitamina", item(leite, 100, "ml"));
        cenario(List.of(indisponivel, parcialB, parcialA, disponivel), estoque(ovo, 1, "unidade"), estoque(leite, 100, "ml"));

        List<String> nomes = service.analisar(USUARIO_ID).stream().map(a -> a.getReceita().getNome()).toList();

        assertEquals(List.of("Vitamina", "Bolo", "Panqueca", "Arroz doce"), nomes);
    }

    private void cenario(List<Receita> receitas, Despensa... itensDespensa) {
        when(despensaRepository.findAllByUsuarioIdOrderByIngredienteNomeAsc(USUARIO_ID)).thenReturn(List.of(itensDespensa));
        when(receitaService.listar(USUARIO_ID)).thenReturn(receitas);
        for (Receita r : receitas) when(receitaService.ingredientesDaReceita(r.getId())).thenReturn(r.getIngredientes());
    }

    private static Ingrediente ingrediente(Long id, String nome) {
        Ingrediente ingrediente = new Ingrediente(nome);
        ReflectionTestUtils.setField(ingrediente, "id", id);
        return ingrediente;
    }

    private static Receita receita(Long id, String nome, ReceitaIngrediente... itens) {
        Receita receita = new Receita();
        ReflectionTestUtils.setField(receita, "id", id);
        receita.setNome(nome);
        for (ReceitaIngrediente item : itens) {
            item.setReceita(receita);
            receita.getIngredientes().add(item);
        }
        return receita;
    }

    private static ReceitaIngrediente item(Ingrediente ingrediente, double quantidade, String unidade) {
        return new ReceitaIngrediente(null, ingrediente, quantidade, unidade);
    }

    private static Despensa estoque(Ingrediente ingrediente, double quantidade, String unidade) {
        return new Despensa(null, ingrediente, quantidade, unidade);
    }
}
