package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MoveDatabase} (issue #8), using a temporary SQLite file.
 */
class MoveDatabaseTest {

	@TempDir
	Path tempDir;

	private MoveDatabase database;
	private Path dbFile;

	@BeforeEach
	void setUp() {
		dbFile = tempDir.resolve("sub/jogadas_test.db");
		database = new MoveDatabase(dbFile.toString());
	}

	@Test
	@DisplayName("Database file is created on construction")
	void createsDatabaseFile() {
		assertTrue(dbFile.toFile().exists());
	}

	@Test
	@DisplayName("Each new game gets a different id")
	void startGameReturnsNewIds() {
		long first = database.startGame();
		long second = database.startGame();
		assertTrue(first > 0);
		assertNotEquals(first, second);
	}

	@Test
	@DisplayName("Moves of a game are stored")
	void savesMoves() {
		Game game = new Game(Fleet.createRandom());
		long gameId = database.startGame();

		game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));
		game.fireShots(List.of(new Position(1, 1), new Position(1, 1), new Position(20, 20)));
		for (IMove move : game.getAlienMoves())
			database.saveMove(gameId, move);

		assertEquals(2, database.countMoves(gameId));
		assertEquals(0, database.countMoves(database.startGame()));
	}

	@Test
	@DisplayName("Moves with an invalid game id are ignored")
	void ignoresInvalidGameId() {
		Game game = new Game(Fleet.createRandom());
		game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));

		assertDoesNotThrow(() -> database.saveMove(-1, game.getAlienMoves().get(0)));
		assertEquals(0, database.countMoves(-1));
	}

	@Test
	@DisplayName("Finished games are listed with their result and number of moves")
	void listsGames() {
		Game game = new Game(Fleet.createRandom());
		long finished = database.startGame();
		game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));
		database.saveMove(finished, game.getAlienMoves().get(0));
		database.finishGame(finished, 1, MoveDatabase.RESULT_QUIT);
		long running = database.startGame();

		List<MoveDatabase.GameRecord> games = database.listGames();

		assertEquals(2, games.size());
		assertEquals(finished, games.get(0).id());
		assertEquals(1, games.get(0).moves());
		assertEquals(MoveDatabase.RESULT_QUIT, games.get(0).result());
		assertNotNull(games.get(0).end());
		assertEquals(running, games.get(1).id());
		assertNull(games.get(1).result());
	}

	@Test
	@DisplayName("Moves are read back with the outcome of each shot")
	void listsMovesWithShots() {
		Game game = new Game(Fleet.createRandom());
		long gameId = database.startGame();
		game.fireShots(List.of(new Position(0, 0), new Position(0, 0), new Position(20, 20)));
		database.saveMove(gameId, game.getAlienMoves().get(0));

		List<MoveDatabase.MoveRecord> moves = database.listMoves(gameId);

		assertEquals(1, moves.size());
		MoveDatabase.MoveRecord move = moves.get(0);
		assertEquals(1, move.number());
		assertEquals("A1 A1 U21", move.shots());
		assertEquals(1, move.valid());
		assertEquals(1, move.repeated());
		assertEquals(1, move.outside());
		assertEquals(3, move.details().size());
		assertEquals("A1", move.details().get(0).label());
		assertEquals(MoveDatabase.SHOT_REPEATED, move.details().get(1).outcome());
		assertEquals(MoveDatabase.SHOT_OUTSIDE, move.details().get(2).outcome());
	}
}
