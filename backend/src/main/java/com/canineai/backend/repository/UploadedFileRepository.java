package com.canineai.backend.repository;

import com.canineai.backend.entity.UploadSession;
import com.canineai.backend.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    List<UploadedFile> findBySession(UploadSession session);
    List<UploadedFile> findBySessionId(UUID sessionId);
}
