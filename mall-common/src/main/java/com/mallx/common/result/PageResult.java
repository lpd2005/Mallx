package com.mallx.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 通用分页数据结构。
 *
 * @param <T> 当前页记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页记录。 */
    private List<T> records;

    /** 符合查询条件的总记录数。 */
    private long total;

    /** 当前页码，从 1 开始。 */
    private long current;

    /** 每页记录数。 */
    private long size;
}
