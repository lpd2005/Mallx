package com.mallx.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.aftersale.entity.AfterSale;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 简短固定行锁SQL放注解，普通CRUD由MyBatis-Plus完成。 */
@Mapper
public interface AfterSaleMapper extends BaseMapper<AfterSale> {
    @Select("SELECT * FROM after_sale WHERE after_sale_no=#{afterSaleNo} FOR UPDATE")
    AfterSale selectByAfterSaleNoForUpdate(@Param("afterSaleNo") String afterSaleNo);
}
