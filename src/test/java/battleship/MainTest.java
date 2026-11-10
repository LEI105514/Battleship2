package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

class MainTest {

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("main mostra o título, o menu e termina com 'desisto'")
    void testMain() {
        System.setIn(new ByteArrayInputStream("desisto\n".getBytes()));

        Main.main(new String[]{});

        String output = out.toString();
        assertTrue(output.contains("***  Battleship  ***"));
        assertTrue(output.contains("AJUDA DO MENU"));
        assertTrue(output.contains("Bons ventos!"));
    }

    @Test
    @DisplayName("Construtor por omissão de Main")
    void testConstructor() {
        assertNotNull(new Main());
    }
}
