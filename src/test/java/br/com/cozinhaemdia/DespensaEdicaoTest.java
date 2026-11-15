package br.com.cozinhaemdia;

import br.com.cozinhaemdia.model.Despensa;
import br.com.cozinhaemdia.repository.DespensaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Edição de itens da despensa e conversão de unidades, de ponta a ponta (HTTP até o PostgreSQL). */
@SpringBootTest
@AutoConfigureMockMvc
class DespensaEdicaoTest {
    @Autowired MockMvc mvc;
    @Autowired DespensaRepository despensa;

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) throws Exception {
        BancoPostgresDeTeste.configurar(registro);
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void editarExigeCsrfEAlteraQuantidadeEUnidade() throws Exception {
        mvc.perform(post("/despensa").param("nome", "Fubá teste").param("quantidade", "200").param("unidade", "g").with(csrf()))
            .andExpect(redirectedUrl("/despensa"));
        Long id = idDo("Fubá teste");

        mvc.perform(post("/despensa/editar").param("itemId", id.toString()).param("quantidade", "3").param("unidade", "kg"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/despensa/editar").param("itemId", id.toString()).param("quantidade", "3").param("unidade", "kg").with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/despensa"))
            .andExpect(flash().attribute("sucesso", "Ingrediente atualizado."));

        Despensa item = despensa.findById(id).orElseThrow();
        assertEquals(3.0, item.getQuantidade(), 0.0001);
        assertEquals("kg", item.getUnidade());
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void editarRejeitaQuantidadeInvalidaEItemInexistente() throws Exception {
        mvc.perform(post("/despensa").param("nome", "Polvilho teste").param("quantidade", "100").param("unidade", "g").with(csrf()));
        Long id = idDo("Polvilho teste");

        mvc.perform(post("/despensa/editar").param("itemId", id.toString()).param("quantidade", "0").param("unidade", "g").with(csrf()))
            .andExpect(flash().attribute("erro", "A quantidade deve ser maior que zero."));
        mvc.perform(post("/despensa/editar").param("itemId", "999999").param("quantidade", "1").param("unidade", "g").with(csrf()))
            .andExpect(flash().attribute("erro", "Item não encontrado na sua despensa."));

        assertEquals(100.0, despensa.findById(id).orElseThrow().getQuantidade(), 0.0001);
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void adicionarSomaConvertendoKgParaG() throws Exception {
        mvc.perform(post("/despensa").param("nome", "Amido teste").param("quantidade", "500").param("unidade", "g").with(csrf()));
        mvc.perform(post("/despensa").param("nome", "Amido teste").param("quantidade", "1").param("unidade", "kg").with(csrf()))
            .andExpect(flash().attribute("sucesso", "Ingrediente adicionado à despensa."));

        Despensa item = despensa.findById(idDo("Amido teste")).orElseThrow();
        assertEquals(1500.0, item.getQuantidade(), 0.0001);
        assertEquals("g", item.getUnidade());
    }

    @Test
    @WithUserDetails("demo@cozinha.local")
    void paginaDaDespensaMostraFormularioDeEdicao() throws Exception {
        mvc.perform(get("/despensa")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/despensa/editar")));
    }

    private Long idDo(String nome) {
        return despensa.findAll().stream()
            .filter(d -> d.getIngrediente().getNome().equalsIgnoreCase(nome)).findFirst().orElseThrow().getId();
    }
}
