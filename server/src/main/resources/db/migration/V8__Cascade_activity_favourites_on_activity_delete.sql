-- Keep user activity favourites dependent on the referenced activity.
ALTER TABLE user_favourites DROP CONSTRAINT IF EXISTS user_favourites_activity_id_fkey;

ALTER TABLE user_favourites
    ADD CONSTRAINT user_favourites_activity_id_fkey
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE;
