-- Marketplace: P2P listings and the free booster cooldown.
-- System prices aren't stored: MarketService.basePrice computes them from rarity, finish and the recent sales.

-- Last free booster claimed (null = never). Only set through AppUserDao.claimFreeBooster, which checks the cooldown.
ALTER TABLE app_user ADD COLUMN last_free_booster_claim TIMESTAMPTZ NULL;

-- A card put on sale by a player. The card leaves the seller's collection while it is listed (escrow),
-- so it can't be sold twice. SOLD rows are the P2P transaction history, used to index the system prices.
create table card_listing
(
    id           BIGSERIAL PRIMARY KEY,
    seller_id    BIGINT      not null references app_user (id),
    meme_card_id BIGINT      not null references meme_card (id),
    finish       TEXT        not null check (finish in ('NORMAL', 'HOLO', 'FIRE')),
    price        BIGINT      not null check (price > 0),
    status       TEXT        not null check (status in ('ACTIVE', 'SOLD', 'CANCELLED')),
    created_at   TIMESTAMPTZ not null default now(),
    buyer_id     BIGINT      null references app_user (id),
    sold_at      TIMESTAMPTZ null,
    check (buyer_id <> seller_id),
    check ((status = 'SOLD') = (buyer_id is not null and sold_at is not null))
);
-- recent sales of one (card, finish), see CardListingDao.findRecentSalePrices
create index on card_listing (meme_card_id, finish, sold_at) where status = 'SOLD';

-- A second player, to trade with demo (pick the player with the X-User-Id header).
INSERT INTO app_user (username, meme_balance) VALUES ('rival', 5000);