
package com.nikao.rag.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

// Add this method to the FileProcessingService class
import java.util.ArrayList;
import java.util.List;

@Service
public class FileProcessingService {

    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        InputStream inputStream = file.getInputStream();

        return switch (contentType) {
            case "application/pdf" -> extractFromPdf(inputStream);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                extractFromDocx(inputStream);
            default -> throw new IllegalArgumentException("Formato de arquivo não suportado: " + contentType);
        };
    }

    public String extractText(InputStream stream, String fileName) throws IOException {
        if (fileName.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(stream)) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (fileName.endsWith(".docx")) {
            XWPFDocument doc = new XWPFDocument(stream);
            return doc.getParagraphs().stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
        // } else if (fileName.endsWith(".xml")) {
        //     try {
        //         return extractFromXml(stream);
        //     } catch (Exception e) {
        //         throw new IOException("Erro ao processar XML", e);
        //     }
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return extractFromExcel(stream);
        } else {
            throw new IllegalArgumentException("Formato de arquivo não suportado: " + fileName);
        }
    }


public List<String> splitIntoChunks(String text, int chunkSize) {
    List<String> chunks = new ArrayList<>();
    for (int start = 0; start < text.length(); start += chunkSize) {
        int end = Math.min(start + chunkSize, text.length());
        chunks.add(text.substring(start, end));
    }
    return chunks;
}

    private String extractFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    public String extractTextFromUrl(String url) throws IOException {
        return Jsoup.connect(url).get().body().text();
    }

    public String extractFromExcel(InputStream stream) throws IOException {
    StringBuilder sb = new StringBuilder();
    Workbook workbook = new XSSFWorkbook(stream);

    for (Sheet sheet : workbook) {
        sb.append("Planilha: ").append(sheet.getSheetName()).append("\n");
        for (Row row : sheet) {
            for (Cell cell : row) {
                switch (cell.getCellType()) {
                    case STRING -> sb.append(cell.getStringCellValue()).append(" | ");
                    case NUMERIC -> sb.append(cell.getNumericCellValue()).append(" | ");
                    case BOOLEAN -> sb.append(cell.getBooleanCellValue()).append(" | ");
                    default -> sb.append(" | ");
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    workbook.close();
    return sb.toString();
}


    public String crawlWebsite(String baseUrl, int maxPages) throws IOException {
        Set<String> visited = new HashSet<>();
        Queue<String> toVisit = new LinkedList<>();
        StringBuilder allContent = new StringBuilder();

        toVisit.add(baseUrl);

        while (!toVisit.isEmpty() && visited.size() < maxPages) {
            String url = toVisit.poll();
            if (!visited.contains(url)) {
                try {
                    Document doc = Jsoup.connect(url).get();
                    allContent.append(doc.body().text()).append("\n\n");

                    // Encontra links internos
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String absHref = link.absUrl("href");
                        if (absHref.startsWith(baseUrl) && !visited.contains(absHref)) {
                            toVisit.add(absHref);
                        }
                    }

                    visited.add(url);
                } catch (Exception e) {
                    System.out.println("Erro ao acessar " + url + ": " + e.getMessage());
                }
            }
        }

        return allContent.toString();
    }

}
