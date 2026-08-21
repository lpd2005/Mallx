package com.mallx.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.product.dto.CategoryRequest;
import com.mallx.product.entity.ProductCategory;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.mapper.ProductCategoryMapper;
import com.mallx.product.mapper.ProductSpuMapper;
import com.mallx.product.service.ProductCategoryService;
import com.mallx.product.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商品分类业务实现。 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    private static final int ENABLED = 1;
    private static final int MAX_LEVEL = 3;
    private final ProductSpuMapper productSpuMapper;

    /** 查询并在内存中组装分类树。 */
    //TODO 算法
    @Override
    public List<CategoryVO> categoryTree() {
        List<ProductCategory> categories = lambdaQuery()
                .eq(ProductCategory::getEnabled, ENABLED)
                .orderByAsc(ProductCategory::getCategoryLevel)
                .orderByAsc(ProductCategory::getSort)
                .list();
        Map<Long, CategoryVO> nodes = new LinkedHashMap<>();
        categories.forEach(category -> nodes.put(category.getId(), toVO(category)));
        List<CategoryVO> roots = new ArrayList<>();
        categories.forEach(category -> {
            CategoryVO node = nodes.get(category.getId());
            CategoryVO parent = nodes.get(category.getParentId());
            if (category.getParentId() == 0 || parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        });
        return roots;
    }

    /** 创建分类并根据父节点计算层级。 */
    @Override
    @Transactional
    public ProductCategory createCategory(CategoryRequest request) {
        int level = resolveLevel(request.getParentId());
        ensureUnique(null, request.getParentId(), request.getCategoryName());
        ProductCategory category = new ProductCategory();
        BeanUtil.copyProperties(request, category);
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryLevel(level);
        validateEnabled(category.getEnabled());
        save(category);
        return category;
    }

    /** 更新分类，同时保护已有子树的层级结构。 */
    @Override
    @Transactional
    public ProductCategory updateCategory(Long categoryId, CategoryRequest request) {
        ProductCategory category = requireCategory(categoryId);
        if (categoryId.equals(request.getParentId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类不能以自身作为父分类");
        }
        if (!category.getParentId().equals(request.getParentId()) && lambdaQuery()
                .eq(ProductCategory::getParentId, categoryId).count() > 0) {
            // 移动非叶子分类需要同步整棵子树层级，当前阶段禁止该操作，避免形成错误层级。
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子分类时不能更换父分类");
        }
        int level = resolveLevel(request.getParentId());
        ensureUnique(categoryId, request.getParentId(), request.getCategoryName());
        BeanUtil.copyProperties(request, category);
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryLevel(level);
        validateEnabled(category.getEnabled());
        updateById(category);
        return category;
    }

    /** 物理删除分类，删除前检查树形结构和商品引用。 */
    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        requireCategory(categoryId);
        if (lambdaQuery().eq(ProductCategory::getParentId, categoryId).count() > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子分类，不能删除当前分类");
        }
        Long spuCount = productSpuMapper.selectCount(Wrappers.<ProductSpu>lambdaQuery()
                .eq(ProductSpu::getCategoryId, categoryId));
        if (spuCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类已关联商品，不能删除");
        }
        if (!removeById(categoryId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品分类不存在");
        }
    }

    /** 获取启用分类，供SPU创建和更新时校验。 */
    @Override
    public ProductCategory requireEnabledCategory(Long categoryId) {
        ProductCategory category = requireCategory(categoryId);
        if (!Integer.valueOf(ENABLED).equals(category.getEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品分类未启用");
        }
        return category;
    }

    private ProductCategory requireCategory(Long id) {
        ProductCategory category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品分类不存在");
        }
        return category;
    }

    private int resolveLevel(Long parentId) {
        if (parentId == null || parentId == 0) {
            return 1;
        }
        ProductCategory parent = requireEnabledCategory(parentId);
        int level = parent.getCategoryLevel() + 1;
        if (level > MAX_LEVEL) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品分类最多支持三级");
        }
        return level;
    }

    private void ensureUnique(Long excludedId, Long parentId, String name) {
        long count = lambdaQuery()
                .eq(ProductCategory::getParentId, parentId)
                .eq(ProductCategory::getCategoryName, name.trim())
                .ne(excludedId != null, ProductCategory::getId, excludedId)
                .count();
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "同级分类名称已存在");
        }
    }

    private void validateEnabled(Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "启用状态不正确");
        }
    }

    private CategoryVO toVO(ProductCategory category) {
        return CategoryVO.builder().
                id(category.getId()).
                parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryLevel(category.getCategoryLevel())
                .sort(category.getSort())
                .iconOssKey(category.getIconOssKey())
                .children(new ArrayList<>()).build();
    }
}
