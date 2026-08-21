package com.mallx.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.product.dto.CategoryRequest;
import com.mallx.product.entity.ProductCategory;
import com.mallx.product.vo.CategoryVO;

import java.util.List;

/** 商品分类业务服务。 */
public interface ProductCategoryService extends IService<ProductCategory> {
    /** 查询全部启用分类并组装为树。 */
    List<CategoryVO> categoryTree();
    /** 创建分类并计算分类层级。 */
    ProductCategory createCategory(CategoryRequest request);
    /** 更新分类；非叶子分类不能直接移动。 */
    ProductCategory updateCategory(Long categoryId, CategoryRequest request);
    /** 物理删除没有子分类且未关联SPU的分类。 */
    void deleteCategory(Long categoryId);
    /** 获取必须存在且已启用的分类。 */
    ProductCategory requireEnabledCategory(Long categoryId);
}
