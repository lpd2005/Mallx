package com.mallx.admin.support;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.PageResult;

/** 分页边界与VO拷贝复用，不引入converter层。 */
public final class AdminPages {
    private AdminPages() {}

    public static <T> Page<T> page(AdminPageQuery query) {
        if (query == null || query.getCurrent() < 1 || query.getSize() < 1 || query.getSize() > 100)
            throw new BusinessException(ResultCode.BAD_REQUEST, "页码必须大于0，每页最多100条");
        return new Page<>(query.getCurrent(), query.getSize());
    }

    public static <T, V> PageResult<V> result(IPage<T> page, Class<V> type) {
        return new PageResult<>(
                BeanUtil.copyToList(page.getRecords(), type),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }
}
