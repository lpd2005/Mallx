package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.user.entity.MemberLevel;
import com.mallx.user.enums.MemberLevelCode;
import com.mallx.user.mapper.MemberLevelMapper;
import com.mallx.user.service.MemberLevelService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 会员等级配置服务实现。 */
@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel>
        implements MemberLevelService {

    private static final int ENABLED = 1;

    @Override
    public MemberLevel requireLevel(MemberLevelCode levelCode, boolean requireEnabled) {
        if (levelCode == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会员等级不能为空");
        }
        MemberLevel level = getById(levelCode.getValue());
        if (level == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会员等级配置不存在");
        }
        if (requireEnabled && !Integer.valueOf(ENABLED).equals(level.getEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该会员等级暂不可开通");
        }
        // 折扣配置属于金额计算输入，读取时再次校验，避免错误配置造成负金额或金额放大。
        if (level.getDiscountRate() == null
                || level.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                || level.getDiscountRate().compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "会员折扣配置不正确");
        }
        return level;
    }
}
