package com.canineai.backend.service.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DocumentBuilder {

    /**
     * Generates a unique SHA-256 hash for report verification integrity checks.
     */
    public String calculateIntegrityHash(String markdown) {
        if (markdown == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(markdown.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String hash = hexString.toString();
            log.info("Computed SHA-256 clinical integrity hash: {}", hash);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to compute document hash", e);
            return "HASH_COMPUTATION_ERROR";
        }
    }
}
