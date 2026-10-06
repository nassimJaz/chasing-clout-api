# Chasing Clout — API

Jeu de cartes à collectionner (TCG) basé sur les 300 mèmes du dataset
[not-lain/meme-dataset](https://huggingface.co/datasets/not-lain/meme-dataset).
Back : Java 17, Spring Boot 3.1, JPA/Hibernate 6, PostgreSQL. Front : Angular 17 (repo `chasing-clout-front`).

## Set up
1. Copie-colle le `.env.sample` en `.env`
2. Remplis le fichier `.env` avec les credentials de ton choix (`DATABASE_PORT=5432` par défaut ; change-le si un autre Postgres utilise déjà ce port)
3. Fais un `docker compose up -d` : crée la base et exécute `initdb/*.sql` (tables, boosters, 300 cartes, utilisateur `demo`)
   - Les scripts ne tournent **que sur un volume vide**. Après une modif SQL : `docker compose down -v` puis `docker compose up -d`
4. Rajoute le plugin : [env-file](https://plugins.jetbrains.com/plugin/7861-envfile)
5. Ajoute ton fichier `.env` comme ci-dessous (n'oublie pas d'activer les fichiers cachés)
<img src="img-readme/img.png">
<img src="img-readme/img_1.png">
<img src="img-readme/img_2.png">

6. Lance `BackSkeletonApplication`. Les images des cartes sont servies sur `http://localhost:8080/memes/<slug>.jpg`

## Modèle de données

Le schéma est écrit en SQL dans `initdb/` ; Hibernate est en `ddl-auto=validate` : il ne crée rien, il vérifie au
démarrage que les entités (`models/`) correspondent aux tables. Les noms de tables/colonnes viennent de la naming
strategy de Spring Boot (`AppUser` → `app_user`, `memeBalance` → `meme_balance`), d'où l'absence de `@Table`/`@Column`.

```mermaid
erDiagram
    app_user ||--o{ purchase : "achète"
    app_user ||--o{ user_booster : "possède"
    app_user ||--o{ user_card : "collectionne"
    booster_type ||--|{ booster_slot : "contient"
    booster_slot ||--|{ booster_slot_odds : "cotes"
    booster_type ||--o{ user_booster : ""
    purchase ||--o{ user_booster : "a produit"
    user_booster ||--o{ pulled_card : "a donné"
    meme_card ||--o{ pulled_card : ""
    meme_card ||--o{ user_card : ""
    app_user ||--o{ card_listing : "vend / achète"
    meme_card ||--o{ card_listing : ""
```

| Table | Rôle |
|---|---|
| `app_user` | Joueur, son solde de *memes* (`CHECK >= 0`) et `last_free_booster_claim` (cooldown du booster gratuit) |
| `meme_card` | Les 300 cartes ; chacune a une rareté **fixe** |
| `booster_type` | BASIC (100), SPECIAL (250), COLLECTION (500) |
| `booster_slot` / `booster_slot_odds` | Slots d'un booster (nb de cartes) et poids de chaque rareté |
| `purchase` | Une commande en boutique (format, prix total) |
| `user_booster` | Inventaire de boosters ; `opened_at` null = pas encore ouvert |
| `pulled_card` | Historique : chaque carte tirée, avec sa finition et sa position |
| `user_card` | Collection : une ligne par (joueur, carte, finition) avec une quantité |
| `card_listing` | Marketplace P2P : annonce ACTIVE / SOLD / CANCELLED ; les lignes SOLD sont l'historique des ventes |

## Choix de conception
- **Rareté sur la carte, finition sur le tirage.** Les 300 mèmes sont répartis 100 Commun / 75 Peu commun / 55 Rare /
  40 Très rare / 20 Ultra rare / 10 Secrète (mélange à graine fixe, voir `scripts/import_memes.py`). La finition
  (Normale 94 %, Holo 5 %, Enflammée 1 %) est tirée pour chaque carte, cf. l'enum `Finish`.
- **Cotes en poids relatifs**, pas en pourcentages : `(60, 30, 10)`, et `(1, 1, 1)` pour les 33,33 % du Booster Collection.
- **La concurrence est gérée par la base**, via 3 requêtes atomiques :
  `AppUserDao.debit` (pas de solde négatif), `UserBoosterDao.markOpened` (un booster ne s'ouvre qu'une fois),
  `UserCardDao.addOne` (upsert de la collection). Toujours passer par elles.
- **Images en statique** dans `src/main/resources/static/memes/` : les URLs du dataset Hugging Face expirent.
  `scripts/import_memes.py` (Python stdlib) régénère les images et `initdb/3_CARDS.sql` ; ne pas éditer ce fichier à la main.
- **Prix et économie** (valeurs de départ, modifiables dans `2_DEFAULT_ENTRIES.sql`) : Basic 100, Spécial 250,
  Collection 500 ; l'utilisateur `demo` démarre avec 5000 memes.

### Règles décidées pour la suite
- **Remise** : sur `n` boosters, `n / 3` (division entière) sont à -60 %, soit `prix × 40 / 100` (les prix sont multiples de 5, donc ça tombe juste).
- **Mixte 50/50** : seulement pour Display Medium (15 + 15, 5 remisés par type) et Display Méga (30 + 30, 10 par type) ;
  1, 3 et 9 boosters ne se coupent pas en deux moitiés égales.
- **Achat ≠ ouverture** : acheter crée des `user_booster` non ouverts, l'ouverture est un appel séparé.
- **Pas d'authentification pour l'instant** : le joueur courant vient du header `X-User-Id` (défaut `demo`, id 1 ; `rival` = id 2
  sur une base neuve). N'importe qui peut le changer : à remplacer par une vraie session/JWT.

### Marketplace (phase 8)
- **« Effet » = finition.** Une carte vendue est un couple (carte, finition). Prix algorithmique =
  `Rarity.price` (5 / 10 / 25 / 60 / 150 / 400) × `Finish.priceMultiplier` (Normale ×1, Holo ×4, Enflammée ×15).
- **Prix de base indexé sur le P2P** (`MarketService.basePrice`) : avec au moins 3 ventes, sur les 10 dernières,
  `prix = algo + 30 % × (95 % × moyenne − algo)`. Le lissage (30 %) et la marge (5 %) gardent le prix **sous la moyenne** ;
  un marché qui baisse ne le fait jamais passer sous le prix algorithmique.
- **Le système rachète, il ne vend pas** : `MarketService.sellToSystem` retire une carte de la collection (`removeOne`, atomique)
  et crédite `buyback = max(1, 50 % × prix de base)`. Pour acheter une carte précise, il faut passer par une annonce P2P.
- **Temps réel, pas de worker** : le prix est calculé à la lecture (requête indexée sur ≤ 10 lignes), donc toujours à jour
  et rien à synchroniser. Un worker/cache ne deviendrait utile qu'avec beaucoup de trafic.
- **Achat simultané d'une même annonce** : `CardListingDao.markSold` (`UPDATE … WHERE status = 'ACTIVE'`) ne réussit que pour
  un acheteur ; l'autre attend le verrou de ligne puis voit SOLD (409). Débit, crédit et carte sont dans la même transaction :
  si le débit échoue, tout est annulé et l'annonce redevient ACTIVE. Les deux soldes sont verrouillés dans l'ordre des id
  (pas de deadlock si A achète à B pendant que B achète à A).
- **Séquestre** : une carte mise en vente sort de la collection (`UserCardDao.removeOne`, atomique) et revient si on annule.
- **Booster gratuit toutes les 30 min** : `AppUserDao.claimFreeBooster` compare `last_free_booster_claim` à l'horloge
  **de la base** (`now()`), jamais à celle du client, dans un seul `UPDATE` : des requêtes rejouées ou simultanées font
  la queue sur le verrou de ligne et une seule passe (les autres : 429). Le booster obtenu s'ouvre par la route habituelle.

## API

Swagger : `http://localhost:8080/swagger-ui.html`. Toutes les réponses sont des DTO (`record` dans `DTO/`), jamais des entités.
CORS ouvert par `@CrossOrigin` sur les controllers. En cas d'erreur, le JSON contient un champ `message` lisible (« Solde insuffisant »…).

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/cards` | Les 300 cartes (`id`, `name`, `imageUrl`, `rarity`) |
| GET | `/boosters` | Les 3 boosters : `price`, `prices` par format, `slots` avec leurs poids. Mixte = `(prices[A] + prices[B]) / 2` |
| GET | `/me` | Profil du joueur (`username`, `memeBalance`, `nextFreeBoosterAt`) |
| POST | `/me/purchases` | Achat : `{"format": "DISPLAY_MEDIUM", "boosterTypeId": 1, "secondBoosterTypeId": 2}` (le 2e id est optionnel). 409 si solde insuffisant, 400 si mixte hors Medium/Méga |
| GET | `/me/boosters` | Boosters non ouverts (`id`, `code`, `name`) |
| POST | `/me/boosters/{id}/open` | Ouvre : cartes tirées dans l'ordre (`position`, `card`, `finish`). 409 si déjà ouvert ou pas au joueur |
| GET | `/me/cards` | Collection : (`card`, `finish`, `quantity`, `firstObtainedAt`) |
| POST | `/me/free-booster` | Booster Basic gratuit (non ouvert). 429 si le dernier date de moins de 30 min ; `GET /me` donne `nextFreeBoosterAt` |
| GET | `/market/prices/{cardId}` | Pour chaque finition : prix de base système (`price`) et prix de rachat (`buyback`) |
| POST | `/market/system-sales` | Vente d'une carte au système : `{"memeCardId": 1, "finish": "HOLO"}` → montant crédité. 409 si la carte n'est pas possédée |
| GET | `/market/listings` | Annonces actives, moins chères d'abord (`card`, `finish`, `price`, `seller`) |
| POST | `/market/listings` | Mettre en vente : `{"memeCardId": 1, "finish": "NORMAL", "price": 300}`. 409 si la carte n'est pas possédée |
| DELETE | `/market/listings/{id}` | Retirer son annonce (la carte revient). 409 si déjà vendue ou pas à soi |
| POST | `/market/listings/{id}/purchase` | Acheter une annonce. 409 si déjà vendue, à soi, ou solde insuffisant |

Toutes les routes `/me/...` et `/market/...` qui agissent pour un joueur lisent le header `X-User-Id` (défaut 1).

## Avancement

- [x] **Phase 0 — Socle** : `initdb/` monté dans Docker, port configurable, conteneur renommé `chasing_clout_db`, Hibernate en `validate`
- [x] **Phase 0 bis — Supprimer l'exemple du squelette** (Student/Major/Course : modèles, DAO, services, controllers, DTO) : vérifié, `mvn test` (contextLoads) passe sur un Postgres neuf en `validate`
- [x] **Phase 1 — Import du dataset** : 300 images + `initdb/3_CARDS.sql`
- [x] **Phase 2 — Schéma, entités JPA, DAO** : vérifié sur un Postgres neuf (scripts OK, contraintes et upsert OK, Spring démarre en `validate`, images servies)
- [x] **Phase 3 — `GachaService` (ouverture d'un booster) + tests** : vérifié, `mvn test` passe (7 tests) sur la base lancée
  - `GachaService.open(userId, userBoosterId)` : `markOpened` d'abord (sinon `ResponseStatusException` 409),
    puis pour chaque slot rareté pondérée → carte uniforme dans la rareté → finition pondérée ; `PulledCard` (positions 1..n) + `addOne`.
  - Bean `RandomGenerator` dans `BackSkeletonApplication` (graine fixe possible en test).
  - `GachaServiceTest` (sans base) : fréquences sur 1M tirages de `pickWeighted` à ±0,2 % pour (60, 30, 10), (1, 1, 1) et les finitions.
  - `GachaServiceIntegrationTest` (base requise, rollback) : 10 / 10 / 12 cartes, positions, rareté permise par le slot,
    collection incrémentée, deuxième ouverture refusée.
- [x] **Phase 4 — Boutique (`ShopService`)** : vérifié, `mvn test` passe (19 tests) sur la base lancée
  - `ShopService.buy(userId, format, boosterTypeId, secondBoosterTypeId)` : `secondBoosterTypeId` non null = mixte 50/50
    (Display Medium/Méga seulement, sinon 400) ; `debit` d'abord (sinon 409 « Solde insuffisant »), puis `Purchase` + `n` `UserBooster` non ouverts.
  - `ShopService.price(unitPrice, count)` : `count / 3` boosters à -60 % ; en mixte, appliqué à chaque moitié.
  - `ShopServiceTest` (sans base) : prix des 5 formats et des mixtes. `ShopServiceIntegrationTest` (base requise, rollback) :
    débit et boosters créés, mixte 15 + 15, solde insuffisant (rien n'est acheté), mixte refusé hors Medium/Méga.
- [x] **Phase 5 — API REST** (voir la section API) : vérifié, `mvn test` passe (21 tests) et l'appli lancée répond (JSON, messages d'erreur, Swagger, images)
  - `CatalogController` (`/cards`, `/boosters`) et `MeController` (`/me/...`, utilisateur `demo` en dur) ; lectures directement via les DAO, écritures via les services.
  - `server.error.include-message=always` pour que le front reçoive les messages des `ResponseStatusException`.
  - `ApiIntegrationTest` (MockMvc, base requise, rollback) : achat → inventaire → ouverture (10 cartes) → 409 à la réouverture → collection ; 409 si solde insuffisant.
- [x] **Phase 6 — Front Angular** (branche `feature/tcg-pages` du front) : vérifié, `ng build` (prod) passe et parcours testé dans Chrome
  (achat Tri-pack → solde mis à jour → inventaire → ouverture → « Ouvrir le suivant » → collection et filtres)
  - Pages `boutique`, `inventaire`, `ouverture/:id`, `collection` ; squelette Student/Major/Home et images Takima supprimés.
  - `GameService` : tous les appels API + `profile$` (solde dans la navbar, rechargé après un achat). Modèles dans `models/game.model.ts`.
  - `meme-card` : carte réutilisée (retournement 3D, cadre par rareté, reflet holo, flammes animées, grisée si non possédée).
  - Les composants n'importent pas `theme.scss` (ça embarque tout le CSS Material, budget prod dépassé) : couleurs en variables `--cc-purple` / `--cc-red` dans `styles.scss`.
- [x] **Phase 7 — Finitions** : URL de l'API dans `environment.ts`, CORS via `@CrossOrigin`, README du front (lancement, pages, organisation du code, pièges)

- [x] **Phase 8 — Marketplace** (voir « Marketplace » dans les choix de conception) : vérifié, `mvn test` passe (34 tests),
  scripts `initdb/` rejoués sur un Postgres vide
  - `initdb/4_MARKETPLACE.sql` : `card_listing`, `app_user.last_free_booster_claim`, joueur `rival`. Sur une base existante :
    `docker exec -i chasing_clout_db psql -U root -d <DATABASE_NAME du .env> -v ON_ERROR_STOP=1 --single-transaction < initdb/4_MARKETPLACE.sql` (sinon `docker compose down -v`).
  - `MarketService` (prix, rachat par le système, annonces, achat P2P), `ShopService.claimFreeBooster`, `MarketController`, header `X-User-Id`.
  - `MarketServiceTest` (formule de prix), `MarketServiceIntegrationTest` (parcours, rollback), `MarketConcurrencyTest`
    (vrais threads, données commitées puis supprimées : 2 acheteurs → 1 seul gagne ; paiement refusé → annonce remise en vente ;
    5 réclamations simultanées → 1 seul booster). Les tests créent leurs propres joueurs au lieu d'utiliser `demo`.
  - Non fait : front de la marketplace (phase 9), authentification réelle, baisse du prix sous le prix algorithmique.
- [ ] **Phase 9 — Front de la marketplace** (repo `chasing-clout-front`, toutes les routes existent déjà côté API)
  - **Modèles** (`models/game.model.ts`) : `nextFreeBoosterAt: string | null` dans `Profile` ; nouveaux types
    `Listing` (`id`, `card`, `finish`, `price`, `status`, `seller`, `createdAt`) et `CardPrice` (`memeCardId`, `finish`, `price`, `buyback`).
  - **`GameService`** : `prices(cardId)`, `sellToSystem(cardId, finish)`, `listings()`, `createListing(cardId, finish, price)`,
    `cancelListing(id)`, `buyListing(id)`, `claimFreeBooster()`. Appeler `refreshProfile()` après chaque appel qui change le solde.
  - **Choix du joueur** (`X-User-Id`) : un sélecteur `demo` / `rival` dans la navbar, gardé en `localStorage`, et un
    `HttpInterceptor` qui ajoute le header à toutes les requêtes. Indispensable pour tester l'achat entre deux joueurs.
  - **Booster gratuit** (navbar) : bouton « Booster gratuit » actif si `nextFreeBoosterAt` est null ou passé, sinon compte à
    rebours. Le compte à rebours n'est qu'un affichage, c'est l'API qui décide (429 si trop tôt, afficher son `message`).
    En cas de succès, aller sur `/ouverture/:id` avec l'id du booster reçu.
  - **Nouvelle page `/marche`** : les annonces actives (`GET /market/listings`, réutiliser `meme-card`) avec prix, vendeur et le
    prix système pour comparer (`GET /market/prices/{cardId}`). Bouton « Acheter », masqué sur ses propres annonces
    (`seller === profile.username`), « Retirer » à la place. Après un achat ou un retrait, recharger la liste et le solde.
    Une 409 veut dire « déjà vendue / solde insuffisant » : afficher le `message` (fonction `apiError` existante).
  - **Collection** : sur une carte possédée, bouton « Vendre » avec un champ prix (pré-rempli avec le prix système de cette
    finition) → `POST /market/listings`, la carte disparaît de la collection tant qu'elle est en vente. Sur une carte possédée,
    « Vendre au système » avec le prix de rachat (`buyback`) de chaque finition → `POST /market/system-sales`.
  - **Routing / module** : route `marche` (resolver `listings`), lien « Marché » dans la navbar, déclarer les nouveaux composants
    dans `app.module.ts`. Garder les couleurs en variables CSS (pas de `@use "theme"` dans un composant, cf. README du front).
  - Pas encore d'endpoint pour l'historique de ses ventes / achats ni pour « mes annonces » (on filtre `GET /market/listings`
    par vendeur) : à ajouter côté API si le front en a besoin.

### Points ouverts
- Quelques noms du dataset sont douteux pour un projet d'école (ex. « Pedobear ») : à filtrer ou renommer dans `scripts/import_memes.py` si besoin.
- Les tests `*IntegrationTest` et `BackSkeletonApplicationTests` (contextLoads) ont besoin de la base lancée.
- Après `AppUserDao.debit`/`credit` (requêtes de masse), un `AppUser` déjà chargé dans la même transaction garde l'ancien solde : le relire après un `clear()`.
  `markSold`, `markCancelled` et `claimFreeBooster` vident déjà le cache JPA (`clearAutomatically`).
- Pas de validation des corps de requête : un `format` ou `boosterTypeId` manquant dans `/me/purchases` donne une 500 (le front envoie toujours les deux).
- `./mvnw` ne marche pas (dossier `.mvn/wrapper/` absent du repo) : utiliser un Maven installé (`mvn`) ou celui d'IntelliJ.

## Reprendre le projet avec Claude Code
- `CLAUDE.md` importe ce README : Claude le lit automatiquement au démarrage d'une session dans ce repo.
- Plugin [Ponytail](https://github.com/DietrichGebert/ponytail) (pousse l'IA à écrire le code le plus simple possible) :
  déclaré dans `.claude/settings.json`, Claude Code propose de l'installer au premier lancement dans ce repo (accepter le
  marketplace puis le plugin). À défaut : `/plugin marketplace add DietrichGebert/ponytail` puis `/plugin install ponytail@ponytail`.
- Plugin [Superpowers](https://github.com/obra/superpowers) (TDD, débogage méthodique, vérification avant de dire « fini ») :
  déclaré au même endroit (`superpowers@claude-plugins-official`), même procédure d'installation.
- Skill du projet `.claude/skills/chasing-clout-backend/` : comment lancer, tester et modifier le schéma, et les pièges déjà
  rencontrés (port 5432 pris, mot de passe Postgres, cache JPA après une requête `@Modifying`…). Chargé automatiquement
  quand une tâche backend s'y prête, rien à installer.
- Exemple de prompt : *« Lis le README et attaque la phase 3 »*. À la fin de chaque phase, cocher la case ci-dessus et noter ce qui a été vérifié.
