package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    private List<IPosition> shots;
    private List<IGame.ShotResult> results;
    private Move move;

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream consola;

    @BeforeEach
    void setUp() {
        shots = new ArrayList<>();
        shots.add(new Position(1, 1));
        shots.add(new Position(2, 2));
        results = new ArrayList<>();
        results.add(new IGame.ShotResult(true, false, null, false));
        move = new Move(5, shots, results);

        consola = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consola));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ---------- Auxiliares ----------

    private static IGame.ShotResult invalido() {
        return new IGame.ShotResult(false, false, null, false);
    }

    private static IGame.ShotResult repetido() {
        return new IGame.ShotResult(true, true, null, false);
    }

    private static IGame.ShotResult agua() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private static IGame.ShotResult acerto(IShip navio) {
        return new IGame.ShotResult(true, false, navio, false);
    }

    private static IGame.ShotResult afundado(IShip navio) {
        return new IGame.ShotResult(true, false, navio, true);
    }

    private static Move jogada(IGame.ShotResult... resultados) {
        return new Move(5, new ArrayList<>(), List.of(resultados));
    }

    private static JsonNode json(String texto) throws Exception {
        return new ObjectMapper().readTree(texto);
    }

    private String linhaJogada() {
        return consola.toString().lines()
                .filter(l -> l.startsWith("Jogada nº5 -> "))
                .findFirst()
                .orElse("")
                .substring("Jogada nº5 -> ".length());
    }

    @Test
    @DisplayName("getNumber devolve o número da jogada")
    void getNumber() {
        assertEquals(5, move.getNumber());
    }

    @Test
    @DisplayName("getShots devolve a lista de tiros passada no construtor")
    void getShots() {
        assertSame(shots, move.getShots());
    }

    @Test
    @DisplayName("getShotResults devolve a lista de resultados passada no construtor")
    void getShotResults() {
        assertSame(results, move.getShotResults());
    }

    @Test
    @DisplayName("toString mostra o número da jogada e quantos tiros e resultados tem")
    void testToString() {
        assertEquals("Move{number=5, shots=2, results=1}", move.toString());
    }

    @Test
    @DisplayName("processEnemyFire: três tiros na água contam como válidos e falhados")
    void processEnemyFireTresTirosNaAgua() throws Exception {
        JsonNode r = json(jogada(agua(), agua(), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(3, r.get("validShots").asInt()),
                () -> assertEquals(3, r.get("missedShots").asInt()),
                () -> assertEquals(0, r.get("repeatedShots").asInt()),
                () -> assertEquals(0, r.get("outsideShots").asInt()),
                () -> assertEquals(0, r.get("sunkBoats").size()),
                () -> assertEquals(0, r.get("hitsOnBoats").size())
        );
    }

    @Test
    @DisplayName("processEnemyFire: tiros inválidos são ignorados e contam como exteriores")
    void processEnemyFireTiroInvalidoIgnorado() throws Exception {
        JsonNode r = json(jogada(invalido(), agua(), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(2, r.get("validShots").asInt()),
                () -> assertEquals(1, r.get("outsideShots").asInt())
        );
    }

    @Test
    @DisplayName("processEnemyFire: tiros repetidos não contam como válidos")
    void processEnemyFireTirosRepetidos() throws Exception {
        JsonNode r = json(jogada(repetido(), repetido(), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(2, r.get("repeatedShots").asInt()),
                () -> assertEquals(1, r.get("validShots").asInt()),
                () -> assertEquals(0, r.get("outsideShots").asInt())
        );
    }

    @Test
    @DisplayName("processEnemyFire: acertos num navio não afundado aparecem em hitsOnBoats")
    void processEnemyFireAcertosSemAfundar() throws Exception {
        IShip fragata = new Frigate(Compass.NORTH, new Position(0, 0));
        JsonNode r = json(jogada(acerto(fragata), acerto(fragata), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(1, r.get("missedShots").asInt()),
                () -> assertEquals(1, r.get("hitsOnBoats").size()),
                () -> assertEquals("Fragata", r.get("hitsOnBoats").get(0).get("type").asText()),
                () -> assertEquals(2, r.get("hitsOnBoats").get(0).get("hits").asInt()),
                () -> assertEquals(0, r.get("sunkBoats").size())
        );
    }

    @Test
    @DisplayName("processEnemyFire: navio afundado aparece em sunkBoats e não em hitsOnBoats")
    void processEnemyFireNavioAfundado() throws Exception {
        IShip caravela = new Caravel(Compass.NORTH, new Position(0, 0));
        JsonNode r = json(jogada(acerto(caravela), afundado(caravela), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(1, r.get("sunkBoats").size()),
                () -> assertEquals("Caravela", r.get("sunkBoats").get(0).get("type").asText()),
                () -> assertEquals(1, r.get("sunkBoats").get(0).get("count").asInt()),
                () -> assertEquals(0, r.get("hitsOnBoats").size())
        );
    }

    @Test
    @DisplayName("processEnemyFire: dois navios do mesmo tipo afundados somam na contagem")
    void processEnemyFireDoisNaviosMesmoTipoAfundados() throws Exception {
        IShip barca1 = new Barge(Compass.NORTH, new Position(0, 0));
        IShip barca2 = new Barge(Compass.NORTH, new Position(5, 5));
        JsonNode r = json(jogada(afundado(barca1), afundado(barca2), agua()).processEnemyFire(false));
        assertAll(
                () -> assertEquals(1, r.get("sunkBoats").size()),
                () -> assertEquals("Barca", r.get("sunkBoats").get(0).get("type").asText()),
                () -> assertEquals(2, r.get("sunkBoats").get(0).get("count").asInt())
        );
    }

    @Test
    @DisplayName("processEnemyFire: sem verbose não escreve o resumo da jogada")
    void processEnemyFireSemVerboseNaoEscreveResumo() {
        jogada(agua(), agua(), agua()).processEnemyFire(false);
        assertFalse(consola.toString().contains("Jogada nº5 -> "));
    }

    @Test
    @DisplayName("processEnemyFire (verbose): só tiros repetidos")
    void processEnemyFireVerboseSoRepetidos() {
        jogada(repetido(), repetido(), repetido()).processEnemyFire(true);
        assertEquals("3 tiros repetidos", linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): um repetido e dois exteriores (singular e plural)")
    void processEnemyFireVerboseUmRepetidoDoisExteriores() {
        jogada(repetido(), invalido(), invalido()).processEnemyFire(true);
        assertEquals("1 tiro repetido, 2 tiros exteriores", linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): todos os tiros exteriores")
    void processEnemyFireVerboseTodosExteriores() {
        jogada(invalido(), invalido(), invalido()).processEnemyFire(true);
        assertEquals("3 tiros exteriores", linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): um tiro na água e um exterior")
    void processEnemyFireVerboseAguaEExterior() {
        jogada(agua(), agua(), invalido()).processEnemyFire(true);
        assertEquals("2 tiros válidos: 2 tiros na água, 1 tiro exterior", linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): um tiro válido na água e dois exteriores")
    void processEnemyFireVerboseUmValido() {
        jogada(agua(), invalido(), invalido()).processEnemyFire(true);
        assertEquals("1 tiro válido: 1 tiro na água, 2 tiros exteriores", linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): acerto, água e repetido")
    void processEnemyFireVerboseAcertoAguaRepetido() {
        IShip fragata = new Frigate(Compass.NORTH, new Position(0, 0));
        jogada(acerto(fragata), agua(), repetido()).processEnemyFire(true);
        assertEquals("2 tiros válidos: 1 tiro num(a) Fragata + 1 tiro na água, 1 tiro repetido",
                linhaJogada());
    }

    @Test
    @DisplayName("processEnemyFire (verbose): dois acertos no mesmo navio e um repetido")
    void processEnemyFireVerboseDoisAcertosERepetido() {
        IShip fragata = new Frigate(Compass.NORTH, new Position(0, 0));
        jogada(acerto(fragata), acerto(fragata), repetido()).processEnemyFire(true);
        String linha = linhaJogada();
        assertAll(
                () -> assertTrue(linha.startsWith("2 tiros válidos: 2 tiros num(a) Fragata")),
                () -> assertTrue(linha.endsWith(", 1 tiro repetido")),
                () -> assertFalse(linha.contains("+"))
        );
    }

    @Test
    @DisplayName("processEnemyFire (verbose): navio afundado sem tiros na água")
    void processEnemyFireVerboseNavioAfundado() {
        IShip caravela = new Caravel(Compass.NORTH, new Position(0, 0));
        jogada(afundado(caravela), invalido(), invalido()).processEnemyFire(true);
        String linha = linhaJogada();
        assertAll(
                () -> assertTrue(linha.startsWith("1 tiro válido: 1 Caravela ao fundo")),
                () -> assertTrue(linha.endsWith(", 2 tiros exteriores")),
                () -> assertFalse(linha.contains("num(a)"))
        );
    }

    @Test
    @DisplayName("processEnemyFire (verbose): dois navios afundados do mesmo tipo e um acerto noutro")
    void processEnemyFireVerboseDoisAfundadosEAcerto() {
        IShip barca1 = new Barge(Compass.NORTH, new Position(0, 0));
        IShip barca2 = new Barge(Compass.NORTH, new Position(5, 5));
        IShip fragata = new Frigate(Compass.NORTH, new Position(8, 0));
        jogada(afundado(barca1), afundado(barca2), acerto(fragata)).processEnemyFire(true);
        String linha = linhaJogada();
        assertAll(
                () -> assertTrue(linha.startsWith("3 tiros válidos: ")),
                () -> assertTrue(linha.contains("2 Barcas ao fundo")),
                () -> assertTrue(linha.contains("1 tiro num(a) Fragata")),
                () -> assertFalse(linha.contains("exterior"))
        );
    }

    @Test
    @DisplayName("processEnemyFire (verbose): um tiro válido e vários repetidos (plural depois dos válidos)")
    void processEnemyFireVerboseValidoEVariosRepetidos() {
        jogada(agua(), repetido(), repetido()).processEnemyFire(true);
        assertEquals("1 tiro válido: 1 tiro na água, 2 tiros repetidos", linhaJogada());
    }
}
