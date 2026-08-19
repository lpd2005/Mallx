package com.mallx.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.user.entity.MemberLevel;
import com.mallx.user.enums.MemberLevelCode;

/** 会员等级配置服务。 */
public interface MemberLevelService extends IService<MemberLevel> {

    /**
     * 获取必须存在的会员配置。
     *
     * @param levelCode 会员编码
     * @param requireEnabled 是否要求配置允许新开通
     * @return 会员等级配置
     */
    MemberLevel requireLevel(MemberLevelCode levelCode, boolean requireEnabled);
}
