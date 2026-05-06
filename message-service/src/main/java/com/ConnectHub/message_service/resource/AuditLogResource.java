package com.ConnectHub.message_service.resource;

import com.ConnectHub.message_service.model.AuditLog;
import com.ConnectHub.message_service.repository.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditLogResource {

    private final AuditLogRepository auditLogRepository;

    public AuditLogResource(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/logs")
    public ResponseEntity<AuditLog> createLog(@RequestBody AuditLog log) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(auditLogRepository.save(log));
    }
}
