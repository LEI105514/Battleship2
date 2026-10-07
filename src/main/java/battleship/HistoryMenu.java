package battleship;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive console menu to browse the games stored in the {@link MoveDatabase}
 * (issue #8). The user first chooses a game and then replays its moves one at a time,
 * seeing the board with the shots fired up to the current move.
 */
public class HistoryMenu {

	private static final String BACK = "v";
	private static final String NEXT = "s";
	private static final String PREVIOUS = "a";
	private static final String FIRST = "p";
	private static final String LAST = "u";
	private static final String ALL = "t";

	static final char EMPTY_MARKER = '.';
	static final char OLD_HIT_MARKER = '*';
	static final char OLD_WATER_MARKER = 'o';
	static final char NEW_HIT_MARKER = 'X';
	static final char NEW_WATER_MARKER = 'O';

	private final MoveDatabase database;
	private final Scanner in;

	/**
	 * Creates the menu.
	 *
	 * @param database the database with the stored games
	 * @param in       the scanner to read the user commands from
	 */
	public HistoryMenu(MoveDatabase database, Scanner in) {
		assert database != null;
		assert in != null;

		this.database = database;
		this.in = in;
	}

	/**
	 * Shows the list of games until the user goes back to the main menu.
	 */
	public void show() {
		while (true) {
			List<MoveDatabase.GameRecord> games = database.listGames();
			printGames(games);
			if (games.isEmpty())
				return;

			System.out.print("Numero do jogo a ver (" + BACK + " = voltar ao menu): ");
			if (!in.hasNext())
				return;
			String choice = in.next().trim().toLowerCase();
			if (choice.equals(BACK))
				return;

			MoveDatabase.GameRecord game = findGame(games, choice);
			if (game == null)
				System.out.println("Jogo inexistente! Escolhe um dos numeros da lista.");
			else
				showGame(game);
		}
	}

	private static MoveDatabase.GameRecord findGame(List<MoveDatabase.GameRecord> games, String choice) {
		try {
			long id = Long.parseLong(choice);
			for (MoveDatabase.GameRecord game : games)
				if (game.id() == id)
					return game;
		} catch (NumberFormatException e) {
			// not a number: handled as an unknown game
		}
		return null;
	}

	private static void printGames(List<MoveDatabase.GameRecord> games) {
		System.out.println();
		System.out.println("======================= HISTORICO DE JOGOS =======================");
		if (games.isEmpty()) {
			System.out.println("Ainda nao existem jogos guardados.");
		} else {
			System.out.printf(" %4s | %-19s | %7s | %s%n", "Jogo", "Inicio", "Jogadas", "Resultado");
			System.out.println("------+---------------------+---------+---------------------------");
			for (MoveDatabase.GameRecord game : games)
				System.out.printf(" %4d | %-19s | %7d | %s%n", game.id(), game.start(), game.moves(),
						game.result() == null ? "em curso" : game.result());
		}
		System.out.println("==================================================================");
	}

	/**
	 * Replays the moves of a game, letting the user move forward and backward.
	 */
	private void showGame(MoveDatabase.GameRecord game) {
		List<MoveDatabase.MoveRecord> moves = database.listMoves(game.id());
		if (moves.isEmpty()) {
			System.out.println("O jogo " + game.id() + " nao tem jogadas guardadas.");
			return;
		}

		int current = 0;
		printMove(game, moves, current);
		while (true) {
			System.out.print("[" + NEXT + "]eguinte  [" + PREVIOUS + "]nterior  [" + FIRST + "]rimeira  ["
					+ LAST + "]ltima  [" + ALL + "]odas  [" + BACK + "]oltar: ");
			if (!in.hasNext())
				return;
			String command = in.next().trim().toLowerCase();
			switch (command) {
				case NEXT -> {
					if (current < moves.size() - 1)
						current++;
					else {
						System.out.println("Ja estas na ultima jogada.");
						continue;
					}
				}
				case PREVIOUS -> {
					if (current > 0)
						current--;
					else {
						System.out.println("Ja estas na primeira jogada.");
						continue;
					}
				}
				case FIRST -> current = 0;
				case LAST -> current = moves.size() - 1;
				case ALL -> {
					printAllMoves(game, moves);
					continue;
				}
				case BACK -> {
					return;
				}
				default -> {
					System.out.println("Comando desconhecido!");
					continue;
				}
			}
			printMove(game, moves, current);
		}
	}

	private static String summary(MoveDatabase.MoveRecord move) {
		StringBuilder text = new StringBuilder();
		text.append(move.valid()).append(" validos, ")
				.append(move.missed()).append(" na agua, ")
				.append(move.hits()).append(" em navios, ")
				.append(move.repeated()).append(" repetidos, ")
				.append(move.outside()).append(" exteriores");
		if (!move.sunk().isEmpty())
			text.append(" | afundados: ").append(move.sunk());
		return text.toString();
	}

	private static void printAllMoves(MoveDatabase.GameRecord game, List<MoveDatabase.MoveRecord> moves) {
		System.out.println();
		System.out.println("Jogo " + game.id() + " - todas as jogadas:");
		for (MoveDatabase.MoveRecord move : moves)
			System.out.printf("   Jogada %d: %-12s -> %s%n", move.number(), move.shots(), summary(move));
		System.out.println();
	}

	private static void printMove(MoveDatabase.GameRecord game, List<MoveDatabase.MoveRecord> moves, int index) {
		MoveDatabase.MoveRecord move = moves.get(index);
		System.out.println();
		System.out.printf("=== Jogo %d | Jogada %d de %d | %s ===%n", game.id(), index + 1, moves.size(), move.time());
		System.out.println("Tiros: " + move.shots() + " -> " + summary(move));

		if (move.details().isEmpty()) {
			System.out.println("(jogada guardada sem detalhe dos tiros: tabuleiro indisponivel)");
		} else {
			printBoard(buildBoard(moves, index));
			System.out.println("'" + NEW_HIT_MARKER + "'/'" + NEW_WATER_MARKER + "'-> tiro desta jogada (navio/agua), '"
					+ OLD_HIT_MARKER + "'/'" + OLD_WATER_MARKER + "'-> jogadas anteriores");
		}
	}

	/**
	 * Builds the board with every shot fired up to (and including) the given move.
	 * Shots of that move are marked differently from the older ones.
	 *
	 * @param moves the moves of the game
	 * @param upTo  index of the current move
	 * @return the board, indexed by [row][column]
	 */
	static char[][] buildBoard(List<MoveDatabase.MoveRecord> moves, int upTo) {
		char[][] board = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
		for (char[] row : board)
			Arrays.fill(row, EMPTY_MARKER);

		for (int i = 0; i <= upTo; i++) {
			boolean current = i == upTo;
			for (MoveDatabase.ShotRecord shot : moves.get(i).details()) {
				if (shot.outcome().equals(MoveDatabase.SHOT_OUTSIDE) || shot.outcome().equals(MoveDatabase.SHOT_REPEATED))
					continue;
				if (shot.isHit())
					board[shot.row()][shot.column()] = current ? NEW_HIT_MARKER : OLD_HIT_MARKER;
				else
					board[shot.row()][shot.column()] = current ? NEW_WATER_MARKER : OLD_WATER_MARKER;
			}
		}
		return board;
	}

	private static void printBoard(char[][] board) {
		System.out.print("    ");
		for (int col = 0; col < Game.BOARD_SIZE; col++)
			System.out.print(" " + (col + 1));
		System.out.println();
		System.out.println("   +" + "--".repeat(Game.BOARD_SIZE) + "-+");
		for (int row = 0; row < Game.BOARD_SIZE; row++) {
			System.out.print(" " + (char) ('A' + row) + " |");
			for (int col = 0; col < Game.BOARD_SIZE; col++)
				System.out.print(" " + board[row][col]);
			System.out.println(" |");
		}
		System.out.println("   +" + "--".repeat(Game.BOARD_SIZE) + "-+");
	}
}
