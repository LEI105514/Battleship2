package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Arrays;

/**
 * Test class for Ship.
 * Author: ${user.name}
 * Date: ${current_date}
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - getCategory: 1
 * - getSize: 1
 * - getBearing: 1
 * - getPositions: 1
 * - stillFloating: 2
 * - shoot: 2
 * - occupies: 2
 * - tooCloseTo (IShip): 2
 * - tooCloseTo (IPosition): 2
 * - getTopMostPos: 2
 * - getBottomMostPos: 2
 * - getLeftMostPos: 2
 * - getRightMostPos: 2
 */
public class ShipTest {

    private Ship ship, shipBorder;

    @BeforeEach
    void setUp() {
        // Since Ship is abstract, instantiate it with a concrete subclass (e.g., Barge)
        ship = new Barge(Compass.NORTH, new Position(5, 5));
        shipBorder = new Barge(Compass.NORTH, new Position(0, 0));
    }

    @AfterEach
    void tearDown() {
        ship = null;
    }

    /**
     * Test for the constructor.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testConstructor() {
        assertNotNull(ship, "Error: Ship instance should not be null.");
        assertEquals("Barca", ship.getCategory(), "Error: Ship category is incorrect.");
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing is incorrect.");
        assertEquals(1, ship.getSize(), "Error: Ship size is incorrect.");
        assertFalse(ship.getPositions().isEmpty(), "Error: Ship positions should not be empty.");
    }

    /**
     * Test for the getCategory method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetCategory() {
        assertEquals("Barca", ship.getCategory(), "Error: Ship category should be 'Barca'.");
    }

    /**
     * Test for the getSize method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetSize() {
        assertEquals(1, ship.getSize(), "Error: Ship size should be 1.");
    }

    /**
     * Test for the getBearing method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetBearing() {
        assertEquals(Compass.NORTH, ship.getBearing(), "Error: Ship bearing should be NORTH.");
    }

    /**
     * Test for the getPositions method.
     * Cyclomatic Complexity: 1
     */
    @Test
    void testGetPositions() {
        List<IPosition> positions = ship.getPositions();
        assertNotNull(positions, "Error: Ship positions should not be null.");
        assertEquals(1, positions.size(), "Error: Ship should have exactly one position.");
        assertEquals(5, positions.get(0).getRow(), "Error: Position's row should be 5.");
        assertEquals(5, positions.get(0).getColumn(), "Error: Position's column should be 5.");
    }

    /**
     * Test for the stillFloating method (all positions intact).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testStillFloating1() {
        assertTrue(ship.stillFloating(), "Error: Ship should still be floating.");
    }

    /**
     * Test for the stillFloating method (all positions hit).
     */
    @Test
    void testStillFloating2() {
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "Error: Ship should no longer be floating after being hit.");
    }

    /**
     * Test for the shoot method (valid position).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testShoot1() {
        Position target = new Position(5, 5);
        ship.shoot(target);
        assertTrue(ship.getPositions().get(0).isHit(), "Error: Position should be marked as hit.");
    }

    /**
     * Test for the shoot method (invalid position).
     */
    @Test
    void testShoot2() {
        Position target = new Position(0, 0);
        ship.shoot(target); // No exception expected
        assertFalse(ship.getPositions().get(0).isHit(), "Error: Position should not be marked as hit for an invalid target.");
    }

    /**
     * Test for the occupies method (position occupied).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testOccupies1() {
        Position pos = new Position(5, 5);
        assertTrue(ship.occupies(pos), "Error: Ship should occupy position (5, 5).");
    }

    /**
     * Test for the occupies method (position not occupied).
     */
    @Test
    void testOccupies2() {
        Position pos = new Position(1, 1);
        assertFalse(ship.occupies(pos), "Error: Ship should not occupy position (1, 1).");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships too close).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testTooCloseToShip1() {
        Ship nearbyShip = new Barge(Compass.NORTH, new Position(5, 6));
        assertTrue(ship.tooCloseTo(nearbyShip), "Error: Ships should be too close.");
    }

    /**
     * Test for the tooCloseTo method with another IShip (ships not close).
     */
    @Test
    void testTooCloseToShip2() {
        Ship farShip = new Barge(Compass.NORTH, new Position(10, 10));
        assertFalse(ship.tooCloseTo(farShip), "Error: Ships should not be too close.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions adjacent).
     * Cyclomatic Complexity: 2
     */
    @Test
    void testTooCloseToPosition1() {
        Position pos = new Position(5, 6); // Adjacent position
        assertTrue(ship.tooCloseTo(pos), "Error: Ship should be too close to the given position.");
    }

    /**
     * Test for the tooCloseTo method with an IPosition (positions not adjacent).
     */
    @Test
    void testTooCloseToPosition2() {
        Position pos = new Position(7, 7); // Non-adjacent position
        assertFalse(ship.tooCloseTo(pos), "Error: Ship should not be too close to the given position.");
    }

    /**
     * Test for the getTopMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetTopMostPos() {
        assertEquals(5, ship.getTopMostPos(), "Error: The topmost position should be 5.");
    }

    /**
     * Test for the getBottomMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetBottomMostPos() {
        assertEquals(5, ship.getBottomMostPos(), "Error: The bottommost position should be 5.");
    }

    /**
     * Test for the getLeftMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetLeftMostPos() {
        assertEquals(5, ship.getLeftMostPos(), "Error: The leftmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetRightMostPos() {
        assertEquals(5, ship.getRightMostPos(), "Error: The rightmost position should be 5.");
    }

    /**
     * Test for the getRightMostPos method.
     * Cyclomatic Complexity: 2
     */
    @Test
    void testGetAdjacentPositions() {
        assertEquals(8, ship.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
        assertEquals(3, shipBorder.getAdjacentPositions().size(), "Error: The number of adjacent positions of a barge in the middle of the tray should be 8.");
    }
    /** Subclasse mínima para testar Ship com posições arbitrárias. */
    private static class TestShip extends Ship {
        TestShip(String category, Compass bearing, IPosition pos, IPosition... cells) {
            super(category, bearing, pos, cells.length);
            positions.addAll(Arrays.asList(cells));
        }
    }

    // Navio "em zigzag" só para exercitar os ramos dos extremos
    private TestShip zigzag() {
        return new TestShip("Teste", Compass.NORTH, new Position(3, 3),
                new Position(3, 3), new Position(2, 4), new Position(4, 2));
    }

    private TestShip single(int r, int c) {
        return new TestShip("Teste", Compass.NORTH, new Position(r, c), new Position(r, c));
    }

    @Test
    @DisplayName("buildShip devolve o tipo certo para cada categoria")
    void testBuildShip() {
        Position p = new Position(1, 1);
        assertInstanceOf(Barge.class, Ship.buildShip("barca", Compass.NORTH, p));
        assertInstanceOf(Caravel.class, Ship.buildShip("caravela", Compass.NORTH, p));
        assertInstanceOf(Carrack.class, Ship.buildShip("nau", Compass.NORTH, p));
        assertInstanceOf(Frigate.class, Ship.buildShip("fragata", Compass.NORTH, p));
        assertInstanceOf(Galleon.class, Ship.buildShip("galeao", Compass.NORTH, p));
    }

    @Test
    @DisplayName("buildShip devolve null para categoria desconhecida")
    void testBuildShipUnknown() {
        assertNull(Ship.buildShip("submarino", Compass.NORTH, new Position(1, 1)));
    }

    @Test
    @DisplayName("buildShip com argumentos null viola os asserts")
    void testBuildShipNulls() {
        Position p = new Position(1, 1);
        assertThrows(AssertionError.class, () -> Ship.buildShip(null, Compass.NORTH, p));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", null, p));
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", Compass.NORTH, null));
    }

    @Test
    @DisplayName("Construtor rejeita categoria, rumo ou posição null")
    void testConstructorNulls() {
        Position p = new Position(1, 1);
        assertThrows(NullPointerException.class, () -> new TestShip(null, Compass.NORTH, p, p));
        assertThrows(NullPointerException.class, () -> new TestShip("x", null, p, p));
        assertThrows(NullPointerException.class, () -> new TestShip("x", Compass.NORTH, null, p));
    }

    @Test
    @DisplayName("getPosition e toString")
    void testGetPositionAndToString() {
        assertEquals(new Position(5, 5), ship.getPosition());
        assertEquals("[Barca NORTH F6]", ship.toString());
    }

    @Test
    @DisplayName("Extremos com várias posições desordenadas")
    void testExtremesMultiPosition() {
        TestShip s = zigzag();
        assertEquals(2, s.getTopMostPos());
        assertEquals(4, s.getBottomMostPos());
        assertEquals(2, s.getLeftMostPos());
        assertEquals(4, s.getRightMostPos());
    }

    @Test
    @DisplayName("getAdjacentPositions sem duplicados nem posições do próprio navio")
    void testAdjacentMultiPosition() {
        TestShip s = new TestShip("Teste", Compass.EAST, new Position(5, 5),
                new Position(5, 5), new Position(5, 6));
        List<IPosition> adj = s.getAdjacentPositions();
        assertEquals(10, adj.size());
        assertFalse(adj.contains(new Position(5, 5)));
        assertFalse(adj.contains(new Position(5, 6)));
    }

    @Test
    @DisplayName("stillFloating com navio parcialmente atingido")
    void testStillFloatingPartial() {
        TestShip s = new TestShip("Teste", Compass.EAST, new Position(5, 5),
                new Position(5, 5), new Position(5, 6));
        s.shoot(new Position(5, 5));
        assertTrue(s.stillFloating());
        s.shoot(new Position(5, 6));
        assertFalse(s.stillFloating());
    }

    @Test
    @DisplayName("occupies com navio de várias posições")
    void testOccupiesMulti() {
        TestShip s = new TestShip("Teste", Compass.EAST, new Position(5, 5),
                new Position(5, 5), new Position(5, 6));
        assertTrue(s.occupies(new Position(5, 6)));
        assertFalse(s.occupies(new Position(5, 7)));
    }

    @Test
    @DisplayName("tooCloseTo(IShip) percorre todas as posições do outro navio")
    void testTooCloseToShipMulti() {
        TestShip other = new TestShip("Teste", Compass.EAST, new Position(0, 0),
                new Position(0, 0), new Position(5, 6));
        assertTrue(ship.tooCloseTo(other));
    }

    @Test
    @DisplayName("sink atinge todas as posições")
    void testSink() {
        TestShip s = new TestShip("Teste", Compass.EAST, new Position(5, 5),
                new Position(5, 5), new Position(5, 6));
        s.sink();
        assertTrue(s.getPositions().stream().allMatch(IPosition::isHit));
        assertFalse(s.stillFloating());
    }

    @Test
    @DisplayName("isInsideBoard para cada limite do tabuleiro")
    void testIsInsideBoard() {
        assertTrue(ship.isInsideBoard());
        assertTrue(single(0, 0).isInsideBoard());
        assertFalse(single(5, -1).isInsideBoard());   // esquerda
        assertFalse(single(5, 10).isInsideBoard());   // direita
        assertFalse(single(-1, 5).isInsideBoard());   // topo
        assertFalse(single(10, 5).isInsideBoard());   // fundo
    }

    @Test
    @DisplayName("Asserts de occupies, tooCloseTo e shoot")
    void testAssertions() {
        assertThrows(AssertionError.class, () -> ship.occupies(null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null));
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null));
        assertThrows(AssertionError.class, () -> ship.shoot(null));
        assertThrows(AssertionError.class, () -> ship.shoot(new Position(10, 10)));
    }

}