package br.com.cozinhaemdia;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.io.IOException;

/**
 * PostgreSQL embutido usado nos testes. Sobe um servidor real e descartável,
 * portanto os testes não dependem de Docker nem do banco de desenvolvimento.
 */
final class BancoPostgresDeTeste {
    private static EmbeddedPostgres servidor;

    private BancoPostgresDeTeste() {}

    static synchronized EmbeddedPostgres servidor() throws IOException {
        if (servidor == null) {
            servidor = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { servidor.close(); } catch (IOException ignorado) { }
            }));
        }
        return servidor;
    }

    static void configurar(DynamicPropertyRegistry registro) throws IOException {
        int porta = servidor().getPort();
        registro.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + porta + "/postgres");
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }
}
