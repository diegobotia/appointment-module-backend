-- =====================================================================
-- V26: Asignación de recursos físicos por cita (capacidad operativa)
-- =====================================================================

CREATE TABLE IF NOT EXISTS appointments.appointment_resource_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    resource_type VARCHAR(32) NOT NULL CHECK (
        resource_type IN ('CONSULTORIO', 'FISIOTERAPIA', 'TERAPIA_OCUPACIONAL', 'REUNION_STAFF')
    ),
    facility_resource_id UUID,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity_session_key VARCHAR(160) NOT NULL,
    released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_appointment_resource_allocations_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_resource_alloc_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments.appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_alloc_facility
        FOREIGN KEY (facility_id) REFERENCES appointments.facilities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_resource_alloc_facility_resource
        FOREIGN KEY (facility_resource_id) REFERENCES appointments.facility_resources(id) ON DELETE SET NULL,
    CONSTRAINT chk_resource_alloc_time_window CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_resource_alloc_lookup
    ON appointments.appointment_resource_allocations(
        facility_id, resource_type, appointment_date, start_time, end_time
    )
    WHERE released_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_resource_alloc_session_key
    ON appointments.appointment_resource_allocations(capacity_session_key)
    WHERE released_at IS NULL;
