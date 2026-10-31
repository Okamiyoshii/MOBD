<?php
/**
 * index.php
 * REST API for the StreetEats Application.
 *
 * Routing relies on query parameters (`resource` and `id`):
 *   GET    /index.php?resource=users               -> list users
 *   GET    /index.php?resource=users&id={id}       -> get single user
 *   POST   /index.php?resource=users               -> create a user
 *   PUT    /index.php?resource=users&id={id}       -> update a user
 *   DELETE /index.php?resource=users&id={id}       -> delete a user
 *
 * Supported resources: users, trucks, stops, bookmarks, menu_items, reviews, notifications
 */

declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require_once __DIR__ . '/config.php';

$db = getDbConnection();

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
    return $data ?? [];
}

// --------------------------------------------------------------
// Request Parameters & Resource Mapping
// --------------------------------------------------------------
$resource = $_GET['resource'] ?? 'trucks';
$id       = $_GET['id'] ?? null;
$method   = $_SERVER['REQUEST_METHOD'];

$tables = [
    'users'         => ['table' => 'Users',           'pk' => 'user_id'],
    'trucks'        => ['table' => 'Food_Trucks',     'pk' => 'truck_id'],
    'stops'         => ['table' => 'Stops_Locations', 'pk' => 'stop_id'],
    'bookmarks'     => ['table' => 'Bookmarks',       'pk' => 'bookmark_id'],
    'menu_items'    => ['table' => 'Menu_Items',      'pk' => 'item_id'],
    'reviews'       => ['table' => 'Reviews',         'pk' => 'review_id'],
    'notifications' => ['table' => 'Notifications',   'pk' => 'notification_id'],
];

if (!array_key_exists($resource, $tables)) {
    sendJson(['error' => "Invalid resource '{$resource}'"], 400);
}

$targetTable = $tables[$resource]['table'];
$pkColumn    = $tables[$resource]['pk'];

// --------------------------------------------------------------
// REST Operations
// --------------------------------------------------------------
switch ($method) {

    case 'GET':
        if ($id !== null) {
            $stmt = $db->prepare("SELECT * FROM `{$targetTable}` WHERE `{$pkColumn}` = :id");
            $stmt->execute([':id' => $id]);
            $row = $stmt->fetch();
            
            if (!$row) {
                sendJson(['error' => "Record '{$id}' not found in {$resource}"], 404);
            }
            sendJson($row);
        } else {
            $sql = "SELECT * FROM `{$targetTable}` WHERE 1=1";
            $params = [];

            // Example Filters
            if ($resource === 'trucks' && !empty($_GET['user_id'])) {
                $sql .= " AND user_id = :user_id";
                $params[':user_id'] = $_GET['user_id'];
            }
            if ($resource === 'menu_items' && !empty($_GET['truck_id'])) {
                $sql .= " AND truck_id = :truck_id";
                $params[':truck_id'] = $_GET['truck_id'];
            }

            $stmt = $db->prepare($sql);
            $stmt->execute($params);
            sendJson($stmt->fetchAll());
        }
        break;

    case 'POST':
        $data = readJsonBody();
        if (empty($data)) {
            sendJson(['error' => 'No data provided to create record'], 400);
        }
        if (empty($data[$pkColumn])) {
            sendJson(['error' => "Primary key field '{$pkColumn}' is required"], 422);
        }

        $fields = array_keys($data);
        $columns = implode(', ', array_map(fn($f) => "`{$f}`", $fields));
        $placeholders = implode(', ', array_map(fn($f) => ":{$f}", $fields));

        $sql = "INSERT INTO `{$targetTable}` ({$columns}) VALUES ({$placeholders})";
        
        try {
            $stmt = $db->prepare($sql);
            $stmt->execute($data);
            
            $fetchStmt = $db->prepare("SELECT * FROM `{$targetTable}` WHERE `{$pkColumn}` = :id");
            $fetchStmt->execute([':id' => $data[$pkColumn]]);
            sendJson($fetchStmt->fetch(), 201);
        } catch (PDOException $e) {
            sendJson(['error' => 'Failed to create record', 'details' => $e->getMessage()], 400);
        }
        break;

    case 'PUT':
        if ($id === null) {
            sendJson(['error' => "ID parameter is required to update {$resource}"], 400);
        }

        $data = readJsonBody();
        if (empty($data)) {
            sendJson(['error' => 'No data provided to update record'], 400);
        }

        // Prevent primary key overwriting
        unset($data[$pkColumn]);

        $setClause = implode(', ', array_map(fn($f) => "`{$f}` = :{$f}", array_keys($data)));
        $sql = "UPDATE `{$targetTable}` SET {$setClause} WHERE `{$pkColumn}` = :id";
        
        $params = array_merge($data, ['id' => $id]);

        try {
            $stmt = $db->prepare($sql);
            $stmt->execute($params);

            if ($stmt->rowCount() === 0) {
                sendJson(['error' => "Record '{$id}' not found or no changes made"], 404);
            }

            $fetchStmt = $db->prepare("SELECT * FROM `{$targetTable}` WHERE `{$pkColumn}` = :id");
            $fetchStmt->execute([':id' => $id]);
            sendJson($fetchStmt->fetch());
        } catch (PDOException $e) {
            sendJson(['error' => 'Failed to update record', 'details' => $e->getMessage()], 400);
        }
        break;

    case 'DELETE':
        if ($id === null) {
            sendJson(['error' => "ID parameter is required to delete from {$resource}"], 400);
        }

        $stmt = $db->prepare("DELETE FROM `{$targetTable}` WHERE `{$pkColumn}` = :id");
        $stmt->execute([':id' => $id]);

        if ($stmt->rowCount() === 0) {
            sendJson(['error' => "Record '{$id}' not found"], 404);
        }

        sendJson(['message' => "Record '{$id}' deleted successfully from {$resource}"]);
        break;

    default:
        sendJson(['error' => 'Method not allowed'], 405);
}