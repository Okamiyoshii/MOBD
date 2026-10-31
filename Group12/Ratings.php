<?php
declare(strict_types=1);
require_once __DIR__ . '/common.php';

// Customers now rate the food truck itself (Ratings.truck_id), not individual stops.
handleCrud($db, 'Ratings', 'rating_id', 'ratings');
