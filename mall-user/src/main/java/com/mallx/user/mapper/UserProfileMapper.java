package com.mallx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.user.entity.UserProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户资料数据访问接口。 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 幂等初始化用户资料。主键已存在时忽略插入，避免注册重试产生重复数据。
     *
     * @param userId 用户ID
     * @param nickname 初始昵称
     * @return 实际插入行数，已存在时为 0
     */
    @Insert("INSERT IGNORE INTO user_profile(user_id, nickname, gender) VALUES(#{userId}, #{nickname}, 0)")
    int insertIfAbsent(@Param("userId") Long userId, @Param("nickname") String nickname);

    /**
     * 使用排他锁读取用户资料，用作该用户地址默认值变更的串行化锁。
     *
     * @param userId 用户ID
     * @return 已锁定的用户资料；不存在时返回 {@code null}
     */
    @Select("SELECT * FROM user_profile WHERE user_id = #{userId} FOR UPDATE")
    UserProfile selectByIdForUpdate(@Param("userId") Long userId);
}
