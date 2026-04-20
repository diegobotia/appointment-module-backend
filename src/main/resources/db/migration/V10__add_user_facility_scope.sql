-- =============================================
-- V10: ALCANCE DE USUARIOS POR SEDE
-- =============================================

CREATE TABLE IF NOT EXISTS user_facilities (
    user_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, facility_id),
    CONSTRAINT fk_user_facilities_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_facilities_facility
        FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_facilities_user_id ON user_facilities(user_id);
CREATE INDEX IF NOT EXISTS idx_user_facilities_facility_id ON user_facilities(facility_id);

-- Garantiza continuidad operativa: usuarios existentes obtienen acceso a sedes activas.
INSERT INTO user_facilities (user_id, facility_id)
SELECT u.id, f.id
FROM users u
CROSS JOIN facilities f
WHERE f.is_active = true
ON CONFLICT (user_id, facility_id) DO NOTHING;
