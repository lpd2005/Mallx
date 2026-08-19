package com.mallx.user.service.impl;

import com.mallx.user.entity.MemberLevel;
import com.mallx.user.entity.UserMembership;
import com.mallx.user.entity.UserMembershipLog;
import com.mallx.user.enums.MemberLevelCode;
import com.mallx.user.enums.MembershipStatus;
import com.mallx.user.mapper.UserMembershipMapper;
import com.mallx.user.service.MemberLevelService;
import com.mallx.user.service.UserMembershipLogService;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserMembershipVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 会员折扣、到期回退和开通规则单元测试。 */
@ExtendWith(MockitoExtension.class)
class UserMembershipServiceImplTest {

    private static final Long USER_ID = 1001L;

    @Mock
    private UserMembershipMapper userMembershipMapper;
    @Mock
    private MemberLevelService memberLevelService;
    @Mock
    private UserMembershipLogService membershipLogService;
    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserMembershipServiceImpl userMembershipService;

    /** 模拟Spring为MyBatis-Plus ServiceImpl注入继承的Mapper。 */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(userMembershipService, "baseMapper", userMembershipMapper);
    }

    /** 新用户没有会员记录，应返回原价非会员权益。 */
    @Test
    void newUserShouldBeNonMember() {
        when(userMembershipMapper.selectById(USER_ID)).thenReturn(null);

        UserMembershipVO result = userMembershipService.getCurrentMembership(USER_ID);

        assertFalse(result.getMember());
        assertEquals(0, result.getLevelCode());
        assertEquals(new BigDecimal("1.00"), result.getDiscountRate());
    }

    /** 普通会员应读取数据库中的95折配置。 */
    @Test
    void normalMemberShouldReceiveNinetyFivePercentDiscount() {
        UserMembership membership = activeMembership(MemberLevelCode.NORMAL, null);
        MemberLevel level = level(MemberLevelCode.NORMAL, "普通会员", "0.9500");
        when(userMembershipMapper.selectById(USER_ID)).thenReturn(membership);
        when(memberLevelService.requireLevel(MemberLevelCode.NORMAL, false)).thenReturn(level);

        UserMembershipVO result = userMembershipService.getCurrentMembership(USER_ID);

        assertTrue(result.getMember());
        assertEquals(MemberLevelCode.NORMAL.getValue(), result.getLevelCode());
        assertEquals(new BigDecimal("0.9500"), result.getDiscountRate());
    }

    /** 有效VIP应读取数据库中的8折配置。 */
    @Test
    void activeVipShouldReceiveEightyPercentDiscount() {
        UserMembership membership = activeMembership(MemberLevelCode.VIP, LocalDateTime.now().plusDays(30));
        MemberLevel level = level(MemberLevelCode.VIP, "VIP会员", "0.8000");
        when(userMembershipMapper.selectById(USER_ID)).thenReturn(membership);
        when(memberLevelService.requireLevel(MemberLevelCode.VIP, false)).thenReturn(level);

        UserMembershipVO result = userMembershipService.getCurrentMembership(USER_ID);

        assertTrue(result.getMember());
        assertEquals(MemberLevelCode.VIP.getValue(), result.getLevelCode());
        assertEquals(new BigDecimal("0.8000"), result.getDiscountRate());
    }

    /** 非会员直接开通的VIP到期后应回退为非会员。 */
    @Test
    void expiredVipShouldFallBackToNonMember() {
        UserMembership membership = activeMembership(MemberLevelCode.VIP, LocalDateTime.now().minusSeconds(1));
        when(userMembershipMapper.selectById(USER_ID)).thenReturn(membership);
        when(userMembershipMapper.expireVipIfNecessary(any(), any())).thenReturn(1);
        when(membershipLogService.save(any(UserMembershipLog.class))).thenReturn(true);

        UserMembershipVO result = userMembershipService.getCurrentMembership(USER_ID);

        assertFalse(result.getMember());
        assertEquals(0, result.getLevelCode());
        assertEquals(new BigDecimal("1.00"), result.getDiscountRate());
        verify(membershipLogService).save(any(UserMembershipLog.class));
    }

    /** 普通会员升级VIP后，VIP到期应恢复普通会员及95折权益。 */
    @Test
    void expiredVipShouldRestorePreviousNormalMembership() {
        LocalDateTime normalStartAt = LocalDateTime.now().minusMonths(3);
        UserMembership vipMembership = activeMembership(
                MemberLevelCode.VIP, LocalDateTime.now().minusSeconds(1));
        vipMembership.setFallbackLevelCode(MemberLevelCode.NORMAL.getValue());
        vipMembership.setFallbackStartAt(normalStartAt);

        UserMembership restoredNormal = activeMembership(MemberLevelCode.NORMAL, null);
        restoredNormal.setStartAt(normalStartAt);
        MemberLevel normalLevel = level(MemberLevelCode.NORMAL, "普通会员", "0.9500");
        when(userMembershipMapper.selectById(USER_ID)).thenReturn(vipMembership, restoredNormal);
        when(userMembershipMapper.expireVipIfNecessary(any(), any())).thenReturn(1);
        when(memberLevelService.requireLevel(MemberLevelCode.NORMAL, false)).thenReturn(normalLevel);
        when(membershipLogService.save(any(UserMembershipLog.class))).thenReturn(true);

        UserMembershipVO result = userMembershipService.getCurrentMembership(USER_ID);

        assertTrue(result.getMember());
        assertEquals(MemberLevelCode.NORMAL.getValue(), result.getLevelCode());
        assertEquals(new BigDecimal("0.9500"), result.getDiscountRate());
        assertEquals(normalStartAt, result.getStartAt());

        ArgumentCaptor<UserMembershipLog> logCaptor = ArgumentCaptor.forClass(UserMembershipLog.class);
        verify(membershipLogService).save(logCaptor.capture());
        assertEquals(MemberLevelCode.NORMAL.getValue(), logCaptor.getValue().getAfterLevelCode());
    }

    /** 开通普通会员时应新增95折会员记录，而不是给所有新用户自动初始化会员。 */
    @Test
    void activateNormalShouldCreateMembershipExplicitly() {
        MemberLevel level = level(MemberLevelCode.NORMAL, "普通会员", "0.9500");
        when(memberLevelService.requireLevel(MemberLevelCode.NORMAL, true)).thenReturn(level);
        when(userMembershipMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(null);
        when(userMembershipMapper.insert(any(UserMembership.class))).thenReturn(1);
        when(membershipLogService.save(any(UserMembershipLog.class))).thenReturn(true);

        UserMembershipVO result = userMembershipService.activateNormal(USER_ID, 9001L, "开通普通会员");

        assertTrue(result.getMember());
        assertEquals(MemberLevelCode.NORMAL.getValue(), result.getLevelCode());
        assertEquals(new BigDecimal("0.9500"), result.getDiscountRate());
        verify(userProfileService).lockProfile(USER_ID);
        verify(userMembershipMapper).insert(any(UserMembership.class));
    }

    /** 普通会员升级VIP时必须保存后备等级和原生效时间，供VIP到期后恢复。 */
    @Test
    void activateVipFromNormalShouldPreserveNormalMembership() {
        LocalDateTime normalStartAt = LocalDateTime.now().minusMonths(2);
        UserMembership normalMembership = activeMembership(MemberLevelCode.NORMAL, null);
        normalMembership.setStartAt(normalStartAt);
        MemberLevel vipLevel = level(MemberLevelCode.VIP, "VIP会员", "0.8000");
        LocalDateTime vipExpireAt = LocalDateTime.now().plusMonths(1);
        when(memberLevelService.requireLevel(MemberLevelCode.VIP, true)).thenReturn(vipLevel);
        when(userMembershipMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(normalMembership);
        when(userMembershipMapper.updateById(any(UserMembership.class))).thenReturn(1);
        when(membershipLogService.save(any(UserMembershipLog.class))).thenReturn(true);

        UserMembershipVO result = userMembershipService.activateVip(
                USER_ID, vipExpireAt, 9001L, "普通会员升级VIP");

        assertEquals(MemberLevelCode.VIP.getValue(), result.getLevelCode());
        assertEquals(MemberLevelCode.NORMAL.getValue(), normalMembership.getFallbackLevelCode());
        assertEquals(normalStartAt, normalMembership.getFallbackStartAt());
        verify(userMembershipMapper).updateById(normalMembership);
    }

    private UserMembership activeMembership(MemberLevelCode levelCode, LocalDateTime expireAt) {
        UserMembership membership = new UserMembership();
        membership.setUserId(USER_ID);
        membership.setLevelCode(levelCode.getValue());
        membership.setStartAt(LocalDateTime.now().minusDays(1));
        membership.setExpireAt(expireAt);
        membership.setStatus(MembershipStatus.ACTIVE.getValue());
        return membership;
    }

    private MemberLevel level(MemberLevelCode levelCode, String name, String discountRate) {
        MemberLevel level = new MemberLevel();
        level.setLevelCode(levelCode.getValue());
        level.setLevelName(name);
        level.setDiscountRate(new BigDecimal(discountRate));
        level.setEnabled(1);
        return level;
    }
}
