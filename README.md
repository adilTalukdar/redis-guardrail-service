# Redis Guardrail Service

This project is a simple backend service that uses Redis to add guardrails or limits to actions such as posting comments or sending notifications. It's written in Java using Spring Boot. The database is PostgreSQL, and Redis is used for rate limits, counters, and some notification logic.

Think of it like a basic “social media” backend, where you can create users, bots, posts, comments, and likes — with special logic so bots can’t overload posts or reply too fast, all enforced by Redis!

A production-ready Spring Boot 3.x microservice built with Java 21 and Maven, featuring Redis-backed atomic guardrails, virality scoring, and a notification engine for a bot-powered social posting platform.

---

## Tech Stack

| Layer        | Technology                            |
|--------------|---------------------------------------|
| Language     | Java 21                               |
| Framework    | Spring Boot 3.4.5                     |
| Persistence  | Spring Data JPA + PostgreSQL 15       |
| Cache/Queue  | Spring Data Redis + Lettuce + Redis 7 |
| Build        | Maven                                 |
| Utilities    | Lombok                                |

---

## Project Structure

```
src/main/java/com/grid/app/
├── controller/
│   ├── AppUserController.java
│   ├── BotController.java
│   └── PostController.java
├── service/
│   ├── AppUserService.java
│   ├── BotService.java
│   ├── PostService.java
│   ├── CommentService.java
│   ├── ViralityService.java
│   └── NotificationService.java
├── repository/
│   ├── AppUserRepository.java
│   ├── BotRepository.java
│   ├── PostRepository.java
│   └── CommentRepository.java
├── model/
│   ├── AppUser.java
│   ├── Bot.java
│   ├── Post.java
│   └── Comment.java
├── dto/
│   ├── CreateUserRequest.java   UserResponse.java
│   ├── CreateBotRequest.java    BotResponse.java
│   ├── CreatePostRequest.java   PostResponse.java
│   ├── CreateCommentRequest.java CommentResponse.java
│   └── LikePostRequest.java     LikePostResponse.java
├── config/
│   └── RedisConfig.java
├── scheduler/
│   └── NotificationScheduler.java
└── exception/
    ├── TooManyRequestsException.java
    └── GlobalExceptionHandler.java
```

---

## Prerequisites

- Java 21
- Maven 
- Docker and Docker Compose (for PostgreSQL and Redis)

---

## Running the Project

### Step 1 — Start PostgreSQL and Redis

**Option A: Docker Compose (recommended)**
```bash
docker-compose up -d
```

This will start:
- PostgreSQL 15 on port `5432` with database `springgrid`
- Redis 7 on port `6379`

**Option B: Local installation**

Ensure PostgreSQL is running with:
- Database: `springgrid`
- Username: `postgres`
- Password: `postgres`
- Port: `5432`

Ensure Redis is running on:
- Host: `localhost`
- Port: `6379`

### Step 2 — Run the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

Spring Boot will auto-create all tables via `spring.jpa.hibernate.ddl-auto=update`.

---

## API Endpoints

| Method | Endpoint                         | Description              |
|--------|----------------------------------|--------------------------|
| POST   | `/api/users`                     | Create a user            |
| POST   | `/api/bots`                      | Create a bot             |
| POST   | `/api/posts`                     | Create a post            |
| POST   | `/api/posts/{postId}/comments`   | Add a comment to a post  |
| POST   | `/api/posts/{postId}/like`       | Like a post              |

Import `SpringGrid.postman_collection.json` into Postman for ready-to-use requests.


---

## Virality Score (Redis)

- Redis key: `post:{postId}:virality_score`
- Points are computed as follows:

| Interaction Type | Points Added |
|------------------|--------------|
| `BOT_REPLY`      | +1           |
| `HUMAN_LIKE`     | +20          |
| `HUMAN_COMMENT`  | +50          |

Uses `StringRedisTemplate.opsForValue().increment()` for atomic increments.

---

## Redis Guardrails (Bot Comments)

Guardrails are enforced in this order every time a bot comments:

### 1. Vertical Cap (depth check)
If `depthLevel > 20`, the request is rejected immediately with HTTP 400 — no Redis I/O occurs.

### 2. Cooldown Cap
Redis key: `cooldown:bot_{botId}:human_{userId}`

- If the key exists → HTTP 429 (bot replied to this user too recently, 10-minute window).
- If not → key is set with TTL 600 seconds after the comment is saved.

### 3. Horizontal Cap (Atomic Lock)

**Thread Safety & Atomicity of the Horizontal Cap:**

The horizontal cap ensures that no more than 100 bot replies can be made to a post. This mechanism uses a Redis Lua script executed atomically in Redis:

```lua
local current = redis.call('INCR', KEYS[1])
if current > 100 then
  redis.call('DECR', KEYS[1])
  return -1
end
return current
```

#### Explanation of Thread Safety (Atomic Lock in Phase 2)

**Approach:**
- The horizontal cap is enforced by running the above Lua script via `RedisTemplate.execute()`.
- All mutating operations (`INCR`, check, optional `DECR`) are bundled inside the single Lua script and sent as one command to the Redis server.

**Guaranteeing Thread Safety:**
- **No JVM-level synchronization is used or needed.**
- **Redis executes commands with single-threaded semantics:** Every Lua script execution is atomic — it runs to completion before any other Redis command (even from other clients) can execute.
- Thus, even with hundreds of concurrent threads or clients, there is **zero chance of a race condition**.
- If the post hits the 100-bot cap, the Lua script rolls back (`DECR`) and the comment is rejected with HTTP 429. All concurrent attempts are serialized by Redis itself.

**Summary:**  
Thread safety and correct Atomic Lock behavior are **guaranteed by Redis's built-in command queue and atomic Lua script execution**. There are no distributed races, and no possibility of “lost updates” or “phantom increments”, regardless of application load.

---

## Notification Engine

After a bot comment is saved, `NotificationService.handleBotNotification(userId, botName)` runs:

- If cooldown key `user:{userId}:notif_cooldown` EXISTS → message is pushed to Redis List `user:{userId}:pending_notifs`
- If cooldown key does NOT exist → notification is sent immediately (logged) and cooldown is set for 900 seconds (15 minutes)

### Cron Sweeper

`NotificationScheduler` runs every 5 minutes (`fixedRate = 300000`):

1. Scans for all `user:*:pending_notifs` keys
2. Drains each list atomically (range + delete)
3. Logs a summary: `"Summarized Push Notification: <first_message> and <N> others interacted with your posts."`

---

## Redis Keys Reference

| Key Pattern                           | Purpose                         | TTL         |
|---------------------------------------|---------------------------------|-------------|
| `post:{postId}:virality_score`        | Running virality score          | No TTL      |
| `post:{postId}:bot_count`             | Count of bot comments on post   | No TTL      |
| `cooldown:bot_{botId}:human_{userId}` | Per-bot-per-user cooldown       | 600 seconds |
| `user:{userId}:notif_cooldown`        | Notification send cooldown      | 900 seconds |
| `user:{userId}:pending_notifs`        | Queued notification messages    | No TTL      |

---

## Stopping the Services

```bash
docker-compose down
```

To also remove the persisted PostgreSQL volume:

```bash
docker-compose down -v
```