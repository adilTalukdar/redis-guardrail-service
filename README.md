# SpringGrid

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
- Maven 3.9+
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

### Sample Requests

**Create User**
```json
POST /api/users
{ "username": "john", "isPremium": true }
```

**Create Bot**
```json
POST /api/bots
{ "name": "BotAlpha", "personaDescription": "Friendly bot" }
```

**Create Post**
```json
POST /api/posts
{ "authorId": 1, "authorType": "USER", "content": "Hello world" }
```

**Add Bot Comment** (triggers all Redis guardrails)
```json
POST /api/posts/1/comments
{
  "authorId": 1,
  "authorType": "BOT",
  "botId": 1,
  "userId": 1,
  "content": "Nice post!",
  "depthLevel": 1
}
```

**Like Post**
```json
POST /api/posts/1/like
{ "userId": 1 }
```

---

## HTTP Status Codes

| Status | Meaning                                      |
|--------|----------------------------------------------|
| 200    | Success (like post)                          |
| 201    | Created (user, bot, post, comment)           |
| 400    | Bad Request (depth > 20, post not found)     |
| 429    | Too Many Requests (horizontal/cooldown cap)  |
| 500    | Internal Server Error                        |

---

## Virality Score (Redis)

Redis key: `post:{postId}:virality_score`

| Interaction Type | Points Added |
|------------------|--------------|
| `BOT_REPLY`      | +1           |
| `HUMAN_LIKE`     | +20          |
| `HUMAN_COMMENT`  | +50          |

Uses `StringRedisTemplate.opsForValue().increment()` for atomic increments.

---

## Redis Guardrails (Bot Comments)

Guardrails are enforced in this order for every bot comment:

### 1. Vertical Cap (depth check)
If `depthLevel > 20`, the request is rejected immediately with HTTP 400 — no Redis I/O occurs.

### 2. Cooldown Cap
Redis key: `cooldown:bot_{botId}:human_{userId}`

If the key exists → HTTP 429 (bot replied to this user too recently, 10-minute window).
If not → the key is set with TTL 600 seconds after the comment is saved.

### 3. Horizontal Cap
Redis key: `post:{postId}:bot_count`

A Lua script is executed atomically via `RedisTemplate.execute(RedisScript)`:

```lua
local current = redis.call('INCR', KEYS[1])
if current > 100 then
  redis.call('DECR', KEYS[1])
  return -1
end
return current
```

If result is `-1` → HTTP 429 (100-bot limit reached).

### Thread Safety Explanation

The Horizontal Cap Lua script is the only place where a check-then-act pattern is required, and it is guaranteed race-condition-free by Redis's own architecture.

Redis is **single-threaded** for command execution. When `RedisTemplate.execute(RedisScript)` sends the Lua script, Redis executes the entire script — `INCR`, the boundary check, and the optional `DECR` — as a single **indivisible, atomic operation**. No other client command can interleave between these steps.

This means that even with **200 concurrent JVM threads** all calling the endpoint simultaneously, each thread's Lua script is queued and executed one at a time inside Redis. The counter will increment from 1 to 100 sequentially, and the 101st (and beyond) will always receive `-1` — the limit is enforced precisely at 100 with **zero possibility of a race condition**.

This is the gold standard for distributed rate limiting: no JVM-level synchronization, no distributed locks, just Redis's native single-threaded Lua execution.

---

## Notification Engine

After a bot comment is saved, `NotificationService.handleBotNotification(userId, botName)` runs:

- **If cooldown key `user:{userId}:notif_cooldown` EXISTS** → message is pushed to Redis List `user:{userId}:pending_notifs`
- **If cooldown key does NOT exist** → notification is sent immediately (logged), and cooldown is set for 900 seconds (15 minutes)

### Cron Sweeper

`NotificationScheduler` runs every 5 minutes (`fixedRate = 300000`):
1. Scans for all `user:*:pending_notifs` keys
2. Drains each list atomically (range + delete)
3. Logs a summary: `"Summarized Push Notification: <first_message> and <N> others interacted with your posts."`

---

## Redis Keys Reference

| Key Pattern                          | Purpose                         | TTL         |
|--------------------------------------|---------------------------------|-------------|
| `post:{postId}:virality_score`       | Running virality score          | No TTL      |
| `post:{postId}:bot_count`            | Count of bot comments on post   | No TTL      |
| `cooldown:bot_{botId}:human_{userId}`| Per-bot-per-user cooldown       | 600 seconds |
| `user:{userId}:notif_cooldown`       | Notification send cooldown      | 900 seconds |
| `user:{userId}:pending_notifs`       | Queued notification messages    | No TTL      |

---

## Stopping the Services

```bash
docker-compose down
```

To also remove the persisted PostgreSQL volume:

```bash
docker-compose down -v
```
