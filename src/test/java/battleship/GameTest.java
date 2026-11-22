package battleship;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game.
 * Author: britoeabreu
 * Date: 2024-03-19
 * Time: 15:30
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - fire: 7
 * - getShots: 1
 * - getRepeatedShots: 1
 * - getInvalidShots: 1
 * - getHits: 1
 * - getSunkShips: 1
 * - getRemainingShips: 1
 * - validShot: 3
 * - repeatedShot: 2
 * - printBoard: 1
 * - printValidShots: 1
 * - printFleet: 1
 */
public class GameTest {

	private Game game;

	private final PrintStream originalOut = System.out;
	private ByteArrayOutputStream consola;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
		consola = new ByteArrayOutputStream();
		System.setOut(new PrintStream(consola));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		game = null;
	}

	// ---------- Auxiliares ----------

	private static List<IPosition> rajada(IPosition a, IPosition b, IPosition c) {
		return List.of(a, b, c);
	}

	private static Scanner entrada(String linha) {
		return new Scanner(linha + "\n");
	}

	private static List<IPosition> todasAsPosicoes() {
		List<IPosition> todas = new ArrayList<>();
		for (int r = 0; r < Game.BOARD_SIZE; r++)
			for (int c = 0; c < Game.BOARD_SIZE; c++)
				todas.add(new Position(r, c));
		return todas;
	}

	// ---------- Testes originais ----------

	@Test
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}

	// ---------- Getters ----------

	@Test
	@DisplayName("getMyMoves e getAlienFleet começam vazios")
	void getMyMovesEAlienFleetVazios() {
		assertTrue(game.getMyMoves().isEmpty());
		assertNotNull(game.getAlienFleet());
		assertTrue(game.getAlienFleet().getShips().isEmpty());
	}

	// ---------- fireSingleShot ----------

	@Test
	@DisplayName("fireSingleShot: tiro na água é válido, não repetido e sem navio")
	void fireSingleShotAgua() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		IGame.ShotResult r = game.fireSingleShot(new Position(5, 5), false);
		assertAll(
				() -> assertTrue(r.valid()),
				() -> assertFalse(r.repeated()),
				() -> assertNull(r.ship()),
				() -> assertFalse(r.sunk()),
				() -> assertEquals(0, game.getHits())
		);
	}

	@Test
	@DisplayName("fireSingleShot: acertar numa barca afunda-a e conta acerto e afundamento")
	void fireSingleShotAfundaBarca() {
		IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
		game.getMyFleet().addShip(barca);
		IGame.ShotResult r = game.fireSingleShot(new Position(1, 1), false);
		assertAll(
				() -> assertSame(barca, r.ship()),
				() -> assertTrue(r.sunk()),
				() -> assertEquals(1, game.getHits()),
				() -> assertEquals(1, game.getSunkShips()),
				() -> assertEquals(0, game.getRemainingShips())
		);
	}

	@Test
	@DisplayName("fireSingleShot: acertar numa fragata sem a afundar conta só o acerto")
	void fireSingleShotAcertoSemAfundar() {
		IShip fragata = new Frigate(Compass.EAST, new Position(5, 5));
		game.getMyFleet().addShip(fragata);
		IGame.ShotResult r = game.fireSingleShot(new Position(5, 5), false);
		assertAll(
				() -> assertSame(fragata, r.ship()),
				() -> assertFalse(r.sunk()),
				() -> assertEquals(1, game.getHits()),
				() -> assertEquals(0, game.getSunkShips())
		);
	}

	@Test
	@DisplayName("fireSingleShot: posição já atingida numa jogada anterior conta como repetida")
	void fireSingleShotRepetidoDeJogadaAnterior() {
		game.fireShots(rajada(new Position(2, 3), new Position(2, 4), new Position(2, 5)));
		IGame.ShotResult r = game.fireSingleShot(new Position(2, 3), false);
		assertAll(
				() -> assertTrue(r.valid()),
				() -> assertTrue(r.repeated()),
				() -> assertEquals(1, game.getRepeatedShots())
		);
	}

	@Test
	@DisplayName("fireSingleShot: posição nula é rejeitada pela asserção")
	void fireSingleShotNulo() {
		assertThrows(AssertionError.class, () -> game.fireSingleShot(null, false));
	}

	// ---------- fireShots ----------

	@Test
	@DisplayName("fireShots: rajada com número de tiros diferente de 3 lança exceção")
	void fireShotsNumeroErrado() {
		List<IPosition> doisTiros = List.of(new Position(0, 0), new Position(0, 1));
		assertThrows(IllegalArgumentException.class, () -> game.fireShots(doisTiros));
		assertTrue(game.getAlienMoves().isEmpty());
	}

	@Test
	@DisplayName("fireShots: tiro repetido dentro da mesma rajada conta como repetido")
	void fireShotsRepetidoNaMesmaRajada() {
		game.fireShots(rajada(new Position(2, 3), new Position(2, 3), new Position(2, 4)));
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireShots: cada rajada cria uma jogada com número sequencial")
	void fireShotsNumeraJogadas() {
		game.fireShots(rajada(new Position(0, 0), new Position(0, 1), new Position(0, 2)));
		game.fireShots(rajada(new Position(1, 0), new Position(1, 1), new Position(1, 2)));
		assertAll(
				() -> assertEquals(2, game.getAlienMoves().size()),
				() -> assertEquals(1, game.getAlienMoves().get(0).getNumber()),
				() -> assertEquals(2, game.getAlienMoves().get(1).getNumber())
		);
	}

	@Test
	@DisplayName("fireShots: lista nula é rejeitada pela asserção")
	void fireShotsNulo() {
		assertThrows(AssertionError.class, () -> game.fireShots(null));
	}

	// ---------- repeatedShot ----------

	@Test
	@DisplayName("repeatedShot: posição nula é rejeitada pela asserção")
	void repeatedShotNulo() {
		assertThrows(AssertionError.class, () -> game.repeatedShot(null));
	}

	// ---------- readEnemyFire ----------

	@Test
	@DisplayName("readEnemyFire: lê três posições no formato compacto (A3 B5 C7)")
	void readEnemyFireFormatoCompacto() throws Exception {
		String json = game.readEnemyFire(entrada("A3 B5 C7"));
		JsonNode tiros = new ObjectMapper().readTree(json);
		assertAll(
				() -> assertEquals(1, game.getAlienMoves().size()),
				() -> assertEquals(3, tiros.size()),
				() -> assertEquals("A", tiros.get(0).get("row").asText()),
				() -> assertEquals(3, tiros.get(0).get("column").asInt())
		);
	}

	@Test
	@DisplayName("readEnemyFire: lê posições com a letra separada do número (A 3 B 5 C 7)")
	void readEnemyFireLetraSeparada() {
		game.readEnemyFire(entrada("A 3 b 5 C 7"));
		List<IPosition> tiros = game.getAlienMoves().get(0).getShots();
		assertAll(
				() -> assertEquals(new Position('A', 3), tiros.get(0)),
				() -> assertEquals(new Position('B', 5), tiros.get(1)),
				() -> assertEquals(new Position('C', 7), tiros.get(2))
		);
	}

	@Test
	@DisplayName("readEnemyFire: ignora posições a mais depois das três primeiras")
	void readEnemyFirePosicoesAMais() {
		game.readEnemyFire(entrada("A1 A2 A3 A4"));
		assertEquals(3, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("readEnemyFire: letra sem número a seguir lança exceção")
	void readEnemyFireLetraSemNumero() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> game.readEnemyFire(entrada("A B5 C7")));
		assertTrue(e.getMessage().contains("Posição incompleta"));
	}

	@Test
	@DisplayName("readEnemyFire: menos de três posições lança exceção")
	void readEnemyFirePosicoesAMenos() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(entrada("A3 B5")));
		assertTrue(game.getAlienMoves().isEmpty());
	}

	@Test
	@DisplayName("readEnemyFire: scanner nulo é rejeitado pela asserção")
	void readEnemyFireNulo() {
		assertThrows(AssertionError.class, () -> game.readEnemyFire(null));
	}

	// ---------- randomEnemyFire ----------

	@Test
	@DisplayName("randomEnemyFire: dispara três tiros distintos dentro do tabuleiro")
	void randomEnemyFireTresTirosDistintos() throws Exception {
		String json = game.randomEnemyFire();
		List<IPosition> tiros = game.getAlienMoves().get(0).getShots();
		assertAll(
				() -> assertEquals(3, new ObjectMapper().readTree(json).size()),
				() -> assertEquals(3, new HashSet<>(tiros).size()),
				() -> assertTrue(tiros.stream().allMatch(IPosition::isInside)),
				() -> assertTrue(consola.toString().contains("rajada "))
		);
	}

	@Test
	@DisplayName("randomEnemyFire: não dispara para posições já atingidas nem junto a navios afundados")
	void randomEnemyFireEvitaPosicoesInuteis() {
		IShip barca = new Barge(Compass.NORTH, new Position(0, 0));
		game.getMyFleet().addShip(barca);
		barca.sink();
		game.fireShots(rajada(new Position(9, 9), new Position(9, 8), new Position(9, 7)));

		game.randomEnemyFire();

		List<IPosition> tiros = game.getAlienMoves().get(1).getShots();
		for (IPosition tiro : tiros) {
			assertFalse(barca.getAdjacentPositions().contains(tiro));
			assertFalse(game.getAlienMoves().get(0).getShots().contains(tiro));
		}
	}

	@Test
	@DisplayName("randomEnemyFire: com menos de três posições livres repete a última posição")
	void randomEnemyFireMenosDeTresCandidatas() {
		List<IPosition> todas = todasAsPosicoes();
		for (int i = 0; i + 3 <= todas.size() - 1; i += 3)
			game.fireShots(rajada(todas.get(i), todas.get(i + 1), todas.get(i + 2)));

		game.randomEnemyFire();

		List<IPosition> ultima = game.getAlienMoves().get(game.getAlienMoves().size() - 1).getShots();
		IPosition livre = todas.get(todas.size() - 1);
		assertAll(
				() -> assertEquals(3, ultima.size()),
				() -> assertTrue(ultima.stream().allMatch(livre::equals))
		);
	}

	// ---------- jsonShots ----------

	@Test
	@DisplayName("jsonShots: converte as posições para linha (letra) e coluna (número)")
	void jsonShotsConverte() throws Exception {
		String json = Game.jsonShots(List.of(new Position(0, 0), new Position(1, 2)));
		JsonNode tiros = new ObjectMapper().readTree(json);
		assertAll(
				() -> assertEquals(2, tiros.size()),
				() -> assertEquals("A", tiros.get(0).get("row").asText()),
				() -> assertEquals(1, tiros.get(0).get("column").asInt()),
				() -> assertEquals("B", tiros.get(1).get("row").asText()),
				() -> assertEquals(3, tiros.get(1).get("column").asInt())
		);
	}

	@Test
	@DisplayName("jsonShots: lista nula é rejeitada pela asserção")
	void jsonShotsNulo() {
		assertThrows(AssertionError.class, () -> Game.jsonShots(null));
	}

	// ---------- printBoard e variantes ----------

	@Test
	@DisplayName("printBoard: mostra navios, tiros certeiros, tiros na água e a legenda")
	void printBoardComTirosELegenda() {
		IFleet frota = new Fleet();
		frota.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		IShip afundada = new Barge(Compass.NORTH, new Position(5, 5));
		frota.addShip(afundada);
		afundada.sink();
		List<IPosition> tiros = rajada(new Position(1, 1), new Position(4, 5), new Position(-1, 0));
		List<IPosition> maisTiros = rajada(new Position(0, 9), new Position(1, 1), new Position(9, 0));
		List<IMove> jogadas = List.of(new Move(1, tiros, List.of()), new Move(2, maisTiros, List.of()));

		Game.printBoard(frota, jogadas, true, true);

		String saida = consola.toString();
		assertAll(
				() -> assertTrue(saida.contains("LEGENDA")),
				() -> assertTrue(saida.contains("*")),
				() -> assertTrue(saida.contains("o")),
				() -> assertTrue(saida.contains("#")),
				() -> assertTrue(saida.contains("-"))
		);
	}

	@Test
	@DisplayName("printBoard: sem tiros nem legenda mostra só o tabuleiro")
	void printBoardSemTirosNemLegenda() {
		IFleet frota = new Fleet();
		frota.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		List<IMove> jogadas = List.of(new Move(1, rajada(new Position(1, 1), new Position(0, 0), new Position(0, 1)), List.of()));

		Game.printBoard(frota, jogadas, false, false);

		String saida = consola.toString();
		assertAll(
				() -> assertFalse(saida.contains("LEGENDA")),
				() -> assertFalse(saida.contains("*")),
				() -> assertTrue(saida.contains("#"))
		);
	}

	@Test
	@DisplayName("printBoard: frota ou lista de jogadas nulas são rejeitadas pela asserção")
	void printBoardNulos() {
		assertThrows(AssertionError.class, () -> Game.printBoard(null, List.of(), false, false));
		assertThrows(AssertionError.class, () -> Game.printBoard(new Fleet(), null, false, false));
	}

	@Test
	@DisplayName("printMyBoard e printAlienBoard escrevem o tabuleiro na consola")
	void printMyBoardEAlienBoard() {
		game.printMyBoard(true, false);
		String depoisDoMeu = consola.toString();
		game.printAlienBoard(false, true);
		String saida = consola.toString();
		assertAll(
				() -> assertTrue(depoisDoMeu.contains("+--")),
				() -> assertFalse(depoisDoMeu.contains("LEGENDA")),
				() -> assertTrue(saida.contains("LEGENDA"))
		);
	}

	@Test
	@DisplayName("over escreve a mensagem de fim de jogo")
	void overEscreveMensagem() {
		game.over();
		assertTrue(consola.toString().contains("Java Sparrow"));
	}
}
