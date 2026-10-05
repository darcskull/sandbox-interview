package dev.interview.lab.audit;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<AuditDocument, String> {}
