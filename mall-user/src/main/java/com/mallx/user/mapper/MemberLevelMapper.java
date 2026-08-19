package com.mallx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.user.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

/** 会员等级配置数据访问层。 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
