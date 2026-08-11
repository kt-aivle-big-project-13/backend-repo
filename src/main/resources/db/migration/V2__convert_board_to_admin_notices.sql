DROP TABLE IF EXISTS board_comments;

ALTER TABLE board_posts
    DROP COLUMN IF EXISTS pinned;
