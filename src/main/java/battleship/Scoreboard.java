package battleship;

import com.github.freva.asciitable.AsciiTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Scoreboard with the ranking of the games played so far.
 *
 * <p>Reads the stored games from the SQLite database ({@link MoveDatabase})
 * and prints a ranking to the console, rendered as an ASCII table using the
 * external library {@code com.github.freva:ascii-table}.</p>
 */
public class Scoreboard {

    /** Result stored by the game when the whole enemy fleet was sunk (a win). */
    private static final String WIN_RESULT = "Frota afundada";

    /** Database that holds the games, moves and shots. */
    private final MoveDatabase database;

    /**
     * Creates a scoreboard backed by the given database.
     *
     * @param database the database to read the games from
     */
    public Scoreboard(MoveDatabase database) {
        this.database = database;
    }

    /**
     * Computes the statistics of every stored game and prints the ranking.
     */
    public void show() {
        List<MoveDatabase.GameRecord> games = database.listGames();

        if (games.isEmpty()) {
            System.out.println("Ainda nao ha jogos guardados para mostrar no scoreboard.");
            return;
        }

        // Compute the statistics of each game from its moves.
        List<Row> rows = new ArrayList<>();
        for (MoveDatabase.GameRecord game : games) {
            int hits = 0;
            int sunk = 0;
            for (MoveDatabase.MoveRecord move : database.listMoves(game.id())) {
                hits += move.hits();
                sunk += countSunk(move.sunk());
            }
            rows.add(new Row(game, hits, sunk));
        }

        // Ranking: won games first, then fewer volleys (moves) is better.
        rows.sort(Comparator
                .comparingInt((Row r) -> WIN_RESULT.equals(r.game.result()) ? 0 : 1)
                .thenComparingInt(r -> r.game.moves()));

        // Build the table.
        String[] headers = {
                "Pos", "Jogo", "Inicio", "Resultado", "Rajadas", "Tiros certeiros", "Navios afundados"
        };
        String[][] data = new String[rows.size()][headers.length];
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            data[i][0] = String.valueOf(i + 1);
            data[i][1] = String.valueOf(r.game.id());
            data[i][2] = r.game.start();
            data[i][3] = r.game.result() == null ? "(em curso)" : r.game.result();
            data[i][4] = String.valueOf(r.game.moves());
            data[i][5] = String.valueOf(r.hits);
            data[i][6] = String.valueOf(r.sunk);
        }

        System.out.println();
        System.out.println(AsciiTable.getTable(headers, data));
    }

    /**
     * Counts the ship types listed in a comma-separated "sunk ships" field.
     *
     * @param sunk the comma-separated list of sunk ship types (may be null/blank)
     * @return the number of ships listed
     */
    private int countSunk(String sunk) {
        if (sunk == null || sunk.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String part : sunk.split(",")) {
            if (!part.isBlank()) {
                count++;
            }
        }
        return count;
    }

    /** Holds a game together with its computed statistics. */
    private static final class Row {
        private final MoveDatabase.GameRecord game;
        private final int hits;
        private final int sunk;

        Row(MoveDatabase.GameRecord game, int hits, int sunk) {
            this.game = game;
            this.hits = hits;
            this.sunk = sunk;
        }
    }
}