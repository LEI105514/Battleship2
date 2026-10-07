package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("User can choose a game, move between its moves and go back")
	void navigatesThroughGame() {
		Game game = new Game(Fleet.createRandom());
		long gameId = database.startGame();
		game.fireShots(List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2)));
		game.fireShots(List.of(new Position(5, 0), new Position(5, 1), new Position(5, 2)));
		for (IMove m : game.getAlienMoves())
			database.saveMove(gameId, m);
		output.reset();

		Scanner in = new Scanner(gameId + " s s a t v v");
		new HistoryMenu(database, in).show();

		String text = output.toString();
		assertTrue(text.contains("Jogada 1 de 2"));
		assertTrue(text.contains("Jogada 2 de 2"));
		assertTrue(text.contains("Ja estas na ultima jogada."));
		assertTrue(text.contains("todas as jogadas"));
		assertFalse(in.hasNext());
	}

	@Test
	@DisplayName("Unknown game numbers are rejected")
	void rejectsUnknownGame() {
		database.startGame();

		new HistoryMenu(database, new Scanner("999 abc v")).show();

		String text = output.toString();
		assertEquals(2, text.split("Jogo inexistente", -1).length - 1);
	}

	@Test
	@DisplayName("Empty history returns to the main menu immediately")
	void emptyHistory() {
		new HistoryMenu(database, new Scanner("")).show();

		assertTrue(output.toString().contains("Ainda nao existem jogos guardados."));
	}
}
