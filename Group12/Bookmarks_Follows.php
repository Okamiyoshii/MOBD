<?php
declare(strict_types=1);
require_once __DIR__ . '/common.php';

handleCrud($db, 'Bookmarks_Follows', 'bookmark_id', 'bookmarks');
