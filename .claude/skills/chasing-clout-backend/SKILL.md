---
name: chasing-clout-backend
description: How to run, test and change the chasing-clout-api backend (Spring Boot + PostgreSQL in Docker), and the fixes for its known pitfalls. Use for any backend task in this repo — adding an endpoint, a service, a table or a column, running the tests, starting the API — and whenever the API won't start, the database refuses a login, data looks empty or stale, or a test fails on balances.
---

# Chasing Clout backend

Project rules and design choices are in the README (imported by CLAUDE.md). This skill is the how-to.

## Run
- Database: `docker compose up -d` (container `chasing_clout_db`, port from `DATABASE_PORT` in `.env`).
- API: `mvn spring-boot:run` or ▶ on `BackSkeletonApplication` in IntelliJ. `application.properties` imports `.env` itself.
- Tests: `mvn test` (the `*IntegrationTest` / `*ConcurrencyTest` classes need the database). Use `mvn`, not `./mvnw`.
- Quick check: `curl http://localhost:8080/me` → `{"id":1,"username":"demo",...}`. Swagger: `/swagger-ui.html`.

## Change the schema
1. New file `initdb/<n>_<NAME>.sql` (never edit an existing one, never `ddl-auto=update`).
2. Apply it to the running database (initdb scripts only run on an empty volume):
   `docker exec -i chasing_clout_db psql -U root -d mydb -v ON_ERROR_STOP=1 < initdb/<n>_<NAME>.sql`
3. Update the entity in `models/` (no `@Table`/`@Column`: Spring's naming strategy maps `memeBalance` → `meme_balance`).
4. `mvn test`: Hibernate runs in `validate`, so a mismatch fails at startup.
5. Check the scripts from scratch on a throwaway container:
   `docker run --rm -d --name initdb_check -e POSTGRES_USER=root -e POSTGRES_PASSWORD=x -e POSTGRES_DB=mydb -v "$(pwd -W)/initdb:/docker-entrypoint-initdb.d:ro" postgres`,
   wait for `PostgreSQL init process complete` in `docker logs`, query, then `docker stop initdb_check`.
6. Tell teammates to apply the script too (step 2) or `docker compose down -v` (loses their data).

## Write code the project's way
- Packages `models` / `DAO` / `services` / `controllers` / `DTO`, constructor injection, no Lombok, DTOs are `record`s with `from(entity)`.
- Anything that races (money, ownership, one-time actions) is ONE atomic SQL statement in a DAO that returns the number
  of rows changed (`debit`, `markOpened`, `markSold`, `claimFreeBooster`...). The service checks `== 1`, otherwise throws
  `ResponseStatusException` (409 conflict, 429 cooldown, 400 bad input). Never read-then-write in Java.
- Errors reach the front through `server.error.include-message=always`: write the message for the player, in French.
- The current player is the `X-User-Id` header (default 1), declared with `@RequestHeader(value = MeController.USER, defaultValue = "1")`.

## Tests
- Pure logic (prices, odds): plain JUnit, no Spring, static package-private method.
- Integration: `@SpringBootTest @Transactional` (rolled back). Create your own players
  (`appUserDao.save(new AppUser("x-" + System.nanoTime(), 5000))`): never assume `demo`'s balance or inventory, it is the
  developer's real save.
- Concurrency: no `@Transactional`, real threads released by a `CountDownLatch`, setup and cleanup with `JdbcTemplate`
  (see `MarketConcurrencyTest`). Assert exactly one winner.

## Known pitfalls
| Symptom | Cause | Fix |
|---|---|---|
| Entity still shows the old value right after a DAO update (balance, status) | `@Modifying` queries bypass the JPA cache | `@Modifying(flushAutomatically = true, clearAutomatically = true)`, or `entityManager.clear()` before rereading |
| API starts but `/boosters` is `[]`, `/me` is 500 | another Postgres (e.g. an old `api_database` container) holds the port | `docker ps`, stop the other one, `docker compose up -d --force-recreate` |
| `password authentication failed for user "root"` | `.env` password changed after the volume was created (Postgres reads it only once) | `docker exec -it chasing_clout_db psql -U root -d mydb -c "ALTER USER root WITH PASSWORD '<.env value>';"` |
| `npm start` fails with `ENOENT package.json` | run in the API folder | the front is `../chasing-clout-front` |
| Test fails on a balance or an inventory size | it used `demo`, whose data changed | make the test create its own player |
