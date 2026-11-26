CREATE TABLE media_uploads (
    id TEXT PRIMARY KEY NOT NULL,
    owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    object_key TEXT NOT NULL UNIQUE,
    mime_type TEXT NOT NULL,
    byte_size INTEGER NOT NULL CHECK (byte_size > 0),
    status TEXT NOT NULL CHECK (status IN ('pending', 'confirmed', 'attached')),
    expires_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE (owner_id, id)
);

CREATE TABLE garments (
    id TEXT PRIMARY KEY NOT NULL,
    owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    photo_upload_id TEXT NOT NULL UNIQUE,
    photo_key TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL,
    color TEXT NOT NULL,
    tags TEXT NOT NULL,
    season TEXT NOT NULL,
    notes TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('active', 'unavailable', 'archived')),
    available INTEGER NOT NULL CHECK (available IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (owner_id, photo_upload_id) REFERENCES media_uploads(owner_id, id)
);

CREATE INDEX garments_owner_status_idx ON garments(owner_id, status);
CREATE INDEX media_uploads_owner_idx ON media_uploads(owner_id);
