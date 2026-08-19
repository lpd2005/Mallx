package com.mallx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.user.entity.UserMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/** 用户会员数据访问层。 */
@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {

    /** 锁定单个用户的会员记录，串行处理开通、续期和取消。 */
    @Select("SELECT * FROM user_membership WHERE user_id = #{userId} FOR UPDATE")
    UserMembership selectByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 条件处理已到期的VIP：有普通会员后备身份时恢复普通会员，否则标记为已过期。
     * 查询请求和定时任务并发执行时，条件字段可以保证只有一方写入到期日志。
     */
    int expireVipIfNecessary(@Param("userId") Long userId, @Param("now") LocalDateTime now);

}
