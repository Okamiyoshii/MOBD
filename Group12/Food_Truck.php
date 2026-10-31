<?php
declare(strict_types=1);
require_once __DIR__ . '/common.php';

/**
 * One truck with its nested owner, menu, stops, ratings, bookmarks and stats.
 * Stats are calculated live from Ratings / Bookmarks_Follows so they can
 * never go out of date.
 */
function loadTruckDetails(PDO $db, array $truck): array
{
    $tid   = $truck['truck_id'];
    $truck = cleanRow($truck); // adds logo_url, never exposes secrets

    $owner = fetchRelated($db, "SELECT * FROM `Truck_Owners` WHERE `owner_id` = :id", [':id' => $truck['owner_id']]);
    $truck['owner'] = $owner ? cleanRow($owner[0]) : null;

    $menu = fetchRelated($db, "SELECT * FROM `Menu_Items` WHERE `truck_id` = :id", [':id' => $tid]);
    $truck['menu_items'] = array_map('cleanRow', $menu);

    $truck['stops']     = fetchRelated($db, "SELECT * FROM `Stops_Locations` WHERE `truck_id` = :id", [':id' => $tid]);
    $truck['ratings']   = fetchRelated($db, "SELECT * FROM `Ratings` WHERE `truck_id` = :id ORDER BY `created_at` DESC", [':id' => $tid]);
    $truck['bookmarks'] = fetchRelated($db, "SELECT * FROM `Bookmarks_Follows` WHERE `truck_id` = :id", [':id' => $tid]);

    $rating    = fetchRelated(
        $db,
        "SELECT COUNT(*) AS total_reviews, COALESCE(ROUND(AVG(`score`), 1), 0) AS overall_rating FROM `Ratings` WHERE `truck_id` = :id",
        [':id' => $tid]
    );
    $followers = fetchRelated($db, "SELECT COUNT(*) AS followers_count FROM `Bookmarks_Follows` WHERE `truck_id` = :id", [':id' => $tid]);
    $counters  = fetchRelated($db, "SELECT `profile_views_7d`, `route_checks_7d` FROM `Truck_Stats` WHERE `truck_id` = :id", [':id' => $tid]);

    $truck['stats'] = [
        'total_reviews'    => (int)($rating[0]['total_reviews'] ?? 0),
        'overall_rating'   => (float)($rating[0]['overall_rating'] ?? 0),
        'followers_count'  => (int)($followers[0]['followers_count'] ?? 0),
        'profile_views_7d' => (int)($counters[0]['profile_views_7d'] ?? 0),
        'route_checks_7d'  => (int)($counters[0]['route_checks_7d'] ?? 0),
    ];

    return $truck;
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $id = $_GET['id'] ?? null;

    if ($id !== null) {
        $stmt = $db->prepare("SELECT * FROM `Food_Trucks` WHERE `truck_id` = :id");
        $stmt->execute([':id' => $id]);
        $truck = $stmt->fetch();
        if (!$truck) {
            sendJson(['error' => "Truck '{$id}' not found"], 404);
        }
        sendJson(loadTruckDetails($db, $truck));
    }

    $trucks = $db->query("SELECT * FROM `Food_Trucks`")->fetchAll();
    sendJson(array_map(fn($t) => loadTruckDetails($db, $t), $trucks));
}

// POST / PUT / DELETE only touch Food_Trucks. The logo goes in the "image" form field.
handleCrud($db, 'Food_Trucks', 'truck_id', 'trucks', ['column' => 'logo_path', 'folder' => 'trucks']);
