package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Tasks}: reading ships and positions, building a fleet and the main menu.
 */
@DisplayName("Tasks")
class TasksTest {

	/**
	 * Eleven barges that do not touch each other (rows and columns 0, 2, 4, ...).
	 */
	private static final String ELEVEN_BARGES = """
			barca 0 0 n
			barca 0 2 n
			barca 0 4 n
			barca 0 6 n
			barca 0 8 n
			barca 2 0 n
			barca 2 2 n
			barca 2 4 n
			barca 2 6 n
			barca 2 8 n
			barca 4 0 n
			""";

	private PrintStream originalOut;
	private InputStream originalIn;
	private ByteArrayOutputStream output;

	@BeforeEach
	void setUp() {
		originalOut = System.out;
		originalIn = System.in;
		output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		System.setIn(originalIn);
	}

	private String printed() {
		return output.toString(StandardCharsets.UTF_8);
	}

	@Nested
	@DisplayName("readPosition")
	class ReadPosition {

		@Test
		@DisplayName("Reads a row and a column as numbers")
		void readsRowAndColumn() {
			Position pos = Tasks.readPosition(new Scanner("3 7"));

			assertEquals(3, pos.getRow());
			assertEquals(7, pos.getColumn());
		}

		@Test
		@DisplayName("A null scanner is rejected by the assertion")
		void nullScanner() {
			assertThrows(AssertionError.class, () -> Tasks.readPosition(null));
		}
	}

	@Nested
	@DisplayName("readShip")
	class ReadShip {

		@Test
		@DisplayName("Reads kind, position and bearing of a ship")
		void readsShip() {
			Ship ship = Tasks.readShip(new Scanner("caravela 1 2 e"));

			assertNotNull(ship);
			assertEquals("Caravela", ship.getCategory());
			assertEquals(Compass.EAST, ship.getBearing());
			assertEquals(1, ship.getPosition().getRow());
			assertEquals(2, ship.getPosition().getColumn());
		}

		@Test
		@DisplayName("An unknown kind of ship returns null")
		void unknownShip() {
			assertNull(Tasks.readShip(new Scanner("submarino 1 2 n")));
		}

		@Test
		@DisplayName("A null scanner is rejected by the assertion")
		void nullScanner() {
			assertThrows(AssertionError.class, () -> Tasks.readShip(null));
		}
	}

	@Nested
	@DisplayName("buildFleet")
	class BuildFleet {

		@Test
		@DisplayName("Builds a complete fleet from the user data")
		void buildsCompleteFleet() {
			Fleet fleet = Tasks.buildFleet(new Scanner(ELEVEN_BARGES));

			assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size());
		}

		@Test
		@DisplayName("Unknown and colliding ships are skipped until the fleet is complete")
		void skipsInvalidShips() {
			String input = "submarino 9 9 n\n" + "barca 0 0 n\n" + "barca 0 1 n\n" + ELEVEN_BARGES.substring(ELEVEN_BARGES.indexOf('\n') + 1);
			Scanner in = new Scanner(input);

			Fleet fleet = Tasks.buildFleet(in);

			assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size());
			assertFalse(in.hasNext());
		}

		@Test
		@DisplayName("A null scanner is rejected by the assertion")
		void nullScanner() {
			assertThrows(AssertionError.class, () -> Tasks.buildFleet(null));
		}
	}

	@Nested
	@DisplayName("readClassicPosition")
	class ReadClassicPosition {

		@Test
		@DisplayName("Reads the compact format 'A3'")
		void compactFormat() {
			IPosition pos = Tasks.readClassicPosition(new Scanner("A3"));

			assertEquals(0, pos.getRow());
			assertEquals(2, pos.getColumn());
		}

		@Test
		@DisplayName("Reads the separated, lower-case format 'c 10'")
		void separatedFormat() {
			IPosition pos = Tasks.readClassicPosition(new Scanner("c 10"));

			assertEquals(2, pos.getRow());
			assertEquals(9, pos.getColumn());
		}

		@Test
		@DisplayName("Reads only one position, leaving the rest of the input")
		void leavesRestOfInput() {
			Scanner in = new Scanner("B2 C3");

			IPosition pos = Tasks.readClassicPosition(in);

			assertEquals("B", pos.getClassicRow() + "");
			assertEquals("C3", in.next());
		}

		@Test
		@DisplayName("Empty input is rejected")
		void emptyInput() {
			Scanner in = new Scanner("");
			assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(in));
		}

		@Test
		@DisplayName("Input without a letter is rejected")
		void onlyNumbers() {
			Scanner in = new Scanner("33");
			assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(in));
		}

		@Test
		@DisplayName("More than one letter followed by a number is rejected")
		void twoLetters() {
			Scanner in = new Scanner("AB 3");
			assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(in));
		}

		@Test
		@DisplayName("A letter followed by a negative number is rejected")
		void negativeNumber() {
			Scanner in = new Scanner("A -3");
			assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(in));
		}
	}

	@Nested
	@DisplayName("Menu")
	class Menu {

		private String runMenu(String input) {
			System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
			Tasks.menu();
			return printed();
		}

		@Test
		@DisplayName("menuHelp() lists every command")
		void menuHelp() {
			Tasks.menuHelp();

			String text = printed();
			for (String command : new String[]{"gerafrota", "lefrota", "estado", "mapa", "rajada", "simula",
					"tiros", "historico", "scoreboard", "desisto"})
				assertTrue(text.contains("- " + command + ":"), command);
		}

		@Test
		@DisplayName("Quitting immediately says goodbye")
		void quitImmediately() {
			String text = runMenu("desisto\n");

			assertTrue(text.contains("AJUDA DO MENU"));
			assertTrue(text.contains("Bons ventos!"));
		}

		@Test
		@DisplayName("Game commands do nothing before a fleet exists and unknown commands are reported")
		void commandsWithoutGame() {
			String text = runMenu("estado mapa rajada simula tiros xpto desisto\n");

			assertTrue(text.contains("Que comando é esse??? Repete ..."));
			assertTrue(text.contains("Bons ventos!"));
		}

		@Test
		@DisplayName("A random game can be played, replaced, browsed and quit")
		void playRandomGame() {
			String text = runMenu("""
					ajuda
					gerafrota
					estado
					mapa
					rajada A1 B2 C3
					tiros
					gerafrota
					historico v
					scoreboard
					desisto
					""");

			assertEquals(2, text.split("AJUDA DO MENU", -1).length - 1);
			assertTrue(text.contains("Tempo gasto nesta jogada"));
			assertTrue(text.contains("HISTORICO DE JOGOS"));
			assertTrue(text.contains("Bons ventos!"));
		}

		@Test
		@DisplayName("A fleet can be read from the user, replacing the current game")
		void playUserFleet() {
			String text = runMenu("lefrota\n" + ELEVEN_BARGES + "lefrota\n" + ELEVEN_BARGES
					+ "rajada A1 B2 C3\nmapa\ndesisto\n");

			assertTrue(text.contains("Tempo gasto nesta jogada"));
			assertTrue(text.contains("Bons ventos!"));
		}
	}
}
