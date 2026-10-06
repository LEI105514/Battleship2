package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.List;

public class PdfExporter {

    public static void exportarHistorico(List<IMove> historyMoves) {
        String filename = "historico_jogadas.pdf";
        ObjectMapper mapper = new ObjectMapper(); // Usado para ler o JSON

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();

                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                contentStream.setFont(fontBold, 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Relatorio da Batalha Naval - Historico de Jogadas");

                contentStream.setFont(fontNormal, 12);
                contentStream.newLineAtOffset(0, -30);

                int yOffset = 720;

                for (IMove move : historyMoves) {
                    // Lógica de nova página se o espaço estiver a acabar
                    if (yOffset < 100) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        PDPageContentStream newStream = new PDPageContentStream(document, page);
                        newStream.beginText();
                        newStream.setFont(fontNormal, 12);
                        newStream.newLineAtOffset(50, 750);
                        yOffset = 750;
                    }

                    // 1. Extrair e formatar as coordenadas da jogada
                    StringBuilder coords = new StringBuilder("(");
                    List<IPosition> shots = move.getShots();
                    for (int i = 0; i < shots.size(); i++) {
                        coords.append(shots.get(i).getClassicRow()).append(shots.get(i).getClassicColumn());
                        if (i < shots.size() - 1) coords.append(", ");
                    }
                    coords.append(")");

                    // 2. Receber o JSON da jogada
                    String jsonString = move.processEnemyFire(false);

                    // 3. Juntar o número da rajada com as coordenadas
                    StringBuilder linhaTexto = new StringBuilder("Rajada ").append(move.getNumber())
                            .append(" ").append(coords).append(": ");

                    try {
                        // Converter String JSON para JsonNode para extrair os valores
                        JsonNode node = mapper.readTree(jsonString);
                        int valid = node.get("validShots").asInt();
                        int missed = node.get("missedShots").asInt();
                        int repeated = node.get("repeatedShots").asInt();

                        linhaTexto.append(valid).append(" validos");
                        if (missed > 0) linhaTexto.append(" (").append(missed).append(" na agua)");
                        if (repeated > 0) linhaTexto.append(", ").append(repeated).append(" repetidos");

                        // Analisar os barcos afundados
                        JsonNode sunk = node.get("sunkBoats");
                        if (sunk != null && !sunk.isEmpty()) {
                            linhaTexto.append(" | Afundou: ");
                            for (int i = 0; i < sunk.size(); i++) {
                                linhaTexto.append(sunk.get(i).get("count").asInt()).append(" ").append(sunk.get(i).get("type").asText());
                                if (i < sunk.size() - 1) linhaTexto.append(", ");
                            }
                        }

                        // Analisar os barcos atingidos (mas não afundados)
                        JsonNode hits = node.get("hitsOnBoats");
                        if (hits != null && !hits.isEmpty()) {
                            linhaTexto.append(" | Atingiu: ");
                            for (int i = 0; i < hits.size(); i++) {
                                linhaTexto.append(hits.get(i).get("hits").asInt()).append("x ").append(hits.get(i).get("type").asText());
                                if (i < hits.size() - 1) linhaTexto.append(", ");
                            }
                        }

                    } catch (Exception e) {
                        linhaTexto.append("Erro ao ler dados do formato JSON.");
                    }

                    String linhaFinal = linhaTexto.toString();

                    // Limitar tamanho máximo por precaução para não sair do ecrã do PDF
                    if (linhaFinal.length() > 95) {
                        linhaFinal = linhaFinal.substring(0, 92) + "...";
                    }

                    contentStream.showText(linhaFinal);
                    contentStream.newLineAtOffset(0, -20);
                    yOffset -= 20;
                }

                contentStream.endText();
            }

            document.save(filename);
            System.out.println("\n[INFO] Relatorio em PDF gerado com sucesso em: " + filename);

        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao gerar o PDF: " + e.getMessage());
        }
    }
}