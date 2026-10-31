<?php

declare(strict_types=1);

// Never show PHP errors to API clients (they leak paths and SQL details).
ini_set('display_errors', '0');
ini_set('display_startup_errors', '0');
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require_once __DIR__ . '/config.php';

const UPLOAD_DIR       = __DIR__ . '/uploads';
const MAX_UPLOAD_BYTES = 2 * 1024 * 1024; // 2 MB

$db = getDbConnection();

/* ---------------------------------------------------------------- basics */

function sendJson($data, int $status = 200): void
{
    http_response_code($status);
    echo json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    exit;
}

function readJsonBody(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === '' || $raw === false) {
        return [];
    }
    $data = json_decode($raw, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        sendJson(['error' => 'Invalid JSON body'], 400);
    }
    return is_array($data) ? $data : [];
}

/** JSON body, or form fields when the app sends multipart/form-data (image uploads). */
function readRequestData(): array
{
    $type = $_SERVER['CONTENT_TYPE'] ?? '';
    if (stripos($type, 'multipart/form-data') !== false) {
        return $_POST;
    }
    return readJsonBody();
}

function fetchRelated(PDO $db, string $query, array $params): array
{
    try {
        $stmt = $db->prepare($query);
        $stmt->execute($params);
        return $stmt->fetchAll();
    } catch (PDOException $e) {
        return [];
    }
}

/* ---------------------------------------------------------------- images */

/** Full public URL for a stored image path, e.g. "menu/itm-0001-ab12cd34.jpg". */
function imageUrl(?string $path): ?string
{
    if (!$path) {
        return null;
    }
    $scheme = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
    $base   = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
    return $scheme . '://' . $_SERVER['HTTP_HOST'] . $base . '/uploads/' . $path;
}

/**
 * Saves the uploaded file sent in the "image" form field.
 * Returns the stored relative path, or null if no file was sent.
 */
function saveUploadedImage(string $folder, string $baseName): ?string
{
    if (!isset($_FILES['image']) || $_FILES['image']['error'] === UPLOAD_ERR_NO_FILE) {
        return null;
    }
    $file = $_FILES['image'];

    if ($file['error'] !== UPLOAD_ERR_OK) {
        sendJson(['error' => 'Image upload failed (code ' . $file['error'] . ')'], 400);
    }
    if ($file['size'] > MAX_UPLOAD_BYTES) {
        sendJson(['error' => 'Image must be 2 MB or smaller'], 413);
    }

    // Check the real file type, not the name the client claims.
    $mime    = (new finfo(FILEINFO_MIME_TYPE))->file($file['tmp_name']);
    $allowed = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
    if (!isset($allowed[$mime])) {
        sendJson(['error' => 'Only JPG, PNG or WEBP images are allowed'], 415);
    }

    $dir = UPLOAD_DIR . '/' . $folder;
    if (!is_dir($dir) && !mkdir($dir, 0755, true)) {
        error_log("Could not create upload folder {$dir}");
        sendJson(['error' => 'Server could not store the image'], 500);
    }

    // Random server-chosen name: the client never controls the file name.
    $safeBase = preg_replace('/[^A-Za-z0-9_-]/', '', $baseName);
    $name     = $safeBase . '-' . bin2hex(random_bytes(4)) . '.' . $allowed[$mime];

    if (!move_uploaded_file($file['tmp_name'], $dir . '/' . $name)) {
        error_log("move_uploaded_file failed for {$dir}/{$name}");
        sendJson(['error' => 'Server could not store the image'], 500);
    }
    return $folder . '/' . $name;
}

function deleteImageFile(?string $path): void
{
    if (!$path) {
        return;
    }
    $root = realpath(UPLOAD_DIR);
    $full = realpath(UPLOAD_DIR . '/' . $path);
    // Only ever delete files that are really inside the uploads folder.
    if ($root && $full && strpos($full, $root . DIRECTORY_SEPARATOR) === 0 && is_file($full)) {
        unlink($full);
    }
}

/* ------------------------------------------------------------ row output */

/**
 * Prepares a DB row for the API response:
 *  - removes password hashes
 *  - for every "*_path" column adds a matching "*_url" (logo_path -> logo_url)
 */
function cleanRow(array $row): array
{
    unset($row['password_hash']);
    foreach ($row as $key => $value) {
        if (substr($key, -5) === '_path') {
            $row[substr($key, 0, -5) . '_url'] = imageUrl($value);
        }
    }
    return $row;
}

/** Real column names of a table, read from the database itself. */
function tableColumns(PDO $db, string $table): array
{
    $stmt = $db->query("SHOW COLUMNS FROM `{$table}`");
    return array_column($stmt->fetchAll(), 'Field');
}

function rejectUnknownFields(PDO $db, string $table, array $data, ?string $cleanupPath): void
{
    $unknown = array_diff(array_keys($data), tableColumns($db, $table));
    if ($unknown) {
        deleteImageFile($cleanupPath);
        sendJson(['error' => 'Unknown field(s): ' . implode(', ', $unknown)], 422);
    }
}

/* ------------------------------------------------------------------ CRUD */

/**
 * Generic CRUD for ONE table. $table and $pk are hardcoded by each endpoint
 * file, never taken from the request.
 *
 * $image (optional) = ['column' => 'logo_path', 'folder' => 'trucks']
 * turns on image upload: send multipart/form-data with the file in the
 * "image" field.
 *
 *   POST   file.php            -> create (JSON, or multipart with image)
 *   POST   file.php?id=X       -> update (use this when changing the image)
 *   PUT    file.php?id=X       -> update (JSON only)
 */
function handleCrud(PDO $db, string $table, string $pk, string $label, ?array $image = null): void
{
    $method = $_SERVER['REQUEST_METHOD'];
    $id     = $_GET['id'] ?? null;

    switch ($method) {

        case 'GET':
            if ($id !== null) {
                $stmt = $db->prepare("SELECT * FROM `{$table}` WHERE `{$pk}` = :id");
                $stmt->execute([':id' => $id]);
                $row = $stmt->fetch();
                if (!$row) {
                    sendJson(['error' => "Record '{$id}' not found in {$label}"], 404);
                }
                sendJson(cleanRow($row));
            }
            $rows = $db->query("SELECT * FROM `{$table}`")->fetchAll();
            sendJson(array_map('cleanRow', $rows));
            break;

        case 'POST':
            if ($id !== null) {
                crudUpdate($db, $table, $pk, $label, $id, $image);
            } else {
                crudCreate($db, $table, $pk, $image);
            }
            break;

        case 'PUT':
            if ($id === null) {
                sendJson(['error' => "ID parameter is required to update {$label}"], 400);
            }
            crudUpdate($db, $table, $pk, $label, $id, $image);
            break;

        case 'DELETE':
            if ($id === null) {
                sendJson(['error' => "ID parameter is required to delete from {$label}"], 400);
            }
            $oldPath = null;
            if ($image) {
                $sel = $db->prepare("SELECT `{$image['column']}` FROM `{$table}` WHERE `{$pk}` = :id");
                $sel->execute([':id' => $id]);
                $oldPath = $sel->fetchColumn() ?: null;
            }
            $stmt = $db->prepare("DELETE FROM `{$table}` WHERE `{$pk}` = :id");
            $stmt->execute([':id' => $id]);
            if ($stmt->rowCount() === 0) {
                sendJson(['error' => "Record '{$id}' not found"], 404);
            }
            deleteImageFile($oldPath);
            sendJson(['message' => "Record '{$id}' deleted successfully from {$label}"]);
            break;

        default:
            sendJson(['error' => 'Method not allowed'], 405);
    }
}

function crudCreate(PDO $db, string $table, string $pk, ?array $image): void
{
    $data = readRequestData();
    if ($image) {
        unset($data[$image['column']]); // the stored path is set by the server only
    }
    if (empty($data)) {
        sendJson(['error' => 'No data provided to create record'], 400);
    }
    if (empty($data[$pk])) {
        sendJson(['error' => "Primary key field '{$pk}' is required"], 422);
    }
    rejectUnknownFields($db, $table, $data, null);

    $newPath = null;
    if ($image) {
        $newPath = saveUploadedImage($image['folder'], (string)$data[$pk]);
        if ($newPath !== null) {
            $data[$image['column']] = $newPath;
        }
    }

    $fields       = array_keys($data);
    $columns      = implode(', ', array_map(fn($f) => "`{$f}`", $fields));
    $placeholders = implode(', ', array_map(fn($f) => ":{$f}", $fields));

    try {
        $stmt = $db->prepare("INSERT INTO `{$table}` ({$columns}) VALUES ({$placeholders})");
        $stmt->execute($data);

        $fetch = $db->prepare("SELECT * FROM `{$table}` WHERE `{$pk}` = :id");
        $fetch->execute([':id' => $data[$pk]]);
        sendJson(cleanRow($fetch->fetch()), 201);
    } catch (PDOException $e) {
        deleteImageFile($newPath);
        error_log($e->getMessage());
        sendJson(['error' => 'Failed to create record'], 400);
    }
}

function crudUpdate(PDO $db, string $table, string $pk, string $label, string $id, ?array $image): void
{
    $existing = $db->prepare("SELECT * FROM `{$table}` WHERE `{$pk}` = :id");
    $existing->execute([':id' => $id]);
    $current = $existing->fetch();
    if (!$current) {
        sendJson(['error' => "Record '{$id}' not found in {$label}"], 404);
    }

    $data = readRequestData();
    unset($data[$pk]); // never allow the primary key to be changed
    if ($image) {
        unset($data[$image['column']]);
    }
    rejectUnknownFields($db, $table, $data, null);

    $newPath = null;
    $oldPath = null;
    if ($image) {
        $newPath = saveUploadedImage($image['folder'], $id);
        if ($newPath !== null) {
            $data[$image['column']] = $newPath;
            $oldPath = $current[$image['column']] ?? null;
        }
    }
    if (empty($data)) {
        sendJson(['error' => 'No data provided to update record'], 400);
    }

    $setClause = implode(', ', array_map(fn($f) => "`{$f}` = :{$f}", array_keys($data)));

    try {
        $stmt = $db->prepare("UPDATE `{$table}` SET {$setClause} WHERE `{$pk}` = :__id");
        $stmt->execute($data + ['__id' => $id]);
    } catch (PDOException $e) {
        deleteImageFile($newPath);
        error_log($e->getMessage());
        sendJson(['error' => 'Failed to update record'], 400);
    }

    deleteImageFile($oldPath); // remove the replaced image

    $fetch = $db->prepare("SELECT * FROM `{$table}` WHERE `{$pk}` = :id");
    $fetch->execute([':id' => $id]);
    sendJson(cleanRow($fetch->fetch()));
}
