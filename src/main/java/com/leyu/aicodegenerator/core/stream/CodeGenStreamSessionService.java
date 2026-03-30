package com.leyu.aicodegenerator.core.stream;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Slf4j
public class CodeGenStreamSessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(2);
    private static final String KEY_PREFIX = "codegen:stream:";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_ERROR = "ERROR";

    private final StringRedisTemplate redisTemplate;

    private final Map<String, SessionState> sessionStateMap = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessionByOwnerApp = new ConcurrentHashMap<>();

    public CodeGenStreamSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public SessionAttachResult attachOrCreate(long appId,
                                              long userId,
                                              String message,
                                              String sessionId,
                                              long lastSeq,
                                              Supplier<Flux<String>> sourceFluxSupplier) {
        if (lastSeq < 0) {
            lastSeq = 0;
        }
        cleanupCompletedSessions();

        SessionState state;
        if (StrUtil.isNotBlank(sessionId)) {
            state = sessionStateMap.get(sessionId);
        } else {
            state = null;
        }

        if (state == null) {
            state = getOrCreateState(appId, userId, message, sourceFluxSupplier);
        }

        Flux<String> replayFlux = replayFromRedis(state.getSessionId(), lastSeq);
        Flux<String> liveFlux = state.isCompleted() ? Flux.empty() : state.getSink().asFlux();

        return new SessionAttachResult(
                state.getSessionId(),
                replayFlux.concatWith(liveFlux)
        );
    }

    private SessionState getOrCreateState(long appId,
                                          long userId,
                                          String message,
                                          Supplier<Flux<String>> sourceFluxSupplier) {
        String ownerAppKey = buildOwnerAppKey(appId, userId);
        String existingSessionId = activeSessionByOwnerApp.get(ownerAppKey);
        if (StrUtil.isNotBlank(existingSessionId)) {
            SessionState existing = sessionStateMap.get(existingSessionId);
            if (existing != null && !existing.isCompleted() && StrUtil.equals(existing.getMessage(), message)) {
                return existing;
            }
        }

        String newSessionId = UUID.randomUUID().toString().replace("-", "");
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        SessionState newState = new SessionState(newSessionId, appId, userId, message, sink, false, null, System.currentTimeMillis());

        sessionStateMap.put(newSessionId, newState);
        activeSessionByOwnerApp.put(ownerAppKey, newSessionId);
        setStatus(newSessionId, STATUS_RUNNING);

        Disposable disposable = sourceFluxSupplier.get().subscribe(
                chunk -> {
                    appendChunk(newSessionId, chunk);
                    sink.tryEmitNext(chunk);
                },
                error -> {
                    newState.setCompleted(true);
                    newState.setFinishedAt(System.currentTimeMillis());
                    setStatus(newSessionId, STATUS_ERROR);
                    sink.tryEmitComplete();
                    activeSessionByOwnerApp.remove(ownerAppKey, newSessionId);
                    log.warn("Stream session failed, sessionId={}, appId={}, error={}", newSessionId, appId, error.getMessage());
                },
                () -> {
                    newState.setCompleted(true);
                    newState.setFinishedAt(System.currentTimeMillis());
                    setStatus(newSessionId, STATUS_DONE);
                    sink.tryEmitComplete();
                    activeSessionByOwnerApp.remove(ownerAppKey, newSessionId);
                    log.info("Stream session completed, sessionId={}, appId={}", newSessionId, appId);
                }
        );
        newState.setUpstreamDisposable(disposable);
        return newState;
    }

    private Flux<String> replayFromRedis(String sessionId, long lastSeq) {
        String chunkKey = chunkKey(sessionId);
        Long size = redisTemplate.opsForList().size(chunkKey);
        if (size == null || size <= lastSeq) {
            return Flux.empty();
        }
        List<String> chunkList = redisTemplate.opsForList().range(chunkKey, lastSeq, -1);
        if (chunkList == null || chunkList.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(chunkList);
    }

    private void appendChunk(String sessionId, String chunk) {
        redisTemplate.opsForList().rightPush(chunkKey(sessionId), chunk);
        refreshTtl(sessionId);
    }

    private void setStatus(String sessionId, String status) {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        valueOps.set(statusKey(sessionId), status, SESSION_TTL);
        refreshTtl(sessionId);
    }

    private void refreshTtl(String sessionId) {
        redisTemplate.expire(chunkKey(sessionId), SESSION_TTL);
        redisTemplate.expire(statusKey(sessionId), SESSION_TTL);
    }

    private void cleanupCompletedSessions() {
        long now = System.currentTimeMillis();
        sessionStateMap.values().removeIf(state -> state.isCompleted() && now - state.getFinishedAt() > SESSION_TTL.toMillis());
    }

    private String buildOwnerAppKey(long appId, long userId) {
        return appId + ":" + userId;
    }

    private String chunkKey(String sessionId) {
        return KEY_PREFIX + sessionId + ":chunks";
    }

    private String statusKey(String sessionId) {
        return KEY_PREFIX + sessionId + ":status";
    }

    @Data
    @AllArgsConstructor
    private static class SessionState {
        private String sessionId;
        private long appId;
        private long userId;
        private String message;
        private Sinks.Many<String> sink;
        private boolean completed;
        private Disposable upstreamDisposable;
        private long finishedAt;
    }

    public record SessionAttachResult(String sessionId, Flux<String> dataFlux) {}
}
