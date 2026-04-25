package com.grid.app.service;

import com.grid.app.dto.CommentResponse;
import com.grid.app.dto.CreateCommentRequest;
import com.grid.app.exception.TooManyRequestsException;
import com.grid.app.model.Bot;
import com.grid.app.model.Comment;
import com.grid.app.repository.BotRepository;
import com.grid.app.repository.CommentRepository;
import com.grid.app.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final BotRepository botRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ViralityService viralityService;
    private final NotificationService notificationService;

    public CommentResponse addComment(Long postId, CreateCommentRequest request) {
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        if ("BOT".equals(request.getAuthorType())) {
            return addBotComment(postId, request);
        }

        return persistCommentAndScore(postId, request, "HUMAN_COMMENT");
    }

    private CommentResponse addBotComment(Long postId, CreateCommentRequest request) {
        if (request.getDepthLevel() > 20) {
            throw new IllegalArgumentException("Bot comment depth level exceeds maximum allowed value of 20");
        }

        Long botId = request.getBotId();
        Long userId = request.getUserId();
        String cooldownKey = "cooldown:bot_" + botId + ":human_" + userId;

        Boolean cooldownActive = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(cooldownActive)) {
            throw new TooManyRequestsException(
                    "Cooldown active for bot " + botId + " and user " + userId + ". Please wait 10 minutes."
            );
        }

        String luaScript =
                "local current = redis.call('INCR', KEYS[1]) " +
                "if current > 100 then " +
                "redis.call('DECR', KEYS[1]) " +
                "return -1 " +
                "end " +
                "return current";

        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);
        Long result = stringRedisTemplate.execute(script, List.of("post:" + postId + ":bot_count"));

        if (result != null && result == -1L) {
            throw new TooManyRequestsException(
                    "Bot comment limit of 100 reached for post " + postId
            );
        }

        stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(600));

        CommentResponse response = persistCommentAndScore(postId, request, "BOT_REPLY");

        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + botId));
        notificationService.handleBotNotification(userId, bot.getName());

        return response;
    }

    private CommentResponse persistCommentAndScore(Long postId, CreateCommentRequest request, String interactionType) {
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .botId(request.getBotId())
                .content(request.getContent())
                .depthLevel(request.getDepthLevel())
                .build();
        Comment saved = commentRepository.save(comment);
        viralityService.updateViralityScore(postId, interactionType);
        return CommentResponse.builder()
                .id(saved.getId())
                .postId(saved.getPostId())
                .authorId(saved.getAuthorId())
                .authorType(saved.getAuthorType())
                .botId(saved.getBotId())
                .content(saved.getContent())
                .depthLevel(saved.getDepthLevel())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
