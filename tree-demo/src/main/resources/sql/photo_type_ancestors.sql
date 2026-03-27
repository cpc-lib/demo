ALTER TABLE base_photo_type
    ADD COLUMN ancestors VARCHAR(2000) NOT NULL DEFAULT '0' COMMENT '祖级路径，逗号分隔，不包含自己' AFTER parent_id;

CREATE INDEX idx_photo_type_parent_deleted_sort
    ON base_photo_type (parent_id, is_deleted, sort);

WITH RECURSIVE photo_type_tree AS (
    SELECT id,
           parent_id,
           CAST('0' AS CHAR(2000)) AS ancestors,
           1 AS layer
    FROM base_photo_type
    WHERE parent_id = '0'
      AND is_deleted = 0
    UNION ALL
    SELECT child.id,
           child.parent_id,
           CONCAT(parent.ancestors, ',', parent.id) AS ancestors,
           parent.layer + 1 AS layer
    FROM base_photo_type child
             INNER JOIN photo_type_tree parent ON child.parent_id = parent.id
    WHERE child.is_deleted = 0
)
UPDATE base_photo_type target
    INNER JOIN photo_type_tree source ON target.id = source.id
SET target.ancestors = source.ancestors,
    target.layer = source.layer;
