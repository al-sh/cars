package com.carsai.back.llm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LlmLogRepository extends JpaRepository<LlmLog, UUID> {
}
