package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MoveDatabase} (issue #8), using a temporary SQLite file.
 */
@DisplayName("MoveDatabase")
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

	/**
	 * Builds a game with a known fleet: a Barge in A1 and a Caravel in F6-G6.
	 */
	private static Game knownGame() {
		Fleet fleet = new Fleet();
		fleet.addShip(new Barge(Compass.NORTH, new Position(0, 0)));
		fleet.addShip(new Caravel(Compass.NORTH, new Position(5, 5)));
		return new Game(fleet);
	}

	@Nested
	@DisplayName("Construction")
	class Construction {

		@Test
		@DisplayName("Database file and parent directory are created on construction")
		void createsDatabaseFile() {
			assertTrue(dbFile.toFile().exists());
		}

		@Test
		@DisplayName("Opening an existing database keeps its data")
		void reopensExistingDatabase() {
			long id = database.startGame();

			MoveDatabase reopened = new MoveDatabase(dbFile.toString());

			assertEquals(1, reopened.listGames().size());
			assertEquals(id, reopened.listGames().get(0).id());
		}

		@Test
		@DisplayName("Default constructor uses the default database file")
		void defaultConstructor() {
			MoveDatabase defaultDb = new MoveDatabase();

			assertNotNull(defaultDb.listGames());
			assertTrue(Path.of(MoveDatabase.DEFAULT_DB_FILE).toFile().exists());
		}

		@Test
		@DisplayName("A null file name is rejected by the assertion")
		void nullFileRejected() {
			assertThrows(AssertionError.class, () -> new MoveDatabase(null));
		}
	}

	@Nested
	@DisplayName("Games")
	class Games {

		@Test
		@DisplayName("Each new game gets a different, positive id")
		void startGameReturnsNewIds() {
			long first = database.startGame();
			long second = database.startGame();
			assertTrue(first > 0);
			assertNotEquals(first, second);
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
			assertNotNull(games.get(0).start());
			assertNotNull(games.get(0).end());
			assertEquals(running, games.get(1).id());
			assertNull(games.get(1).end());
			assertNull(games.get(1).result());
		}

		@Test
		@DisplayName("Finishing a game with an invalid id changes nothing")
		void finishGameIgnoresInvalidId() {
			long id = database.startGame();

			database.finishGame(-1, 5, MoveDatabase.RESULT_FLEET_SUNK);

			assertNull(database.listGames().get(0).result());
			assertEquals(id, database.listGames().get(0).id());
		}

		@Test
		@DisplayName("An empty database has no games")
		void emptyDatabase() {
			assertTrue(database.listGames().isEmpty());
		}
	}

	@Nested
	@DisplayName("Moves")
	class Moves {

		@Test
		@DisplayName("Moves of a game are stored and counted")
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
		@DisplayName("A null move is rejected by the assertion")
		void nullMoveRejected() {
			long gameId = database.startGame();
			assertThrows(AssertionError.class, () -> database.saveMove(gameId, null));
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
			assertNotNull(move.time());
			assertEquals(3, move.details().size());
			assertEquals("A1", move.details().get(0).label());
			assertEquals(MoveDatabase.SHOT_REPEATED, move.details().get(1).outcome());
			assertEquals(MoveDatabase.SHOT_OUTSIDE, move.details().get(2).outcome());
		}

		@Test
		@DisplayName("Sunk ships, hits and water shots are classified and summarised")
		void classifiesEveryOutcome() {
			Game game = knownGame();
			long gameId = database.startGame();
			// A1 sinks the Barge, F6 hits the Caravel, J10 is water
			game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));
			database.saveMove(gameId, game.getAlienMoves().get(0));

			MoveDatabase.MoveRecord move = database.listMoves(gameId).get(0);

			assertEquals("A1 F6 J10", move.shots());
			assertEquals(3, move.valid());
			assertEquals(2, move.hits());
			assertEquals(1, move.missed());
			assertEquals(0, move.repeated());
			assertEquals(0, move.outside());
			assertEquals("Barca", move.sunk());

			List<MoveDatabase.ShotRecord> shots = move.details();
			assertEquals(MoveDatabase.SHOT_SUNK, shots.get(0).outcome());
			assertEquals("Barca", shots.get(0).ship());
			assertEquals(MoveDatabase.SHOT_HIT, shots.get(1).outcome());
			assertEquals("Caravela", shots.get(1).ship());
			assertEquals(MoveDatabase.SHOT_WATER, shots.get(2).outcome());
			assertNull(shots.get(2).ship());
		}

		@Test
		@DisplayName("Moves are listed ordered by number and only for the requested game")
		void listsMovesOfOneGameInOrder() {
			Game game = knownGame();
			long gameId = database.startGame();
			long otherGame = database.startGame();
			game.fireShots(List.of(new Position(9, 0), new Position(9, 1), new Position(9, 2)));
			game.fireShots(List.of(new Position(8, 0), new Position(8, 1), new Position(8, 2)));
			database.saveMove(gameId, game.getAlienMoves().get(1));
			database.saveMove(gameId, game.getAlienMoves().get(0));

			List<MoveDatabase.MoveRecord> moves = database.listMoves(gameId);

			assertEquals(2, moves.size());
			assertEquals(1, moves.get(0).number());
			assertEquals(2, moves.get(1).number());
			assertTrue(database.listMoves(otherGame).isEmpty());
		}

		@Test
		@DisplayName("A move without shots is stored without shot details")
		void savesMoveWithoutShots() {
			long gameId = database.startGame();

			database.saveMove(gameId, new Move(1, List.of(), List.of()));

			MoveDatabase.MoveRecord move = database.listMoves(gameId).get(0);
			assertEquals("", move.shots());
			assertEquals("", move.sunk());
			assertTrue(move.details().isEmpty());
		}

		@Test
		@DisplayName("A move of a game that does not exist is rolled back")
		void rollsBackMoveOfUnknownGame() {
			Game game = knownGame();
			game.fireShots(List.of(new Position(9, 0), new Position(9, 1), new Position(9, 2)));

			assertDoesNotThrow(() -> database.saveMove(9999, game.getAlienMoves().get(0)));

			assertEquals(0, database.countMoves(9999));
			assertTrue(database.listMoves(9999).isEmpty());
		}
	}

	@Nested
	@DisplayName("ShotRecord")
	class ShotRecordTests {

		@Test
		@DisplayName("label() uses the classic notation (row letter + column number)")
		void label() {
			assertEquals("A1", new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_WATER, null).label());
			assertEquals("J10", new MoveDatabase.ShotRecord(9, 9, MoveDatabase.SHOT_WATER, null).label());
			assertEquals("C5", new MoveDatabase.ShotRecord(2, 4, MoveDatabase.SHOT_WATER, null).label());
		}

		@Test
		@DisplayName("isHit() is true only for hits and sinks")
		void isHit() {
			assertTrue(new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_HIT, "Nau").isHit());
			assertTrue(new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_SUNK, "Barca").isHit());
			assertFalse(new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_WATER, null).isHit());
			assertFalse(new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_REPEATED, null).isHit());
			assertFalse(new MoveDatabase.ShotRecord(0, 0, MoveDatabase.SHOT_OUTSIDE, null).isHit());
		}
	}

	@Nested
	@DisplayName("Unavailable database")
	class UnavailableDatabase {

		private MoveDatabase broken;

		@BeforeEach
		void setUp() throws IOException {
			// A regular file where a directory is expected: the parent directory cannot be created
			Path blocker = Files.createFile(tempDir.resolve("blocker"));
			broken = new MoveDatabase(blocker.resolve("x/jogadas.db").toString());
		}

		@Test
		@DisplayName("Starting a game returns -1")
		void startGameFails() {
			assertEquals(-1, broken.startGame());
		}

		@Test
		@DisplayName("Counting moves returns -1")
		void countMovesFails() {
			assertEquals(-1, broken.countMoves(1));
		}

		@Test
		@DisplayName("Listing games and moves returns empty lists")
		void listsAreEmpty() {
			assertTrue(broken.listGames().isEmpty());
			assertTrue(broken.listMoves(1).isEmpty());
		}

		@Test
		@DisplayName("Saving moves and finishing games never throws")
		void writesDoNotThrow() {
			Game game = knownGame();
			game.fireShots(List.of(new Position(9, 0), new Position(9, 1), new Position(9, 2)));

			assertDoesNotThrow(() -> broken.saveMove(1, game.getAlienMoves().get(0)));
			assertDoesNotThrow(() -> broken.finishGame(1, 1, MoveDatabase.RESULT_QUIT));
		}
	}
}
