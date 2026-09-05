package com.mallx.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.user.entity.UserMembership;
import com.mallx.user.vo.UserMembershipVO;

import java.time.LocalDateTime;

/** 用户会员服务。 */
public interface UserMembershipService extends IService<UserMembership> {

    /** 查询用户当前实际生效的会员权益。 */
    UserMembershipVO getCurrentMembership(Long userId);

    /** 开通永久普通会员；已有有效VIP时保持VIP，不允许通过该入口降级。 */
    UserMembershipVO activateNormal(Long userId, Long operatorId, String remark);

    /** 开通VIP或更新VIP到期时间。 */
    UserMembershipVO activateVip(Long userId, LocalDateTime expireAt, Long operatorId, String remark);

    /** 按购买月数模拟开通或续费VIP，到期时间只能由服务端计算。 */
    UserMembershipVO activateVipForMonths(Long userId, int durationMonths, Long operatorId, String remark);

    /** 取消当前会员；重复取消保持幂等。 */
    void cancelMembership(Long userId, Long operatorId, String remark);

    /** 分批处理已经到期的VIP，返回实际处理数量。 */
    int expireVipMemberships(int batchSize);
}
