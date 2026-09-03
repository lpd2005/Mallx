package com.mallx.admin.support;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;

import java.util.List;

/** 关联ID全量替换前的校验和去重。 */
public final class AdminIds {
    private AdminIds() {}

    public static List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException(ResultCode.BAD_REQUEST, "关联ID参数无效");
        return ids.stream().distinct().sorted().toList();
    }
}
