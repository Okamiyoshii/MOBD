<?php
declare(strict_types=1);

// index.php no longer touches the database. It only lists the endpoints.
header('Content-Type: application/json; charset=utf-8');

echo json_encode([
    'message'   => 'StreetEats API',
    'endpoints' => [
        'trucks'     => 'Food_Truck.php',
        'owners'     => 'Truck_Owners.php',
        'customers'  => 'Users_Customers.php',
        'bookmarks'  => 'Bookmarks_Follows.php',
        'menu_items' => 'Menu_Items.php',
        'stops'      => 'Stops_Locations.php',
        'ratings'    => 'Ratings.php',
    ],
    'usage' => 'GET/POST/PUT/DELETE on each file. Use ?id=... for a single record.',
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
