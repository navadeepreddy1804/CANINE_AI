package com.canineai.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class DicomValidator {

    /**
     * Checks if the stream contains a valid DICOM preamble.
     * DICOM standard specifies a 128-byte preamble followed by "DICM" signature.
     */
    public boolean isValidDicom(InputStream stream) {
        try {
            if (!stream.markSupported()) {
                log.warn("Input stream does not support mark/reset, wrapping in BufferedInputStream");
                stream = new BufferedInputStream(stream);
            }
            
            stream.mark(133);
            byte[] buffer = new byte[132];
            int bytesRead = readFully(stream, buffer);
            stream.reset();

            if (bytesRead < 132) {
                return false;
            }

            // Check signature "DICM" at offset 128
            return buffer[128] == 'D' &&
                   buffer[129] == 'I' &&
                   buffer[130] == 'C' &&
                   buffer[131] == 'M';

        } catch (IOException e) {
            log.error("Failed to read header stream for DICOM verification check", e);
            return false;
        }
    }

    public boolean isValidDicomFile(File file) {
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            return isValidDicom(fis);
        } catch (IOException e) {
            log.error("Failed to read local file: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    private int readFully(InputStream is, byte[] b) throws IOException {
        int offset = 0;
        int len = b.length;
        while (offset < len) {
            int count = is.read(b, offset, len - offset);
            if (count < 0) {
                break;
            }
            offset += count;
        }
        return offset;
    }
}
