# Redis Guardrail Service

This project is a simple backend service that uses Redis to add guardrails or limits to actions such as posting comments or sending notifications. It's written in Java using Spring Boot. The database is PostgreSQL, and Redis is used for rate limits, counters, and some notification logic.

Think of it like a basic “social media” backend, where you can create users, bots, posts, comments, and likes — with special logic so bots can’t overload posts or reply too fast, all enforced by Redis!

---

## Requirements

- **Java 21**
- **Maven**
- **Docker and Docker Compose** (for running PostgreSQL and Redis easily)
- **Postman**

---

## How to Run

1. **Start PostgreSQL & Redis**  
   The simplest way is with Docker Compose. Just run:

   ```bash
   docker-compose up -d
   ```
   - This will start PostgreSQL (port 5432) and Redis (port 6379) as defined in `docker-compose.yml`.

2. **Start the Java App**  
   From the project folder, run:
   ```bash
   mvn spring-boot:run
   ```
   - The API server starts on [http://localhost:8080](http://localhost:8080).

3. **Try the API**  
   - Import `SpringGrid.postman_collection.json` into Postman for ready to use API requests like “create user”, “create bot”, “add comment”, and so on.

---

## Main Features (in simple words)

- **Users and Bots:** Anyone can create users and bots. Bots can comment too, but face special limits.
- **Posts and Comments:** Users and bots create posts and add comments. Bots are held back by rules:
  - *Depth Cap:* Prevents bots from replying too “deep” in threads (max 20).
  - *Cooldown Cap:* Prevents bots from replying to the same user too quickly (wait 10 minutes).
  - *Horizontal Cap:* Limits how many unique bots can comment on a single post (max 100).
- **Virality Score:** Every post has a score in Redis that grows with likes and comments.
- **Simple Notification System:** Sends out notifications using Redis lists and cooldown keys.

---

## Stopping Everything

When you want to stop and clean up, just run:
```bash
docker-compose down
```
To also remove database data:
```bash
docker-compose down -v
```

---

## Extra Info

- Everything runs locally by default: Java app, Postgres, and Redis.
- There’s a Postman collection (`SpringGrid.postman_collection.json`) to make testing easy.
- Docker Compose file (`docker-compose.yml`) is provided so you don’t need to install Postgres or Redis manually.
- The app creates all needed tables automatically on startup.

---

If you have any issues running the project, make sure Docker is installed and working and that you have the right Java version.
