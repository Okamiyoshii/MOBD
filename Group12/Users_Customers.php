<?php
declare(strict_types=1);
require_once __DIR__ . '/common.php';

handleCrud($db, 'Users_Customers', 'customer_id', 'customers');
