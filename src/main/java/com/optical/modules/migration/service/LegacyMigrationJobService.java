package com.optical.modules.migration.service;

import com.optical.modules.migration.dto.LegacyCustomerPrescriptionImportResponse;
import com.optical.modules.migration.dto.LegacyMigrationJobResponse;
import com.optical.modules.migration.dto.LegacyMigrationJobStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LegacyMigrationJobService {

    private final LegacyCustomerPrescriptionImportService legacyCustomerPrescriptionImportService;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public LegacyMigrationJobResponse startCustomerPrescriptionImport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL dump file is required");
        }

        String jobId = UUID.randomUUID().toString();
        byte[] fileBytes = readFile(file);
        LocalDateTime createdAt = LocalDateTime.now();
        JobState state = new JobState(jobId, file.getOriginalFilename(), createdAt);
        jobs.put(jobId, state);

        CompletableFuture.runAsync(() -> runImport(state, fileBytes));

        return LegacyMigrationJobResponse.builder()
                .jobId(jobId)
                .status(state.status)
                .sourceFileName(state.sourceFileName)
                .createdAt(state.createdAt)
                .build();
    }

    public LegacyMigrationJobStatusResponse getJobStatus(String jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Migration job not found");
        }

        return LegacyMigrationJobStatusResponse.builder()
                .jobId(state.jobId)
                .status(state.status)
                .sourceFileName(state.sourceFileName)
                .createdAt(state.createdAt)
                .startedAt(state.startedAt)
                .finishedAt(state.finishedAt)
                .message(state.message)
                .progress(state.progress)
                .build();
    }

    private void runImport(JobState state, byte[] fileBytes) {
        try {
            legacyCustomerPrescriptionImportService.importFromSqlDump(
                    state.sourceFileName,
                    fileBytes,
                    new LegacyCustomerPrescriptionImportService.ImportProgressListener() {
                        @Override
                        public void onStarted() {
                            state.status = "RUNNING";
                            state.startedAt = LocalDateTime.now();
                            state.message = "Migration started";
                        }

                        @Override
                        public void onProgress(LegacyCustomerPrescriptionImportResponse progress) {
                            state.progress = progress;
                            state.message = "Migration in progress";
                        }

                        @Override
                        public void onCompleted(LegacyCustomerPrescriptionImportResponse result) {
                            state.progress = result;
                            state.status = "COMPLETED";
                            state.finishedAt = LocalDateTime.now();
                            state.message = "Migration completed";
                        }
                    }
            );
        } catch (Exception ex) {
            state.status = "FAILED";
            state.finishedAt = LocalDateTime.now();
            state.message = ex.getMessage() == null ? "Migration failed" : ex.getMessage();
        }
    }

    private byte[] readFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded SQL dump", ex);
        }
    }

    private static final class JobState {
        private final String jobId;
        private final String sourceFileName;
        private final LocalDateTime createdAt;
        private volatile String status = "QUEUED";
        private volatile LocalDateTime startedAt;
        private volatile LocalDateTime finishedAt;
        private volatile String message = "Migration queued";
        private volatile LegacyCustomerPrescriptionImportResponse progress;

        private JobState(String jobId, String sourceFileName, LocalDateTime createdAt) {
            this.jobId = jobId;
            this.sourceFileName = sourceFileName;
            this.createdAt = createdAt;
        }
    }
}
