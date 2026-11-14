package br.com.cozinhaemdia;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Garante que os scripts de database/ são válidos em PostgreSQL. */
class ScriptsSqlTest {

    @Test
    void esquemaEDadosDeExemploSaoExecutadosSemErro() throws Exception {
        var servidor = BancoPostgresDeTeste.servidor();
        try (Connection admin = servidor.getPostgresDatabase().getConnection(); Statement st = admin.createStatement()) {
            st.execute("CREATE DATABASE teste_scripts");
        }
        try (Connection conexao = servidor.getDatabase("postgres", "teste_scripts").getConnection()) {
            ScriptUtils.executeSqlScript(conexao, new FileSystemResource("database/schema.sql"));
            ScriptUtils.executeSqlScript(conexao, new FileSystemResource("database/sample_data.sql"));
            assertEquals(1, contar(conexao, "usuarios"));
            assertEquals(20, contar(conexao, "ingredientes"));
            assertEquals(6, contar(conexao, "receitas"));
            assertEquals(26, contar(conexao, "receita_ingredientes"));
            assertEquals(13, contar(conexao, "despensa"));
            // a sequência foi ajustada: um novo registro não pode colidir com os ids fixos
            try (Statement st = conexao.createStatement()) {
                st.execute("INSERT INTO ingredientes (nome) VALUES ('Pimenta')");
            }
        }
    }

    private static int contar(Connection conexao, String tabela) throws Exception {
        try (Statement st = conexao.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
