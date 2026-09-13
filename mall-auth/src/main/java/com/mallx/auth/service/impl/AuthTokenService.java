package com.mallx.auth.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.crypto.digest.DigestUtil;
import com.mallx.auth.config.AuthTokenProperties;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.auth.enums.AccountStatus;
import com.mallx.auth.mapper.AuthAccountMapper;
import com.mallx.auth.vo.LoginResponse;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** 负责双Token签发、轮换和同一会话的成对注销。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenService {
    private static final String LINKED_REFRESH_DIGEST = "mallx:refresh-digest";
    private static final String REFRESH_TOKEN_PREFIX = "mallx:auth:v1:refresh:";
    private static final String REFRESH_LOCK_PREFIX = "mallx:auth:v1:lock:refresh:";
    private static final long REFRESH_LOCK_WAIT_SECONDS = 2;
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthTokenProperties properties;
    private final AuthAccountMapper authAccountMapper;
    private final RedissonClient redissonClient;

    /** 登录成功后签发短期Access Token和长期Refresh Token。 */
    public LoginResponse issue(Long userId) {
        long accessSeconds = properties.getAccessTtl().toSeconds();
        long refreshSeconds = properties.getRefreshTtl().toSeconds();
        String accessToken = randomToken();
        String refreshToken = randomToken();
        String refreshDigest = refreshDigest(refreshToken); // 提前计算避免多次计算
        boolean accessIssued = false;
        boolean refreshIssued = false;
        try {
            StpUtil.login(userId, SaLoginParameter.create()
                    .setToken(accessToken)
                    .setTimeout(accessSeconds)
                    .setActiveTimeout(accessSeconds));
            accessIssued = true;
            refreshBucketByDigest(refreshDigest).set(sessionValue(userId, accessToken), properties.getRefreshTtl());
            refreshIssued = true;

            StpLogic accessTokenLogic = StpUtil.getStpLogic(); // 获取当前登录态的逻辑对象
            accessTokenLogic.getTokenSessionByToken(accessToken, true)
                    .set(LINKED_REFRESH_DIGEST, refreshDigest);

            return LoginResponse.builder()
                    .userId(userId)
                    .tokenName(StpUtil.getTokenName())
                    .tokenValue(accessToken)
                    .accessTokenExpiresIn(accessSeconds)
                    .refreshToken(refreshToken)
                    .refreshTokenExpiresIn(refreshSeconds)
                    .build();
        } catch (RuntimeException exception) {
            if (accessIssued) {
                StpUtil.logoutByTokenValue(accessToken);
            }
            if (refreshIssued) {
                refreshBucketByDigest(refreshDigest).delete();
            }
            throw exception;
        }
    }

    /**
     * Refresh Token实行单次轮换：同一旧令牌的并发刷新只有一个请求能够成功。
     * 先成功签发新令牌，再注销旧令牌，避免签发异常导致用户立即丢失可用会话。
     */
    public LoginResponse refresh(String refreshTokenValue) {
        String refreshToken = normalizeRefreshToken(refreshTokenValue);
        RLock lock = redissonClient.getLock(
                REFRESH_LOCK_PREFIX + DigestUtil.sha256Hex(refreshToken));
        boolean acquired = false;
        try {
            // 不指定固定租期，交给Redisson看门狗续期，避免慢查询期间锁提前释放。
            acquired = lock.tryLock(REFRESH_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Refresh Token正在刷新，请稍后重试");
            }
            return refreshLocked(refreshToken);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "令牌刷新被中断，请重新登录");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 注销当前Access Token及其对应的Refresh Token。 */
    public void logoutCurrent() {
        String accessToken = StpUtil.getTokenValue();
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        StpLogic accessTokenLogic = StpUtil.getStpLogic();
        SaSession accessSession = accessTokenLogic.getTokenSessionByToken(accessToken, false);
        String refreshDigest = accessSession == null
                ? null : stringValue(accessSession.get(LINKED_REFRESH_DIGEST));
        Object loginId = accessTokenLogic.getLoginIdByToken(accessToken);
        StpUtil.logoutByTokenValue(accessToken);
        if (refreshDigest != null) {
            refreshBucketByDigest(refreshDigest).delete();
        }
        log.info("User logout, userId={}", loginId);
    }

    private LoginResponse refreshLocked(String refreshToken) {
        String session = refreshBucket(refreshToken).get();
        int separator = session == null ? -1 : session.indexOf('|');
        Long userId = separator <= 0 ? null : longValue(session.substring(0, separator));
        String oldAccessToken = separator < 0 ? null : stringValue(session.substring(separator + 1));
        if (userId == null || oldAccessToken == null) {
            refreshBucket(refreshToken).delete();
            throw invalidRefreshToken();
        }

        AuthAccount account = authAccountMapper.selectById(userId);
        if (account == null
                || !Objects.equals(AccountStatus.ENABLED.getValue(), account.getStatus())) {
            revokePair(oldAccessToken, refreshToken);
            throw new BusinessException(ResultCode.FORBIDDEN, "账号不存在或已禁用");
        }

        LoginResponse newTokens = issue(userId);
        try {
            revokePair(oldAccessToken, refreshToken);
        } catch (RuntimeException exception) {
            revokePair(newTokens.getTokenValue(), newTokens.getRefreshToken());
            throw exception;
        }
        log.info("User token refreshed, userId={}", userId);
        return newTokens;
    }

    private void revokePair(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            StpUtil.logoutByTokenValue(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshBucket(refreshToken).delete();
        }
    }

    private RBucket<String> refreshBucket(String refreshToken) {
        return refreshBucketByDigest(refreshDigest(refreshToken));
    }

    private RBucket<String> refreshBucketByDigest(String refreshDigest) {
        return redissonClient.getBucket(REFRESH_TOKEN_PREFIX + refreshDigest);
    }

    private String refreshDigest(String refreshToken) {
        return DigestUtil.sha256Hex(refreshToken);
    }

    private String sessionValue(Long userId, String accessToken) {
        return userId + "|" + accessToken;
    }

    private String normalizeRefreshToken(String value) {
        if (value == null || value.isBlank()) {
            throw invalidRefreshToken();
        }
        return value.trim();
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token无效或已过期");
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
