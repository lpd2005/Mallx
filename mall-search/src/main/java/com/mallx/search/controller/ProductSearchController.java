package com.mallx.search.controller;

import com.mallx.common.annotation.Public;
import com.mallx.common.result.PageResult;
import com.mallx.common.result.Result;
import com.mallx.search.dto.ProductSearchQuery;
import com.mallx.search.service.ProductSearchService;
import com.mallx.search.vo.ProductSearchVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 游客可用的商品全文检索、联想和热门词接口。 */
@Validated
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class ProductSearchController {
    private final ProductSearchService searchService;

    /** 商品搜索主入口，参数通过对象方式接收查询字符串。 */
    @Public
    @GetMapping("/products")
    public Result<PageResult<ProductSearchVO>> search(@Valid ProductSearchQuery query) {
        return Result.success(searchService.search(query));
    }

    /** 根据商品名或品牌名前缀返回最多 10 个商品名称。 */
    @Public
    @GetMapping("/suggestions")
    public Result<List<String>> suggestions(
            @RequestParam @Size(min = 1, max = 50, message = "联想关键词长度必须为1到50位") String keyword) {
        return Result.success(searchService.suggestions(keyword));
    }

    /** 从 Redis ZSet 中读取按搜索次数倒序排列的热门关键词。 */
    @Public
    @GetMapping("/hot-words")
    public Result<List<String>> hotWords() {
        return Result.success(searchService.hotWords());
    }
}
