
package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoreboardTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve apresentar uma mensagem quando não existem jogos guardados")
    void testShowWithoutGames() {

        // Criar uma base de dados temporária apenas para este teste
        String dbPath = tempDir.resolve("scoreboard-test.db").toString();
        MoveDatabase database = new MoveDatabase(dbPath);
        Scoreboard scoreboard = new Scoreboard(database);

        // Capturar o texto apresentado na consola
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream capture = new PrintStream(
                output, true, StandardCharsets.UTF_8)) {

            System.setOut(capture);
            scoreboard.show();

        } finally {
            System.setOut(originalOut);
        }

        // Verificar a mensagem apresentada
        assertTrue(output.toString(StandardCharsets.UTF_8)
                .contains("Ainda nao ha jogos guardados para mostrar no scoreboard."));
    }
    @Test
    @DisplayName("Deve apresentar uma vitória guardada na classificação")
    void testShowWithSavedGame() {

        // Criar uma base de dados temporária
        String dbPath = tempDir.resolve("scoreboard-with-game.db").toString();
        MoveDatabase database = new MoveDatabase(dbPath);

        // Criar e terminar um jogo com vitória
        long gameId = database.startGame();
        assertTrue(gameId > 0);

        database.finishGame(
                gameId,
                0,
                MoveDatabase.RESULT_FLEET_SUNK
        );

        Scoreboard scoreboard = new Scoreboard(database);

        // Capturar o texto da consola
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream capture = new PrintStream(
                output, true, StandardCharsets.UTF_8)) {

            System.setOut(capture);
            scoreboard.show();

        } finally {
            System.setOut(originalOut);
        }

        String resultado = output.toString(StandardCharsets.UTF_8);

        // Confirmar que a classificação apresenta o jogo
        assertTrue(resultado.contains("Frota afundada"));
        assertTrue(resultado.contains("Navios afundados"));
    }
    @Test
    @DisplayName("Deve contar navios afundados ignorando valores vazios")
    void testCountSunk() throws Exception {

        String dbPath = tempDir.resolve("count-sunk-test.db").toString();
        Scoreboard scoreboard = new Scoreboard(new MoveDatabase(dbPath));

        var metodo = Scoreboard.class.getDeclaredMethod(
                "countSunk", String.class
        );
        metodo.setAccessible(true);

        // Sem informação sobre navios afundados
        assertEquals(0, (int) metodo.invoke(scoreboard, (Object) null));
        assertEquals(0, (int) metodo.invoke(scoreboard, "   "));

        // Ignorar elementos vazios
        assertEquals(0, (int) metodo.invoke(scoreboard, ", ,"));

        // Contar dois navios válidos
        assertEquals(2, (int) metodo.invoke(
                scoreboard, "Barca, ,Caravela"
        ));
    }
    @Test
    @DisplayName("Deve mostrar jogos com jogadas e resultados diferentes")
    void testShowWithMovesAndOngoingGame() {

        MoveDatabase database = new MoveDatabase(
                tempDir.resolve("scoreboard-ranking.db").toString()
        ) {
            @Override
            public List<GameRecord> listGames() {
                return List.of(
                        new GameRecord(1L, "2026-11-09", null, 1, null),
                        new GameRecord(2L, "2026-11-09",
                                "2026-11-09", 0, "Frota afundada")
                );
            }

            @Override
            public List<MoveRecord> listMoves(long gameId) {
                if (gameId == 1L) {
                    return List.of(new MoveRecord(
                            1, "A1 B2", 2, 0, 0, 0,
                            2, "Barca, Caravela",
                            "2026-11-09", List.of()
                    ));
                }
                return List.of();
            }
        };

        Scoreboard scoreboard = new Scoreboard(database);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream capture = new PrintStream(
                output, true, StandardCharsets.UTF_8)) {

            System.setOut(capture);
            scoreboard.show();

        } finally {
            System.setOut(originalOut);
        }

        String resultado = output.toString(StandardCharsets.UTF_8);

        assertTrue(resultado.contains("(em curso)"));
        assertTrue(resultado.contains("Frota afundada"));
        assertTrue(resultado.contains("Tiros certeiros"));
    }
}
