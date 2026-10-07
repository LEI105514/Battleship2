package battleship;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the games and their moves (shot rounds) in a local SQLite database,
 * using the sqlite-jdbc driver (issue #8).
 * <p>
 * The database has three tables:
 * <ul>
 *     <li>{@code jogos} - one row per game, with start/end time, number of moves and result;</li>
 *     <li>{@code jogadas} - one row per move, with the shots fired and a summary of their outcome;</li>
 *     <li>{@code tiros} - one row per shot, with its position and outcome (used to replay the game).</li>
 * </ul>
 * Database errors are logged and never interrupt the game.
 */
public class MoveDatabase {

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Default location of the database file, relative to the project root.
	 */
	public static final String DEFAULT_DB_FILE = "data/jogadas.db";

	/**
	 * Result stored when the whole fleet has been sunk.
	 */
	public static final String RESULT_FLEET_SUNK = "Frota afundada";

	/**
	 * Result stored when the player quits before the end of the game.
	 */
	public static final String RESULT_QUIT = "Desistencia";

	/**
	 * Possible outcomes of a single shot.
	 */
	public static final String SHOT_WATER = "agua";
	public static final String SHOT_HIT = "navio";
	public static final String SHOT_SUNK = "afundou";
	public static final String SHOT_REPEATED = "repetido";
	public static final String SHOT_OUTSIDE = "exterior";

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * A stored game.
	 *
	 * @param id     game id
	 * @param start  start date/time
	 * @param end    end date/time, or {@code null} if the game was not finished
	 * @param moves  number of stored moves
	 * @param result how the game ended, or {@code null} if it was not finished
	 */
	public record GameRecord(long id, String start, String end, int moves, String result) {
	}

	/**
	 * A stored shot.
	 *
	 * @param row     row index (0-based)
	 * @param column  column index (0-based)
	 * @param outcome one of the {@code SHOT_*} constants
	 * @param ship    category of the ship that was hit, or {@code null}
	 */
	public record ShotRecord(int row, int column, String outcome, String ship) {

		/**
		 * @return the shot in the classic format, e.g. "A1"
		 */
		public String label() {
			return (char) ('A' + row) + String.valueOf(column + 1);
		}

		/**
		 * @return true if the shot hit a ship
		 */
		public boolean isHit() {
			return SHOT_HIT.equals(outcome) || SHOT_SUNK.equals(outcome);
		}
	}

	/**
	 * A stored move.
	 *
	 * @param number   move number inside the game
	 * @param shots    the shots in the classic format, e.g. "A1 E5 J10"
	 * @param valid    number of valid shots
	 * @param repeated number of repeated shots
	 * @param outside  number of shots outside the board
	 * @param missed   number of shots in the water
	 * @param hits     number of shots on ships
	 * @param sunk     categories of the ships sunk in this move, comma separated
	 * @param time     date/time of the move
	 * @param details  the individual shots (empty for moves stored before this table existed)
	 */
	public record MoveRecord(int number, String shots, int valid, int repeated, int outside, int missed,
	                         int hits, String sunk, String time, List<ShotRecord> details) {
	}

	private final String url;

	/**
	 * Creates a database stored in {@link #DEFAULT_DB_FILE}.
	 */
	public MoveDatabase() {
		this(DEFAULT_DB_FILE);
	}

	/**
	 * Creates a database stored in the given file. The file and its parent
	 * directory are created if they do not exist yet.
	 *
	 * @param dbFile path of the SQLite database file
	 */
	public MoveDatabase(String dbFile) {
		assert dbFile != null;

		File parent = new File(dbFile).getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs())
			LOGGER.warn("Nao foi possivel criar a diretoria {}", parent);

		this.url = "jdbc:sqlite:" + dbFile;
		createTables();
	}

	private Connection connect() throws SQLException {
		Connection connection = DriverManager.getConnection(url);
		try (Statement st = connection.createStatement()) {
			st.execute("PRAGMA foreign_keys = ON");
		}
		return connection;
	}

	private void createTables() {
		try (Connection connection = connect(); Statement st = connection.createStatement()) {
			st.execute("""
					CREATE TABLE IF NOT EXISTS jogos (
					    id           INTEGER PRIMARY KEY AUTOINCREMENT,
					    inicio       TEXT NOT NULL,
					    fim          TEXT,
					    num_jogadas  INTEGER NOT NULL DEFAULT 0,
					    resultado    TEXT
					)""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS jogadas (
					    id               INTEGER PRIMARY KEY AUTOINCREMENT,
					    jogo_id          INTEGER NOT NULL REFERENCES jogos(id),
					    numero           INTEGER NOT NULL,
					    tiros            TEXT NOT NULL,
					    tiros_validos    INTEGER NOT NULL,
					    tiros_repetidos  INTEGER NOT NULL,
					    tiros_exteriores INTEGER NOT NULL,
					    tiros_agua       INTEGER NOT NULL,
					    tiros_em_navios  INTEGER NOT NULL,
					    navios_afundados TEXT NOT NULL,
					    data_hora        TEXT NOT NULL
					)""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS tiros (
					    id         INTEGER PRIMARY KEY AUTOINCREMENT,
					    jogada_id  INTEGER NOT NULL REFERENCES jogadas(id),
					    linha      INTEGER NOT NULL,
					    coluna     INTEGER NOT NULL,
					    resultado  TEXT NOT NULL,
					    navio      TEXT
					)""");
		} catch (SQLException e) {
			LOGGER.error("Erro ao criar as tabelas da base de dados: {}", e.getMessage());
		}
	}

	private static String now() {
		return LocalDateTime.now().format(TIMESTAMP_FORMAT);
	}

	/**
	 * Registers a new game.
	 *
	 * @return the id of the new game, or -1 if it could not be stored
	 */
	public long startGame() {
		String sql = "INSERT INTO jogos (inicio) VALUES (?)";
		try (Connection connection = connect();
		     PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, now());
			ps.executeUpdate();
			try (ResultSet keys = ps.getGeneratedKeys()) {
				if (keys.next())
					return keys.getLong(1);
			}
		} catch (SQLException e) {
			LOGGER.error("Erro ao registar o jogo na base de dados: {}", e.getMessage());
		}
		return -1;
	}

	private static String outcomeOf(IGame.ShotResult result) {
		if (!result.valid())
			return SHOT_OUTSIDE;
		if (result.repeated())
			return SHOT_REPEATED;
		if (result.ship() == null)
			return SHOT_WATER;
		return result.sunk() ? SHOT_SUNK : SHOT_HIT;
	}

	/**
	 * Stores a move (shot round) of the given game, including each of its shots.
	 *
	 * @param gameId id returned by {@link #startGame()}
	 * @param move   the move to store
	 */
	public void saveMove(long gameId, IMove move) {
		assert move != null;

		if (gameId < 0)
			return;

		List<IPosition> shots = move.getShots();
		List<IGame.ShotResult> results = move.getShotResults();

		int valid = 0;
		int repeated = 0;
		int outside = 0;
		int missed = 0;
		int hits = 0;
		List<String> sunk = new ArrayList<>();
		StringBuilder shotsText = new StringBuilder();
		for (int i = 0; i < shots.size(); i++) {
			IPosition shot = shots.get(i);
			if (!shotsText.isEmpty())
				shotsText.append(' ');
			shotsText.append(shot.getClassicRow()).append(shot.getClassicColumn());

			switch (outcomeOf(results.get(i))) {
				case SHOT_OUTSIDE -> outside++;
				case SHOT_REPEATED -> repeated++;
				case SHOT_WATER -> { valid++; missed++; }
				case SHOT_HIT -> { valid++; hits++; }
				default -> { valid++; hits++; sunk.add(results.get(i).ship().getCategory()); }
			}
		}

		String moveSql = """
				INSERT INTO jogadas (jogo_id, numero, tiros, tiros_validos, tiros_repetidos, tiros_exteriores,
				                     tiros_agua, tiros_em_navios, navios_afundados, data_hora)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
		String shotSql = "INSERT INTO tiros (jogada_id, linha, coluna, resultado, navio) VALUES (?, ?, ?, ?, ?)";

		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			try (PreparedStatement psMove = connection.prepareStatement(moveSql, Statement.RETURN_GENERATED_KEYS);
			     PreparedStatement psShot = connection.prepareStatement(shotSql)) {
				psMove.setLong(1, gameId);
				psMove.setInt(2, move.getNumber());
				psMove.setString(3, shotsText.toString());
				psMove.setInt(4, valid);
				psMove.setInt(5, repeated);
				psMove.setInt(6, outside);
				psMove.setInt(7, missed);
				psMove.setInt(8, hits);
				psMove.setString(9, String.join(", ", sunk));
				psMove.setString(10, now());
				psMove.executeUpdate();

				long moveId;
				try (ResultSet keys = psMove.getGeneratedKeys()) {
					keys.next();
					moveId = keys.getLong(1);
				}

				for (int i = 0; i < shots.size(); i++) {
					IGame.ShotResult result = results.get(i);
					psShot.setLong(1, moveId);
					psShot.setInt(2, shots.get(i).getRow());
					psShot.setInt(3, shots.get(i).getColumn());
					psShot.setString(4, outcomeOf(result));
					psShot.setString(5, result.ship() == null ? null : result.ship().getCategory());
					psShot.addBatch();
				}
				psShot.executeBatch();
				connection.commit();
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			}
		} catch (SQLException e) {
			LOGGER.error("Erro ao guardar a jogada na base de dados: {}", e.getMessage());
		}
	}

	/**
	 * Marks a game as finished.
	 *
	 * @param gameId     id returned by {@link #startGame()}
	 * @param totalMoves number of moves played
	 * @param result     description of how the game ended
	 */
	public void finishGame(long gameId, int totalMoves, String result) {
		if (gameId < 0)
			return;

		String sql = "UPDATE jogos SET fim = ?, num_jogadas = ?, resultado = ? WHERE id = ?";
		try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, now());
			ps.setInt(2, totalMoves);
			ps.setString(3, result);
			ps.setLong(4, gameId);
			ps.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Erro ao terminar o jogo na base de dados: {}", e.getMessage());
		}
	}

	/**
	 * Counts the moves stored for the given game.
	 *
	 * @param gameId id returned by {@link #startGame()}
	 * @return number of stored moves, or -1 on error
	 */
	public int countMoves(long gameId) {
		String sql = "SELECT COUNT(*) FROM jogadas WHERE jogo_id = ?";
		try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setLong(1, gameId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		} catch (SQLException e) {
			LOGGER.error("Erro ao consultar a base de dados: {}", e.getMessage());
			return -1;
		}
	}

	/**
	 * Lists every stored game, ordered by id.
	 *
	 * @return the stored games (empty on error)
	 */
	public List<GameRecord> listGames() {
		String sql = """
				SELECT g.id, g.inicio, g.fim, g.resultado,
				       (SELECT COUNT(*) FROM jogadas j WHERE j.jogo_id = g.id) AS n
				FROM jogos g ORDER BY g.id""";
		List<GameRecord> games = new ArrayList<>();
		try (Connection connection = connect();
		     Statement st = connection.createStatement();
		     ResultSet rs = st.executeQuery(sql)) {
			while (rs.next())
				games.add(new GameRecord(rs.getLong("id"), rs.getString("inicio"), rs.getString("fim"),
						rs.getInt("n"), rs.getString("resultado")));
		} catch (SQLException e) {
			LOGGER.error("Erro ao ler os jogos da base de dados: {}", e.getMessage());
		}
		return games;
	}

	/**
	 * Lists the moves of a game, ordered by move number, each with its individual shots.
	 *
	 * @param gameId id of the game
	 * @return the stored moves (empty on error)
	 */
	public List<MoveRecord> listMoves(long gameId) {
		String movesSql = """
				SELECT id, numero, tiros, tiros_validos, tiros_repetidos, tiros_exteriores, tiros_agua,
				       tiros_em_navios, navios_afundados, data_hora
				FROM jogadas WHERE jogo_id = ? ORDER BY numero""";
		String shotsSql = "SELECT linha, coluna, resultado, navio FROM tiros WHERE jogada_id = ? ORDER BY id";

		List<MoveRecord> moves = new ArrayList<>();
		try (Connection connection = connect();
		     PreparedStatement psMoves = connection.prepareStatement(movesSql);
		     PreparedStatement psShots = connection.prepareStatement(shotsSql)) {
			psMoves.setLong(1, gameId);
			try (ResultSet rs = psMoves.executeQuery()) {
				while (rs.next()) {
					List<ShotRecord> details = new ArrayList<>();
					psShots.setLong(1, rs.getLong("id"));
					try (ResultSet shots = psShots.executeQuery()) {
						while (shots.next())
							details.add(new ShotRecord(shots.getInt("linha"), shots.getInt("coluna"),
									shots.getString("resultado"), shots.getString("navio")));
					}
					moves.add(new MoveRecord(rs.getInt("numero"), rs.getString("tiros"),
							rs.getInt("tiros_validos"), rs.getInt("tiros_repetidos"),
							rs.getInt("tiros_exteriores"), rs.getInt("tiros_agua"),
							rs.getInt("tiros_em_navios"), rs.getString("navios_afundados"),
							rs.getString("data_hora"), details));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Erro ao ler as jogadas da base de dados: {}", e.getMessage());
		}
		return moves;
	}
}
