-- XenForo test schema for plugin testing
CREATE TABLE xf_user (
    user_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    user_state ENUM('valid', 'email_confirm', 'moderated', 'rejected', 'disabled') NOT NULL DEFAULT 'valid',
    is_admin TINYINT UNSIGNED NOT NULL DEFAULT 0,
    is_banned TINYINT UNSIGNED NOT NULL DEFAULT 0,
    register_date INT UNSIGNED NOT NULL DEFAULT 0,
    last_activity INT UNSIGNED NOT NULL DEFAULT 0,
    INDEX (username),
    INDEX (email)
);

CREATE TABLE xf_thread (
    thread_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    node_id INT UNSIGNED NOT NULL,
    title VARCHAR(150) NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    username VARCHAR(50) NOT NULL,
    post_date INT UNSIGNED NOT NULL,
    sticky TINYINT UNSIGNED NOT NULL DEFAULT 0,
    discussion_state ENUM('visible', 'moderated', 'deleted') NOT NULL DEFAULT 'visible',
    discussion_open TINYINT UNSIGNED NOT NULL DEFAULT 1,
    reply_count INT UNSIGNED NOT NULL DEFAULT 0,
    view_count INT UNSIGNED NOT NULL DEFAULT 0,
    first_post_id INT UNSIGNED NOT NULL DEFAULT 0,
    last_post_date INT UNSIGNED NOT NULL DEFAULT 0,
    last_post_id INT UNSIGNED NOT NULL DEFAULT 0,
    last_post_user_id INT UNSIGNED NOT NULL DEFAULT 0,
    last_post_username VARCHAR(50) NOT NULL DEFAULT '',
    INDEX (node_id),
    INDEX (user_id),
    INDEX (post_date)
);

CREATE TABLE xf_post (
    post_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    thread_id INT UNSIGNED NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    username VARCHAR(50) NOT NULL,
    post_date INT UNSIGNED NOT NULL,
    message MEDIUMTEXT NOT NULL,
    message_state ENUM('visible', 'moderated', 'deleted') NOT NULL DEFAULT 'visible',
    position INT UNSIGNED NOT NULL DEFAULT 0,
    likes INT UNSIGNED NOT NULL DEFAULT 0,
    INDEX thread_post (thread_id, post_date),
    INDEX (user_id)
);

CREATE TABLE xf_node (
    node_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    node_type_id VARCHAR(25) NOT NULL,
    parent_node_id INT UNSIGNED NOT NULL DEFAULT 0,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    depth TINYINT UNSIGNED NOT NULL DEFAULT 0,
    lft INT UNSIGNED NOT NULL DEFAULT 0,
    rgt INT UNSIGNED NOT NULL DEFAULT 0,
    breadcrumb_data BLOB NOT NULL,
    INDEX (parent_node_id),
    INDEX (display_order)
);

CREATE TABLE xf_session (
    session_id VARBINARY(32) NOT NULL PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL DEFAULT 0,
    ip VARBINARY(16) NOT NULL DEFAULT '',
    expiry_date INT UNSIGNED NOT NULL,
    session_data MEDIUMBLOB NOT NULL,
    INDEX (expiry_date)
);
