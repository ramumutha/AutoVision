package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceLineRepository
        extends JpaRepository<ServiceLine, UUID> {

    List<ServiceLine> findAllByServiceOrderIdOrderByLineNumber(
            UUID serviceOrderId
    );

    List<ServiceLine> findAllByServiceOrderIdAndServiceJobIdOrderByLineNumber(
            UUID serviceOrderId,
            UUID serviceJobId
    );

    List<ServiceLine> findAllByServiceOrderIdAndServiceJobIdIsNullOrderByLineNumber(
            UUID serviceOrderId
    );

    Optional<ServiceLine> findByIdAndServiceOrderId(
            UUID id,
            UUID serviceOrderId
    );

    Optional<ServiceLine> findByServiceOrderIdAndLineNumber(
            UUID serviceOrderId,
            int lineNumber
    );

    boolean existsByServiceOrderIdAndLineNumber(
            UUID serviceOrderId,
            int lineNumber
    );
}