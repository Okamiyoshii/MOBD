<?php
declare(strict_types=1);
require_once __DIR__ . '/common.php';

// The food photo goes in the "image" form field (multipart/form-data).
handleCrud($db, 'Menu_Items', 'item_id', 'menu_items', ['column' => 'image_path', 'folder' => 'menu']);
