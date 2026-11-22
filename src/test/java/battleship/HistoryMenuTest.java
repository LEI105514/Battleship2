package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HistoryMenu} (issue #8).
 */
@DisplayName("HistoryMenu")
class HistoryMenuTest {

	@TempDir
	Path tempDir;

	private MoveDatabase database;
	private PrintStream originalOut;
	private ByteArrayOutputStream output;

	@BeforeEach
	void setUp() {
		database = new MoveDatabase(tempDir.resolve("jogadas_test.db").toString());
		originalOut = System.out;
		output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
	}

	private static MoveDatabase.MoveRecord move(int number, MoveDatabase.ShotRecord... shots) {
		return new MoveDatabase.MoveRecord(number, "", 0, 0, 0, 0, 0, "", "", List.of(shots));
	}

	/**
	 * Runs the menu with the given user input and returns everything it printed.
	 */
	private String run(String input) {
		output.reset();
		new HistoryMenu(database, new Scanner(input)).show();
		return output.toString();
	}

	/**
	 * Stores a game with two moves on a known fleet (Barge in A1, Caravel in F6-G6).
	 * The first move sinks the Barge, the second one only hits water.
	 */
	private long gameWithTwoMoves() {
		Fleet fleet = new Fleet();
		fleet.addShip(new Barge(Compass.NORTH, new Position(0, 0)));
		fleet.addShip(new Caravel(Compass.NORTH, new Position(5, 5)));
		Game game = new Game(fleet);
		long gameId = database.startGame();
		game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));
		game.fireShots(List.of(new Position(3, 0), new Position(3, 1), new Position(3, 2)));
		for (IMove m : game.getAlienMoves())
			database.saveMove(gameId, m);
		return gameId;
	}

	private static int occurrences(String text, String fragment) {
		return text.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
	}

	@Nested
	@DisplayName("Construction")
	class Construction {

		@Test
		@DisplayName("A null database is rejected by the assertion")
		void nullDatabase() {
			Scanner in = new Scanner("");
			assertThrows(AssertionError.class, () -> new HistoryMenu(null, in));
		}

		@Test
		@DisplayName("A null scanner is rejected by the assertion")
		void nullScanner() {
			assertThrows(AssertionError.class, () -> new HistoryMenu(database, null));
		}
	}

	@Nested
	@DisplayName("Game list")
	class GameList {

		@Test
		@DisplayName("Empty history returns to the main menu immediately")
		void emptyHistory() {
			String text = run("");

			assertTrue(text.contains("Ainda nao existem jogos guardados."));
			assertFalse(text.contains("Numero do jogo"));
		}

		@Test
		@DisplayName("Running and finished games are listed with their result")
		void listsRunningAndFinishedGames() {
			long finished = database.startGame();
			database.finishGame(finished, 0, MoveDatabase.RESULT_FLEET_SUNK);
			database.startGame();

			String text = run("v");

			assertTrue(text.contains("HISTORICO DE JOGOS"));
			assertTrue(text.contains(MoveDatabase.RESULT_FLEET_SUNK));
			assertTrue(text.contains("em curso"));
		}

		@Test
		@DisplayName("Choosing 'v' goes back to the main menu")
		void backToMainMenu() {
			database.startGame();
			Scanner in = new Scanner("v resto");

			new HistoryMenu(database, in).show();

			assertEquals("resto", in.next());
		}

		@Test
		@DisplayName("Menu returns when the input ends while choosing a game")
		void inputEndsWhileChoosingGame() {
			database.startGame();

			String text = run("");

			assertTrue(text.contains("Numero do jogo a ver"));
		}

		@Test
		@DisplayName("Unknown game numbers and non-numeric choices are rejected")
		void rejectsUnknownGame() {
			database.startGame();

			String text = run("999 abc v");

			assertEquals(2, occurrences(text, "Jogo inexistente"));
		}

		@Test
		@DisplayName("Choices are case-insensitive")
		void choicesIgnoreCase() {
			database.startGame();
			Scanner in = new Scanner("V resto");

			new HistoryMenu(database, in).show();

			assertEquals("resto", in.next());
		}
	}

	@Nested
	@DisplayName("Game replay")
	class GameReplay {

		@Test
		@DisplayName("A game without moves cannot be replayed")
		void gameWithoutMoves() {
			long gameId = database.startGame();

			String text = run(gameId + " v");

			assertTrue(text.contains("O jogo " + gameId + " nao tem jogadas guardadas."));
		}

		@Test
		@DisplayName("User can move between the moves of a game with every command")
		void navigatesThroughGame() {
			long gameId = gameWithTwoMoves();
			Scanner in = new Scanner(gameId + " a s s a p u t xyz v v");
			output.reset();

			new HistoryMenu(database, in).show();

			String text = output.toString();
			assertTrue(text.contains("Ja estas na primeira jogada."));
			assertTrue(text.contains("Ja estas na ultima jogada."));
			assertTrue(text.contains("Comando desconhecido!"));
			assertTrue(text.contains("todas as jogadas"));
			// first shown on entry, after 'a' back, after 'p'; second after 's' and after 'u'
			assertEquals(3, occurrences(text, "Jogada 1 de 2"));
			assertEquals(2, occurrences(text, "Jogada 2 de 2"));
			assertFalse(in.hasNext());
		}

		@Test
		@DisplayName("Move summary shows the shots and the sunk ships")
		void summaryShowsSunkShips() {
			long gameId = gameWithTwoMoves();

			String text = run(gameId + " t v v");

			assertTrue(text.contains("Tiros: A1 F6 J10 -> 3 validos, 1 na agua, 2 em navios, 0 repetidos, 0 exteriores | afundados: Barca"));
			assertTrue(text.contains("Jogada 2: D1 D2 D3"));
		}

		@Test
		@DisplayName("The board of the current move is printed with its legend")
		void printsBoard() {
			long gameId = gameWithTwoMoves();

			String text = run(gameId + " v v");

			assertTrue(text.contains(" 1 2 3 4 5 6 7 8 9 10"));
			assertTrue(text.contains(" A | X . . . . . . . . . |"));
			assertTrue(text.contains(" J | . . . . . . . . . O |"));
			assertTrue(text.contains("-> tiro desta jogada"));
		}

		@Test
		@DisplayName("Moves stored without shot details show no board")
		void moveWithoutDetails() {
			long gameId = database.startGame();
			database.saveMove(gameId, new Move(1, List.of(), List.of()));

			String text = run(gameId + " v v");

			assertTrue(text.contains("tabuleiro indisponivel"));
			assertFalse(text.contains("-> tiro desta jogada"));
		}

		@Test
		@DisplayName("Replay ends when the input ends")
		void inputEndsDuringReplay() {
			long gameId = gameWithTwoMoves();

			String text = run(String.valueOf(gameId));

			assertTrue(text.contains("Jogada 1 de 2"));
			assertFalse(text.contains("Jogada 2 de 2"));
		}
	}

	@Nested
	@DisplayName("buildBoard")
	class BuildBoard {

		@Test
		@DisplayName("Board shows older shots and highlights the current move")
		void buildBoardMarksShots() {
			List<MoveDatabase.MoveRecord> moves = List.of(
					move(1, new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_HIT, "Barca"),
							new MoveDatabase.ShotRecord(1, 1, MoveDatabase.SHOT_WATER, null)),
					move(2, new MoveDatabase.ShotRecord(2, 2, MoveDatabase.SHOT_SUNK, "Barca"),
							new MoveDatabase.ShotRecord(3, 3, MoveDatabase.SHOT_WATER, null),
							new MoveDatabase.ShotRecord(15, 15, MoveDatabase.SHOT_OUTSIDE, null)));

			char[][] first = HistoryMenu.buildBoard(moves, 0);
			assertEquals(HistoryMenu.NEW_HIT_MARKER, first[0][0]);
			assertEquals(HistoryMenu.NEW_WATER_MARKER, first[1][1]);
			assertEquals(HistoryMenu.EMPTY_MARKER, first[2][2]);

			char[][] second = HistoryMenu.buildBoard(moves, 1);
			assertEquals(HistoryMenu.OLD_HIT_MARKER, second[0][0]);
			assertEquals(HistoryMenu.OLD_WATER_MARKER, second[1][1]);
			assertEquals(HistoryMenu.NEW_HIT_MARKER, second[2][2]);
			assertEquals(HistoryMenu.NEW_WATER_MARKER, second[3][3]);
		}

		@Test
		@DisplayName("Repeated shots do not overwrite the original mark")
		void repeatedShotsAreIgnored() {
			List<MoveDatabase.MoveRecord> moves = List.of(
					move(1, new MoveDatabase.ShotRecord(4, 4, MoveDatabase.SHOT_HIT, "Nau")),
					move(2, new MoveDatabase.ShotRecord(4, 4, MoveDatabase.SHOT_REPEATED, null)));

			char[][] board = HistoryMenu.buildBoard(moves, 1);

			assertEquals(HistoryMenu.OLD_HIT_MARKER, board[4][4]);
		}

		@Test
		@DisplayName("Board has the game size and is empty without shots")
		void emptyBoard() {
			char[][] board = HistoryMenu.buildBoard(List.of(move(1)), 0);

			assertEquals(Game.BOARD_SIZE, board.length);
			for (char[] row : board)
				for (char cell : row)
					assertEquals(HistoryMenu.EMPTY_MARKER, cell);
		}
	}
}
