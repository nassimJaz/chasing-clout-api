Do $$

    DECLARE _TYPE bigint;
    DECLARE _SLOT bigint;

    BEGIN

    -- Booster Basic: 6 + 4 cards
    INSERT INTO booster_type (code, name, price) VALUES ('BASIC', 'Booster Basic', 100) RETURNING id INTO _TYPE;
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 1, 6) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'COMMON', 60), (_SLOT, 'UNCOMMON', 30), (_SLOT, 'RARE', 10);
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 2, 4) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'RARE', 70), (_SLOT, 'VERY_RARE', 25), (_SLOT, 'ULTRA_RARE', 4), (_SLOT, 'SECRET', 1);

    -- Booster Spécial: 6 + 4 cards
    INSERT INTO booster_type (code, name, price) VALUES ('SPECIAL', 'Booster Spécial', 250) RETURNING id INTO _TYPE;
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 1, 6) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'COMMON', 40), (_SLOT, 'UNCOMMON', 30), (_SLOT, 'RARE', 30);
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 2, 4) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'RARE', 50), (_SLOT, 'VERY_RARE', 40), (_SLOT, 'ULTRA_RARE', 7), (_SLOT, 'SECRET', 3);

    -- Booster Collection: 6 + 4 + 2 cards (slot 1 is 33.33% each, i.e. equal weights)
    INSERT INTO booster_type (code, name, price) VALUES ('COLLECTION', 'Booster Collection', 500) RETURNING id INTO _TYPE;
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 1, 6) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'COMMON', 1), (_SLOT, 'UNCOMMON', 1), (_SLOT, 'RARE', 1);
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 2, 4) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'RARE', 40), (_SLOT, 'VERY_RARE', 45), (_SLOT, 'ULTRA_RARE', 10), (_SLOT, 'SECRET', 5);
    INSERT INTO booster_slot (booster_type_id, position, card_count) VALUES (_TYPE, 3, 2) RETURNING id INTO _SLOT;
    INSERT INTO booster_slot_odds (booster_slot_id, rarity, weight) VALUES
        (_SLOT, 'VERY_RARE', 70), (_SLOT, 'ULTRA_RARE', 20), (_SLOT, 'SECRET', 10);

    END $$;
