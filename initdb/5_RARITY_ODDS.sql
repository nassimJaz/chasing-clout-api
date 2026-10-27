-- Harder odds for the rare slots (slot 1 of each booster is unchanged). Before, a Collection display gave ~24 Secrètes
-- although only 10 exist. Expected per booster: Basic UR 1/50, S 1/300; Spécial UR 1/20, S 1/100; Collection UR 1/8, S 1/40.
-- Slots are found by booster code + position, never by id. Idempotent: only UPDATE and DELETE.
-- On an existing database: docker exec -i chasing_clout_db psql -U root -d <DATABASE_NAME du .env> -v ON_ERROR_STOP=1 < initdb/5_RARITY_ODDS.sql
BEGIN;

UPDATE booster_slot_odds o
SET weight = v.weight
FROM booster_slot s
         JOIN booster_type t ON t.id = s.booster_type_id,
     (VALUES ('BASIC', 2, 'RARE', 1013), ('BASIC', 2, 'VERY_RARE', 180), ('BASIC', 2, 'ULTRA_RARE', 6), ('BASIC', 2, 'SECRET', 1),
             ('SPECIAL', 2, 'RARE', 244), ('SPECIAL', 2, 'VERY_RARE', 150), ('SPECIAL', 2, 'ULTRA_RARE', 5), ('SPECIAL', 2, 'SECRET', 1),
             ('COLLECTION', 2, 'RARE', 60), ('COLLECTION', 2, 'VERY_RARE', 40),
             ('COLLECTION', 3, 'VERY_RARE', 74), ('COLLECTION', 3, 'ULTRA_RARE', 5), ('COLLECTION', 3, 'SECRET', 1))
         AS v(code, position, rarity, weight)
WHERE o.booster_slot_id = s.id
  AND t.code = v.code
  AND s.position = v.position
  AND o.rarity = v.rarity;

-- Collection slot 2 becomes Rare / Very rare only: Ultra rares and Secrètes come from slot 3.
DELETE
FROM booster_slot_odds o
USING booster_slot s
    JOIN booster_type t ON t.id = s.booster_type_id
WHERE o.booster_slot_id = s.id
  AND t.code = 'COLLECTION'
  AND s.position = 2
  AND o.rarity IN ('ULTRA_RARE', 'SECRET');

COMMIT;
