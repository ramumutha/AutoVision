package com.autovision.platform.aftersales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceJobRepository
        extends JpaRepository<ServiceJob, UUID> {

    List<ServiceJob> findAllByServiceOrderIdOrderByJobNumber(
            UUID serviceOrderId
    );

    List<ServiceJob> findAllByServiceOrderIdAndStatusOrderByJobNumber(
            UUID serviceOrderId,
            ServiceJobStatus status
    );

    List<ServiceJob> findAllByServiceOrderIdAndApprovalStatusOrderByJobNumber(
            UUID serviceOrderId,
            ServiceJobApprovalStatus approvalStatus
    );

    Optional<ServiceJob> findByIdAndServiceOrderId(
            UUID id,
            UUID serviceOrderId
    );

    Optional<ServiceJob> findByServiceOrderIdAndJobNumber(
            UUID serviceOrderId,
            String jobNumber
    );

    boolean existsByServiceOrderIdAndJobNumber(
            UUID serviceOrderId,
            String jobNumber
    );
}