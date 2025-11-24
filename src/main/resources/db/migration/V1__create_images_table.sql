CREATE TABLE images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255),
    cloud_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_entity_type CHECK (entity_type IN ('USER_PROFILE', 'USER_BANNER', 'PRODUCT'))
);

CREATE INDEX idx_images_user_id ON images(user_id);
CREATE INDEX idx_images_user_id_entity_type ON images(user_id, entity_type);
CREATE INDEX idx_images_product_id ON images(product_id);
CREATE INDEX idx_images_deleted_at ON images(deleted_at);

