package com.canineai.backend.service.report;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.awt.Color;

import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class PDFExporterImpl implements PDFExporter {

    @Value("${canineai.ai.mode:real}")
    private String aiMode;

    @Override
    public byte[] exportPdf(String persistedReportContent, List<Path> previewImagePaths) {
        log.info("Starting OpenPDF generation from persisted clinical report data.");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            String safeContent = persistedReportContent != null ? persistedReportContent : "No report content available.";
            String plainText = safeContent.replaceAll("\\n+", "\n").trim();

            String[] lines = plainText.split("\n");
            
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            PdfPTable currentTable = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Handle Markdown Tables
                if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                    if (trimmed.contains("---")) {
                        // Skip the separator line
                        continue;
                    }
                    String[] cols = trimmed.substring(1, trimmed.length() - 1).split("\\|");
                    
                    if (currentTable == null) {
                        currentTable = new PdfPTable(cols.length);
                        currentTable.setWidthPercentage(100);
                        currentTable.setSpacingBefore(10f);
                        currentTable.setSpacingAfter(10f);
                        
                        // Header row
                        for (String col : cols) {
                            PdfPCell cell = new PdfPCell(new Paragraph(col.trim(), tableHeaderFont));
                            cell.setBackgroundColor(new Color(0, 102, 204));
                            cell.setPadding(5);
                            currentTable.addCell(cell);
                        }
                    } else {
                        // Data row
                        for (String col : cols) {
                            PdfPCell cell = new PdfPCell(new Paragraph(col.trim(), bodyFont));
                            cell.setPadding(5);
                            currentTable.addCell(cell);
                        }
                    }
                    continue;
                } else if (currentTable != null) {
                    // Table ended
                    document.add(currentTable);
                    currentTable = null;
                }

                if (trimmed.startsWith("# ")) {
                    document.add(new Paragraph(trimmed.substring(2).trim(), titleFont));
                    document.add(new Paragraph(" "));
                } else if (trimmed.startsWith("## ")) {
                    document.add(new Paragraph(trimmed.substring(3).trim(), headerFont));
                    document.add(new Paragraph(" "));
                } else if (trimmed.startsWith("### ")) {
                    document.add(new Paragraph(trimmed.substring(4).trim(), headerFont));
                    document.add(new Paragraph(" "));
                } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    document.add(new Paragraph(" • " + trimmed.substring(1).trim(), bodyFont));
                } else {
                    document.add(new Paragraph(trimmed, bodyFont));
                }
            }

            // If a table was the last thing
            if (currentTable != null) {
                document.add(currentTable);
            }

            if (previewImagePaths != null) {
                for (Path previewImagePath : previewImagePaths) {
                    if (previewImagePath != null && java.nio.file.Files.isRegularFile(previewImagePath)) {
                        try {
                            Image preview = Image.getInstance(previewImagePath.toAbsolutePath().toString());
                            preview.scaleToFit(500, 500);
                            preview.setAlignment(Image.ALIGN_CENTER);
                            document.add(new Paragraph("CBCT Preview: " + previewImagePath.getFileName(), headerFont));
                            document.add(preview);
                        } catch (Exception imgEx) {
                            log.warn("Could not embed preview image {}: {}", previewImagePath, imgEx.getMessage());
                        }
                    }
                }
            }

            if ("demo".equalsIgnoreCase(aiMode)) {
                document.add(new Paragraph(" "));
                
                PdfPTable footerTable = new PdfPTable(1);
                footerTable.setWidthPercentage(100);
                PdfPCell cell = new PdfPCell(new Paragraph("⚠️ CLINICAL DEMONSTRATION MODE ⚠️\n\nThis report was generated using deterministic simulated rules for demonstration purposes only. Real AI inference was bypassed.", new Font(Font.HELVETICA, 10, Font.BOLD, Color.RED)));
                cell.setBackgroundColor(new Color(255, 230, 230));
                cell.setBorderColor(Color.RED);
                cell.setPadding(10);
                footerTable.addCell(cell);
                
                document.add(footerTable);
            }

            document.close();
            log.info("PDF document binary compilation finished successfully.");
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF document using OpenPDF library: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failure: " + e.getMessage());
        }
    }
}
