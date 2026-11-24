package br.com.cozinhaemdia.service;

import br.com.cozinhaemdia.service.ListaDeCompras.Compra;
import br.com.cozinhaemdia.model.Receita;
import br.com.cozinhaemdia.service.DisponibilidadeService.AnaliseReceita;
import br.com.cozinhaemdia.service.DisponibilidadeService.ItemFaltante;
import br.com.cozinhaemdia.service.DisponibilidadeService.StatusDisponibilidade;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras da lista de mercado montada a partir do que falta nas receitas. */
class ListaDeComprasTest {
    private static Receita receita(String nome) {
        Receita r = new Receita();
        r.setNome(nome);
        return r;
    }

    private static AnaliseReceita analise(String nome, StatusDisponibilidade status, ItemFaltante... faltantes) {
        return new AnaliseReceita(receita(nome), status, List.of(faltantes), 0, faltantes.length, 0);
    }

    @Test
    void somaOQueFaltaEntreReceitasDoMesmoIngredienteEUnidade() {
        List<Compra> compras = ListaDeCompras.montar(List.of(
                analise("Bolo", StatusDisponibilidade.PARCIAL, new ItemFaltante("Açúcar", 200, "g", 0)),
                analise("Pudim", StatusDisponibilidade.PARCIAL, new ItemFaltante("açúcar", 250, "g", 0))));

        assertEquals(1, compras.size());
        assertEquals(450.0, compras.get(0).quantidade());
        assertEquals(List.of("Bolo", "Pudim"), compras.get(0).receitas());
        assertEquals("450 g", compras.get(0).quantidadeTexto());
        assertEquals("450 g de Açúcar", compras.get(0).linhaTexto());
    }

    @Test
    void unidadesDiferentesNaoSaoSomadas() {
        List<Compra> compras = ListaDeCompras.montar(List.of(
                analise("Bolo", StatusDisponibilidade.PARCIAL, new ItemFaltante("Leite", 200, "ml", 0)),
                analise("Pudim", StatusDisponibilidade.PARCIAL, new ItemFaltante("Leite", 1, "xícara", 0))));

        assertEquals(2, compras.size());
    }

    @Test
    void receitasProntasNaoEntramNaLista() {
        List<Compra> compras = ListaDeCompras.montar(List.of(
                analise("Omelete", StatusDisponibilidade.DISPONIVEL),
                analise("Bolo", StatusDisponibilidade.INDISPONIVEL, new ItemFaltante("Ovo", 3, "unidade", 0))));

        assertEquals(1, compras.size());
        assertEquals("Ovo", compras.get(0).nome());
    }

    @Test
    void ordenaPeloIngredienteUsadoEmMaisReceitasDepoisPeloNome() {
        List<Compra> compras = ListaDeCompras.montar(List.of(
                analise("A", StatusDisponibilidade.PARCIAL, new ItemFaltante("Zimbro", 1, "g", 0), new ItemFaltante("Óleo", 10, "ml", 0)),
                analise("B", StatusDisponibilidade.PARCIAL, new ItemFaltante("Óleo", 10, "ml", 0), new ItemFaltante("Alho", 1, "dente", 0))));

        assertEquals(List.of("Óleo", "Alho", "Zimbro"), compras.stream().map(Compra::nome).toList());
        assertEquals(List.of("A", "B"), compras.get(0).receitas());
    }

    @Test
    void mesmaReceitaNaoApareceDuasVezesNaOrigem() {
        List<Compra> compras = ListaDeCompras.montar(List.of(
                analise("Bolo", StatusDisponibilidade.PARCIAL, new ItemFaltante("Ovo", 1, "unidade", 0), new ItemFaltante("Ovo", 2, "unidade", 0))));

        assertEquals(1, compras.get(0).receitas().size());
        assertEquals(3.0, compras.get(0).quantidade());
    }

    @Test
    void valorDoFormularioTrazQuantidadeUnidadeENome() {
        Compra c = new Compra("Farinha de trigo", 250.5, "g", List.of("Bolo"));

        assertEquals("250.5|g|Farinha de trigo", c.valorFormulario());
    }

    @Test
    void listaVaziaQuandoNaoFaltaNada() {
        assertTrue(ListaDeCompras.montar(List.of()).isEmpty());
    }
}
