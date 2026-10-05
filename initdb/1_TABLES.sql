create table app_user
(
    id           BIGSERIAL PRIMARY KEY,
    username     TEXT        not null unique,
    -- in-game currency ("memes"); the check makes a concurrent double spend fail instead of going negative
    meme_balance BIGINT      not null default 0 check (meme_balance >= 0),
    created_at   TIMESTAMPTZ not null default now()
);

create table meme_card
(
    id           BIGSERIAL PRIMARY KEY,
    source_index INT  not null unique, -- row index in the Hugging Face dataset
    name         TEXT not null,
    image_url    TEXT not null,
    description  TEXT null,
    rarity       TEXT not null check (rarity in ('COMMON', 'UNCOMMON', 'RARE', 'VERY_RARE', 'ULTRA_RARE', 'SECRET'))
);
create index on meme_card (rarity);

create table booster_type
(
    id    BIGSERIAL PRIMARY KEY,
    code  TEXT   not null unique,
    name  TEXT   not null,
    price BIGINT not null check (price > 0)
);

create table booster_slot
(
    id              BIGSERIAL PRIMARY KEY,
    booster_type_id BIGINT not null references booster_type (id),
    position        INT    not null,
    card_count      INT    not null check (card_count > 0),
    unique (booster_type_id, position)
);

-- Relative weights, not percentages: (60, 30, 10) and (1, 1, 1) are both valid.
create table booster_slot_odds
(
    booster_slot_id BIGINT not null references booster_slot (id),
    rarity          TEXT   not null check (rarity in ('COMMON', 'UNCOMMON', 'RARE', 'VERY_RARE', 'ULTRA_RARE', 'SECRET')),
    weight          INT    not null check (weight > 0),
    primary key (booster_slot_id, rarity)
);

create table purchase
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      not null references app_user (id),
    format      TEXT        not null check (format in ('SINGLE', 'TRI_PACK', 'DISPLAY_MINI', 'DISPLAY_MEDIUM', 'DISPLAY_MEGA')),
    total_price BIGINT      not null check (total_price >= 0),
    created_at  TIMESTAMPTZ not null default now()
);

-- Booster inventory: opened_at null = not opened yet. purchase_id is null for boosters given for free.
create table user_booster
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      not null references app_user (id),
    booster_type_id BIGINT      not null references booster_type (id),
    purchase_id     BIGINT      null references purchase (id),
    opened_at       TIMESTAMPTZ null
);
create index on user_booster (user_id);

-- History of every card drawn, in reveal order.
create table pulled_card
(
    id              BIGSERIAL PRIMARY KEY,
    user_booster_id BIGINT not null references user_booster (id),
    meme_card_id    BIGINT not null references meme_card (id),
    finish          TEXT   not null check (finish in ('NORMAL', 'HOLO', 'FIRE')),
    position        INT    not null,
    unique (user_booster_id, position)
);

-- The user's collection: one row per (card, finish) with a counter.
create table user_card
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT      not null references app_user (id),
    meme_card_id      BIGINT      not null references meme_card (id),
    finish            TEXT        not null check (finish in ('NORMAL', 'HOLO', 'FIRE')),
    quantity          INT         not null check (quantity > 0),
    first_obtained_at TIMESTAMPTZ not null default now(),
    unique (user_id, meme_card_id, finish)
);
