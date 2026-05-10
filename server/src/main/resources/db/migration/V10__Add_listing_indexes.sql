CREATE INDEX IF NOT EXISTS ix_activities_status_created_at
    ON activities(status, created_at DESC, id);

CREATE INDEX IF NOT EXISTS ix_user_favourites_user_type_created_at
    ON user_favourites(user_id, favourite_type, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_user_favourites_user_type_activity
    ON user_favourites(user_id, favourite_type, activity_id);
