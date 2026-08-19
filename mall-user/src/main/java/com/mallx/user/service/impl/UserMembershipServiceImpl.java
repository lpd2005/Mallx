package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.user.entity.MemberLevel;
import com.mallx.user.entity.UserMembership;
import com.mallx.user.entity.UserMembershipLog;
import com.mallx.user.enums.MemberLevelCode;
import com.mallx.user.enums.MembershipChangeType;
import com.mallx.user.enums.MembershipStatus;
import com.mallx.user.mapper.UserMembershipMapper;
import com.mallx.user.service.MemberLevelService;
import com.mallx.user.service.UserMembershipLogService;
import com.mallx.user.service.UserMembershipService;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserMembershipVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/** 用户会员服务实现。 */
@Service
@RequiredArgsConstructor
public class UserMembershipServiceImpl extends ServiceImpl<UserMembershipMapper, UserMembership>
        implements UserMembershipService {

    private static final int NONE_LEVEL_CODE = 0;
    private static final String NONE_LEVEL_NAME = "非会员";
    private static final int NONE_STATUS = 0;
    private static final int MAX_EXPIRE_BATCH_SIZE = 1000;// 到期处理批次大小限制

    /** 用于行锁和条件到期更新等无法由通用Service方法表达的SQL。 */
    private final UserMembershipMapper userMembershipMapper;
    private final MemberLevelService memberLevelService;
    private final UserMembershipLogService membershipLogService;
    /** 以用户资料行作为同一用户会员变更的稳定锁，即使会员记录尚不存在也能串行处理。 */
    private final UserProfileService userProfileService;

    /**
     * 查询时实时校验VIP到期时间，因此即使定时任务尚未执行，过期用户也不会继续享受8折。
     */
    @Override
    @Transactional
    public UserMembershipVO getCurrentMembership(Long userId) {
        requireUserId(userId);
        UserMembership membership = getById(userId);
        if (membership == null || !MembershipStatus.ACTIVE.matches(membership.getStatus())) {
            return nonMember(userId);
        }

        MemberLevelCode levelCode = parseLevelCode(membership.getLevelCode());
        if (MemberLevelCode.VIP == levelCode) {
            LocalDateTime now = LocalDateTime.now();
            // VIP缺少到期时间属于不合法数据。权益计算采取“失败关闭”，避免错误发放8折权益。
            if (membership.getExpireAt() == null) {
                return nonMember(userId);
            }
            if (!membership.getExpireAt().isAfter(now)) {
                expireVip(membership, now);
                // 到期SQL可能恢复了普通会员，也可能把直接开通的VIP标记为已过期，因此重新读取最终状态。
                UserMembership membershipAfterExpire = getById(userId);
                if (isActiveLevel(membershipAfterExpire, MemberLevelCode.NORMAL)) {
                    MemberLevel normalLevel = memberLevelService.requireLevel(MemberLevelCode.NORMAL, false);
                    return memberVO(membershipAfterExpire, normalLevel);
                }
                return nonMember(userId);
            }
        }

        MemberLevel level = memberLevelService.requireLevel(levelCode, false);
        return memberVO(membership, level);
    }

    /** 开通普通会员时清空到期时间，普通会员默认永久有效。 */
    @Override
    @Transactional
    public UserMembershipVO activateNormal(Long userId, Long operatorId, String remark) {
        requireUserId(userId);
        MemberLevel level = memberLevelService.requireLevel(MemberLevelCode.NORMAL, true);
        UserMembership membership = lockMembership(userId);

        if (isActiveLevel(membership, MemberLevelCode.NORMAL)) {
            return memberVO(membership, level);
        }

        MembershipChangeType changeType = membership == null
                || !MembershipStatus.ACTIVE.matches(membership.getStatus())
                ? MembershipChangeType.OPEN : MembershipChangeType.CHANGE;
        Integer beforeLevelCode = activeLevelCode(membership);
        LocalDateTime beforeExpireAt = activeExpireAt(membership);
        LocalDateTime now = LocalDateTime.now();
        boolean insert = membership == null;

        if (insert) {
            membership = new UserMembership();
            membership.setUserId(userId);
        }
        membership.setLevelCode(MemberLevelCode.NORMAL.getValue());
        membership.setStartAt(now);
        membership.setExpireAt(null);
        membership.setStatus(MembershipStatus.ACTIVE.getValue());
        // 显式调整为普通会员后，已经不再需要VIP的后备身份快照。
        membership.setFallbackLevelCode(null);
        membership.setFallbackStartAt(null);
        persistMembership(membership, insert);
        saveLog(userId, beforeLevelCode, MemberLevelCode.NORMAL.getValue(), changeType,
                beforeExpireAt, null, operatorId, remark);
        return memberVO(membership, level);
    }

    /**
     * 首次开通VIP、从普通会员升级以及延长VIP期限都走同一个原子变更入口。
     */
    @Override
    @Transactional
    public UserMembershipVO activateVip(Long userId, LocalDateTime expireAt, Long operatorId, String remark) {
        requireUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        // 到期时间必须晚于当前时间
        if (expireAt == null || !expireAt.isAfter(now)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "VIP到期时间必须晚于当前时间");
        }
        MemberLevel level = memberLevelService.requireLevel(MemberLevelCode.VIP, true);
        UserMembership membership = lockMembership(userId);

        // 如果当前是VIP，且到期时间不变，则直接返回当前会员信息
        if (isActiveLevel(membership, MemberLevelCode.VIP) && expireAt.equals(membership.getExpireAt())) {
            return memberVO(membership, level);
        }
        // 准备变更前快照
        Integer beforeLevelCode = activeLevelCode(membership);
        LocalDateTime beforeExpireAt = activeExpireAt(membership);
        MembershipChangeType changeType = resolveVipChangeType(membership);
        boolean insert = membership == null;
        if (insert) {
            membership = new UserMembership();
            membership.setUserId(userId);
        }
        if (MemberLevelCode.NORMAL.matches(beforeLevelCode)) {
            // 普通会员升级VIP时保留基础会员信息，VIP到期后据此恢复95折权益。
            membership.setFallbackLevelCode(MemberLevelCode.NORMAL.getValue());
            membership.setFallbackStartAt(membership.getStartAt());
        } else if (changeType != MembershipChangeType.RENEW) {
            // 非会员直接开通VIP没有后备等级；重新开通时也要清理历史残留值。
            membership.setFallbackLevelCode(null);
            membership.setFallbackStartAt(null);
        }
        membership.setLevelCode(MemberLevelCode.VIP.getValue());
        membership.setStartAt(changeType == MembershipChangeType.RENEW
                ? membership.getStartAt() : now);
        membership.setExpireAt(expireAt);
        membership.setStatus(MembershipStatus.ACTIVE.getValue());
        persistMembership(membership, insert);
        saveLog(userId, beforeLevelCode, MemberLevelCode.VIP.getValue(), changeType,
                beforeExpireAt, expireAt, operatorId, remark);
        return memberVO(membership, level);
    }

    /** 取消操作保持幂等，不存在有效会员时直接返回。 */
    @Override
    @Transactional
    public void cancelMembership(Long userId, Long operatorId, String remark) {
        requireUserId(userId);
        UserMembership membership = lockMembership(userId);
        if (membership == null || !MembershipStatus.ACTIVE.matches(membership.getStatus())) {
            return;
        }
        Integer beforeLevelCode = membership.getLevelCode();
        LocalDateTime beforeExpireAt = membership.getExpireAt();
        membership.setStatus(MembershipStatus.CANCELED.getValue());
        // cancelMembership表示取消全部会员权益，而不是只结束VIP覆盖层。
        membership.setFallbackLevelCode(null);
        membership.setFallbackStartAt(null);
        if (!updateById(membership)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会员状态更新失败");
        }
        saveLog(userId, beforeLevelCode, null, MembershipChangeType.CANCEL,
                beforeExpireAt, null, operatorId, remark);
    }

    /** 定时任务只做状态落库；真正的权益查询不依赖任务是否准时执行。 */
    @Override
    @Transactional
    public int expireVipMemberships(int batchSize) {
        if (batchSize <= 0 || batchSize > MAX_EXPIRE_BATCH_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "到期处理批次大小必须在1到1000之间");
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserMembership> expiredMemberships = lambdaQuery()
                .eq(UserMembership::getLevelCode, MemberLevelCode.VIP.getValue())
                .eq(UserMembership::getStatus, MembershipStatus.ACTIVE.getValue())
                .isNotNull(UserMembership::getExpireAt)
                .le(UserMembership::getExpireAt, now)
                .orderByAsc(UserMembership::getExpireAt)
                // batchSize已经限制为1到1000之间，可以安全用于固定LIMIT片段。
                .last("LIMIT " + batchSize)
                .list();
        int expiredCount = 0;
        for (UserMembership membership : expiredMemberships) {
            if (expireVip(membership, now)) {
                expiredCount++;
            }
        }
        return expiredCount;
    }

    /** 锁资料行后再查会员行，解决“会员行尚不存在”时无法加行锁的问题。 */
    private UserMembership lockMembership(Long userId) {
        userProfileService.lockProfile(userId);
        return userMembershipMapper.selectByUserIdForUpdate(userId);
    }

    /** 使用条件SQL处理VIP到期；有普通会员后备身份时恢复普通会员，否则成为非会员。 */
    private boolean expireVip(UserMembership membership, LocalDateTime now) {
        int updated = userMembershipMapper.expireVipIfNecessary(membership.getUserId(), now);
        if (updated == 0) {
            return false;
        }
        Integer fallbackLevelCode = MemberLevelCode.NORMAL.matches(membership.getFallbackLevelCode())
                ? MemberLevelCode.NORMAL.getValue() : null;
        saveLog(membership.getUserId(), membership.getLevelCode(), fallbackLevelCode, MembershipChangeType.EXPIRE,
                membership.getExpireAt(), null, null, "VIP到期自动失效");
        return true;
    }

    /** 新记录使用save，已有记录使用updateById，不直接调用baseMapper。 */
    private void persistMembership(UserMembership membership, boolean insert) {
        boolean success = insert ? save(membership) : updateById(membership);
        if (!success) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会员状态保存失败");
        }
    }

    /** 解析VIP变更类型：无有效会员为新开，原VIP为续期，普通会员升VIP为升级。 */
    private MembershipChangeType resolveVipChangeType(UserMembership membership) {
        if (membership == null || !MembershipStatus.ACTIVE.matches(membership.getStatus())) {
            return MembershipChangeType.OPEN;
        }
        return MemberLevelCode.VIP.matches(membership.getLevelCode())
                ? MembershipChangeType.RENEW : MembershipChangeType.UPGRADE;
    }

    /** 判断会员记录是否存在且处于生效状态，且等级与目标等级一致。 */
    private boolean isActiveLevel(UserMembership membership, MemberLevelCode levelCode) {
        return membership != null
                && MembershipStatus.ACTIVE.matches(membership.getStatus())
                && levelCode.matches(membership.getLevelCode());
    }

    /** 取当前生效会员的等级代码，用于变更前快照；无生效会员时返回null。 */
    private Integer activeLevelCode(UserMembership membership) {
        return membership != null && MembershipStatus.ACTIVE.matches(membership.getStatus())
                ? membership.getLevelCode() : null;
    }

    /** 取当前生效会员的到期时间，用于变更前快照；无生效会员时返回null。 */
    private LocalDateTime activeExpireAt(UserMembership membership) {
        return membership != null && MembershipStatus.ACTIVE.matches(membership.getStatus())
                ? membership.getExpireAt() : null;
    }

    /** 写入会员变更前后快照，订单和后台排查可以据此还原变更过程。 */
    private void saveLog(Long userId, Integer beforeLevelCode, Integer afterLevelCode,
                         MembershipChangeType changeType, LocalDateTime beforeExpireAt,
                         LocalDateTime afterExpireAt, Long operatorId, String remark) {
        UserMembershipLog log = new UserMembershipLog();
        log.setUserId(userId);
        log.setBeforeLevelCode(beforeLevelCode);
        log.setAfterLevelCode(afterLevelCode);
        log.setChangeType(changeType.getValue());
        log.setBeforeExpireAt(beforeExpireAt);
        log.setAfterExpireAt(afterExpireAt);
        log.setOperatorId(operatorId);
        log.setRemark(normalizeRemark(remark));
        if (!membershipLogService.save(log)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会员变更日志保存失败");
        }
    }

    /** 组装有效会员的视图对象，等级信息与折扣率来自等级配置。 */
    private UserMembershipVO memberVO(UserMembership membership, MemberLevel level) {
        return UserMembershipVO.builder()
                .userId(membership.getUserId())
                .member(true)
                .levelCode(level.getLevelCode())
                .levelName(level.getLevelName())
                .discountRate(level.getDiscountRate())
                .startAt(membership.getStartAt())
                .expireAt(membership.getExpireAt())
                .status(MembershipStatus.ACTIVE.getValue())
                .build();
    }

    /** 组装非会员视图：折扣率1.00（原价），无等级无权益。 */
    private UserMembershipVO nonMember(Long userId) {
        return UserMembershipVO.builder()
                .userId(userId)
                .member(false)
                .levelCode(NONE_LEVEL_CODE)
                .levelName(NONE_LEVEL_NAME)
                .discountRate(BigDecimal.ONE.setScale(2,RoundingMode.HALF_UP))
                .status(NONE_STATUS)
                .build();
    }

    /** 数据库等级值转枚举；非法值视为脏数据，抛出内部错误。 */
    private MemberLevelCode parseLevelCode(Integer levelCode) {
        MemberLevelCode result = MemberLevelCode.fromValue(levelCode);
        if (result == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "用户会员等级数据不正确");
        }
        return result;
    }

    /** 用户ID参数校验：为空或非正数时抛出请求参数错误。 */
    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不正确");
        }
    }

    /** 备注归一化：去首尾空白、空串转为null、超长截断到255字符。 */
    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String normalized = remark.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }
}
