<?php
/**
 * MenuItem.php
 * Data-access layer for the `Menu_Items` table.
 */

class MenuItem
{
    private PDO $db;
    private string $table = 'Menu_Items';

    public function __construct(PDO $db)
    {
        $this->db = $db;
    }

    /** Return all menu items, optionally filtered by truck_id. */
    public function getAll(array $filters = []): array
    {
        $sql = "SELECT * FROM {$this->table} WHERE 1=1";
        $params = [];

        if (!empty($filters['truck_id'])) {
            $sql .= " AND truck_id = :truck_id";
            $params[':truck_id'] = $filters['truck_id'];
        }

        $sql .= " ORDER BY item_id ASC";

        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        return $stmt->fetchAll();
    }

    /** Return a single menu item by string item_id, or null if not found. */
    public function getById(string $id): ?array
    {
        $stmt = $this->db->prepare("SELECT * FROM {$this->table} WHERE item_id = :id");
        $stmt->execute([':id' => $id]);
        $row = $stmt->fetch();
        return $row ?: null;
    }

    /** Create a menu item using the provided string item_id. Returns the item_id. */
    public function create(array $data): string
    {
        $sql = "INSERT INTO {$this->table}
                    (item_id, truck_id, item_name, price, description, image_url)
                VALUES
                    (:item_id, :truck_id, :item_name, :price, :description, :image_url)";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':item_id'     => $data['item_id'],
            ':truck_id'    => $data['truck_id'],
            ':item_name'   => $data['item_name'],
            ':price'       => $data['price'],
            ':description' => $data['description'] ?? null,
            ':image_url'   => $data['image_url']   ?? null,
        ]);

        return $data['item_id'];
    }

    /** Update a menu item. Only provided fields are updated. Returns true if a row changed. */
    public function update(string $id, array $data): bool
    {
        $fields = [];
        $params = [':id' => $id];

        $allowed = ['truck_id', 'item_name', 'price', 'description', 'image_url'];
        foreach ($allowed as $field) {
            if (array_key_exists($field, $data)) {
                $fields[] = "{$field} = :{$field}";
                $params[":{$field}"] = $data[$field];
            }
        }

        if (empty($fields)) {
            return false;
        }

        $sql = "UPDATE {$this->table} SET " . implode(', ', $fields) . " WHERE item_id = :id";
        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);

        return $stmt->rowCount() > 0;
    }

    /** Delete a menu item by string item_id. Returns true if a row was removed. */
    public function delete(string $id): bool
    {
        $stmt = $this->db->prepare("DELETE FROM {$this->table} WHERE item_id = :id");
        $stmt->execute([':id' => $id]);
        return $stmt->rowCount() > 0;
    }

    /** Check whether an item_id already exists. */
    public function idExists(string $id): bool
    {
        $stmt = $this->db->prepare("SELECT COUNT(*) FROM {$this->table} WHERE item_id = :id");
        $stmt->execute([':id' => $id]);
        return (int) $stmt->fetchColumn() > 0;
    }
}