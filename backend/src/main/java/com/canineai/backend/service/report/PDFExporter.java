package com.canineai.backend.service.report;

import java.nio.file.Path;
import java.util.List;

public interface PDFExporter {

    /**
     * Converts HTML formatted templates to raw binary PDF stream outputs.
     */
    byte[] exportPdf(String persistedReportContent, List<Path> previewImagePaths);
}
