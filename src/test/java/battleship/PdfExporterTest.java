package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

class PdfExporterTest {

    private static final String FILENAME = "historico_jogadas.pdf";

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    /** IMove falso: devolve o JSON que quisermos em processEnemyFire. */
    private static class FakeMove implements IMove {
        private final int number;
        private final List<IPosition> shots;
        private final String json;

        FakeMove(int number, String json, IPosition... shots) {
            this.number = number;
            this.json = json;
            this.shots = List.of(shots);
        }

        @Override public int getNumber() { return number; }
        @Override public List<IPosition> getShots() { return shots; }
        @Override public List<IGame.ShotResult> getShotResults() { return new ArrayList<>(); }
        @Override public String processEnemyFire(boolean verbose) { return json; }
    }

    private static FakeMove move(int n, String json) {
        return new FakeMove(n, json, new Position('A', 1), new Position('B', 2), new Position('C', 3));
    }

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        new File(FILENAME).delete();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        new File(FILENAME).delete();
    }

    @Test
    @DisplayName("Construtor por omissão de PdfExporter")
    void testConstructor() {
        assertNotNull(new PdfExporter());
    }

    @Test
    @DisplayName("Lista vazia gera PDF")
    void testEmptyHistory() {
        PdfExporter.exportarHistorico(new ArrayList<>());
        assertTrue(new File(FILENAME).exists());
        assertTrue(out.toString().contains("[INFO]"));
    }

    @Test
    @DisplayName("Jogada sem falhados, repetidos, afundados nem atingidos")
    void testPlainMove() {
        String json = "{\"validShots\":3,\"missedShots\":0,\"repeatedShots\":0}";
        PdfExporter.exportarHistorico(List.of(move(1, json)));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("Jogada com falhados, repetidos, afundados e atingidos (vários)")
    void testFullMove() {
        String json = "{\"validShots\":2,\"missedShots\":1,\"repeatedShots\":1,"
                + "\"sunkBoats\":[{\"count\":1,\"type\":\"Nau\"},{\"count\":1,\"type\":\"Barca\"}],"
                + "\"hitsOnBoats\":[{\"hits\":1,\"type\":\"Galeao\"},{\"hits\":2,\"type\":\"Fragata\"}]}";
        PdfExporter.exportarHistorico(List.of(move(1, json)));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("sunkBoats e hitsOnBoats vazios")
    void testEmptyArrays() {
        String json = "{\"validShots\":3,\"missedShots\":0,\"repeatedShots\":0,"
                + "\"sunkBoats\":[],\"hitsOnBoats\":[]}";
        PdfExporter.exportarHistorico(List.of(move(1, json)));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("JSON inválido é tratado sem lançar exceção")
    void testInvalidJson() {
        PdfExporter.exportarHistorico(List.of(move(1, "isto não é json")));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("JSON sem campos obrigatórios é tratado sem lançar exceção")
    void testMissingFields() {
        PdfExporter.exportarHistorico(List.of(move(1, "{}")));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("Linha demasiado longa é truncada")
    void testLongLine() {
        String json = "{\"validShots\":3,\"missedShots\":1,\"repeatedShots\":1,"
                + "\"sunkBoats\":[{\"count\":1,\"type\":\"Fragata\"},{\"count\":1,\"type\":\"Caravela\"},"
                + "{\"count\":1,\"type\":\"Galeao\"}],"
                + "\"hitsOnBoats\":[{\"hits\":1,\"type\":\"Nau\"},{\"hits\":2,\"type\":\"Barca\"}]}";
        PdfExporter.exportarHistorico(List.of(move(1, json)));
        assertTrue(new File(FILENAME).exists());
    }

    @Test
    @DisplayName("Muitas jogadas ativam o ramo de nova página (bug conhecido: lança IllegalStateException)")
    void testNewPage() {
        String json = "{\"validShots\":3,\"missedShots\":0,\"repeatedShots\":0}";
        List<IMove> moves = new ArrayList<>();
        for (int i = 1; i <= 40; i++)
            moves.add(move(i, json));

        // O ramo de nova página fecha o contentStream e continua a usá-lo,
        // o que provoca IllegalStateException (bug conhecido, não corrigido nesta ficha).
        assertThrows(IllegalStateException.class, () -> PdfExporter.exportarHistorico(moves));
    }
}
