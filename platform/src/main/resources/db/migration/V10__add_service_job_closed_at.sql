-- Sprint 5 S5.3.4 Service Job lifecycle persistence hardening.
--
-- WORK_COMPLETED and CLOSED are deliberately distinct lifecycle states.
-- completed_at records physical/service work completion.
-- closed_at records final operational Job closure.
--
-- This migration introduces no invoicing, notification, technician,
-- inventory, checkpoint or approval-workflow dependency.

ALTER TABLE platform.service_jobs
    ADD COLUMN closed_at TIMESTAMPTZ;

ALTER TABLE platform.service_jobs
    ADD CONSTRAINT ck_service_jobs_closed_at
        CHECK (
            closed_at IS NULL
            OR closed_at >= opened_at
        );