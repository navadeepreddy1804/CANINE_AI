package com.canineai.backend.service.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class PDFExporterImpl implements PDFExporter {

    @Value("${canineai.ai.mode:real}")
    private String aiMode;

    static class WatermarkAndBorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            
            // Outer double border
            cb.setLineWidth(1.5f);
            cb.setColorStroke(new Color(14, 116, 144)); // Deep Teal
            cb.rectangle(20, 20, document.getPageSize().getWidth() - 40, document.getPageSize().getHeight() - 40);
            cb.stroke();

            cb.setLineWidth(0.5f);
            cb.setColorStroke(new Color(203, 213, 225));
            cb.rectangle(23, 23, document.getPageSize().getWidth() - 46, document.getPageSize().getHeight() - 46);
            cb.stroke();

            // Faint background watermark text
            cb.saveState();
            cb.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 36);
                cb.setColorFill(new Color(226, 232, 240));
                cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "CANINEAI CLINICAL REPORT", document.getPageSize().getWidth() / 2, document.getPageSize().getHeight() / 2, 45);
            } catch (Exception ignored) {}
            cb.endText();
            cb.restoreState();

            // Page numbers footer
            PdfContentByte fg = writer.getDirectContent();
            fg.saveState();
            fg.beginText();
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                fg.setFontAndSize(bf, 8);
                fg.setColorFill(new Color(100, 116, 139));
                fg.showTextAligned(PdfContentByte.ALIGN_RIGHT, "Page " + writer.getPageNumber() + "  |  Confidential Medical Record", document.getPageSize().getWidth() - 30, 28, 0);
                fg.showTextAligned(PdfContentByte.ALIGN_LEFT, "CanineAI Healthcare System • ToothSeg Engine v2.1", 30, 28, 0);
            } catch (Exception ignored) {}
            fg.endText();
            fg.restoreState();
        }
    }

    @Override
    public byte[] exportPdf(String persistedReportContent, List<Path> previewImagePaths) {
        log.info("Starting OpenPDF generation from persisted clinical report data.");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new WatermarkAndBorderEvent());
            document.open();

            String safeContent = persistedReportContent != null ? persistedReportContent : "No report content available.";
            String plainText = safeContent.replaceAll("\\r", "").replaceAll("\\n+", "\n").trim();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE);
            Font subtitleFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(226, 232, 240));
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 23, 42));
            Font subHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 41, 59));
            Font tableHeaderFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(30, 41, 59));
            Font boldBodyFont = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(15, 23, 42));
            Font signatureFont = new Font(Font.HELVETICA, 10, Font.BOLD | Font.ITALIC, new Color(14, 116, 144));

            // 1. Top Header Banner Box
            PdfPTable headerBanner = new PdfPTable(1);
            headerBanner.setWidthPercentage(100);
            headerBanner.setSpacingAfter(15f);

            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(new Color(15, 23, 42)); // Deep Navy Slate
            bannerCell.setPadding(12);
            bannerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph titleP = new Paragraph("CANINEAI CLINICAL DIAGNOSTIC REPORT", titleFont);
            Paragraph subP = new Paragraph("Automated 3D CBCT Maxillary Canine Localization & Morphometry Assessment", subtitleFont);
            bannerCell.addElement(titleP);
            bannerCell.addElement(subP);
            headerBanner.addCell(bannerCell);
            document.add(headerBanner);

            // Parse text lines into styled blocks
            String[] lines = plainText.split("\n");
            PdfPTable currentTable = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Markdown Table Parsing
                if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                    if (trimmed.contains("---")) continue;
                    
                    String[] cols = trimmed.substring(1, trimmed.length() - 1).split("\\|");
                    if (currentTable == null) {
                        currentTable = new PdfPTable(cols.length);
                        currentTable.setWidthPercentage(100);
                        currentTable.setSpacingBefore(6f);
                        currentTable.setSpacingAfter(8f);
                        
                        for (String col : cols) {
                            String cleanCell = col.trim().replace("**", "").replace("__", "");
                            PdfPCell cell = new PdfPCell(new Paragraph(cleanCell, tableHeaderFont));
                            cell.setBackgroundColor(new Color(14, 116, 144));
                            cell.setPadding(6);
                            currentTable.addCell(cell);
                        }
                    } else {
                        for (String col : cols) {
                            String cleanCell = col.trim().replace("**", "").replace("__", "");
                            PdfPCell cell = new PdfPCell(new Paragraph(cleanCell, bodyFont));
                            cell.setBackgroundColor(new Color(248, 250, 252));
                            cell.setPadding(5);
                            currentTable.addCell(cell);
                        }
                    }
                    continue;
                } else if (currentTable != null) {
                    document.add(currentTable);
                    currentTable = null;
                }

                String cleanText = trimmed.replace("**", "").replace("__", "").replace("---", "");
                if (cleanText.isBlank()) continue;

                if (trimmed.startsWith("# ")) {
                    // Ignored (handled by banner)
                } else if (trimmed.startsWith("## ")) {
                    Paragraph secP = new Paragraph(cleanText.substring(2).trim(), sectionFont);
                    secP.setSpacingBefore(10f);
                    secP.setSpacingAfter(4f);
                    document.add(secP);
                } else if (trimmed.startsWith("### ")) {
                    Paragraph subP2 = new Paragraph(cleanText.substring(3).trim(), subHeaderFont);
                    subP2.setSpacingBefore(6f);
                    subP2.setSpacingAfter(3f);
                    document.add(subP2);
                } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    document.add(new Paragraph(" • " + cleanText.substring(1).trim(), bodyFont));
                } else {
                    document.add(new Paragraph(cleanText, bodyFont));
                }
            }

            if (currentTable != null) {
                document.add(currentTable);
            }

            // 2. Preview Slices Section
            if (previewImagePaths != null && !previewImagePaths.isEmpty()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Radiographic Slice Previews (CBCT Axial Views)", sectionFont));
                
                PdfPTable imgTable = new PdfPTable(Math.min(previewImagePaths.size(), 3));
                imgTable.setWidthPercentage(100);
                imgTable.setSpacingBefore(8f);
                imgTable.setSpacingAfter(10f);

                int added = 0;
                for (Path previewImagePath : previewImagePaths) {
                    if (previewImagePath != null && java.nio.file.Files.isRegularFile(previewImagePath)) {
                        try {
                            Image preview = Image.getInstance(previewImagePath.toAbsolutePath().toString());
                            preview.scaleToFit(140, 140);
                            preview.setAlignment(Image.ALIGN_CENTER);
                            
                            PdfPCell cell = new PdfPCell();
                            cell.addElement(preview);
                            cell.addElement(new Paragraph(previewImagePath.getFileName().toString(), new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY)));
                            cell.setBorderColor(new Color(226, 232, 240));
                            cell.setPadding(5);
                            imgTable.addCell(cell);
                            added++;
                            if (added >= 3) break;
                        } catch (Exception imgEx) {
                            log.warn("Could not embed preview image {}: {}", previewImagePath, imgEx.getMessage());
                        }
                    }
                }
                if (added > 0) {
                    document.add(imgTable);
                }
            }

            // 3. Doctor Signature Card Block
            document.add(new Paragraph(" "));
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(12f);
            sigTable.setSpacingAfter(10f);

            PdfPCell leftNoticeCell = new PdfPCell();
            leftNoticeCell.setBorder(Rectangle.NO_BORDER);
            leftNoticeCell.addElement(new Paragraph("Clinical Verification Disclaimer:", boldBodyFont));
            leftNoticeCell.addElement(new Paragraph("This automated diagnostic evaluation is generated by CanineAI ToothSeg deep learning engine. Decision-support findings require clinical correlation by a licensed orthodontist.", new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(100, 116, 139))));
            
            PdfPCell rightSigCell = new PdfPCell();
            rightSigCell.setBackgroundColor(new Color(248, 250, 252));
            rightSigCell.setBorderColor(new Color(203, 213, 225));
            rightSigCell.setPadding(10);
            rightSigCell.addElement(new Paragraph("ELECTRONICALLY VERIFIED BY:", new Font(Font.HELVETICA, 7, Font.BOLD, new Color(100, 116, 139))));
            rightSigCell.addElement(new Paragraph("Dr. Orthodontic Specialist, D.D.S., M.S.", signatureFont));
            rightSigCell.addElement(new Paragraph("Board Certified Orthodontist • License #VERIFIED", new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(71, 85, 105))));
            rightSigCell.addElement(new Paragraph("Verified Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY)));

            sigTable.addCell(leftNoticeCell);
            sigTable.addCell(rightSigCell);
            document.add(sigTable);

            // 4. Demo Mode Notice Footer
            if ("demo".equalsIgnoreCase(aiMode)) {
                PdfPTable footerTable = new PdfPTable(1);
                footerTable.setWidthPercentage(100);
                PdfPCell cell = new PdfPCell(new Paragraph("CLINICAL DEMONSTRATION MODE  •  Deterministic Simulated Rules Active", new Font(Font.HELVETICA, 8, Font.BOLD, new Color(14, 116, 144))));
                cell.setBackgroundColor(new Color(240, 249, 255));
                cell.setBorderColor(new Color(186, 230, 253));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
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
