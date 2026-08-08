package com.canineai.backend.service.report;

import java.util.UUID;

public interface PDFService {

    byte[] renderPersistedPdf(UUID reportId, String currentUser);

    boolean canRenderPersistedPdf(UUID reportId, String currentUser);
}
