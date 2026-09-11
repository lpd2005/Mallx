package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.common.result.Result;
import com.mallx.search.service.impl.ProductSearchFailureService;
import com.mallx.search.service.impl.ProductSearchIndexService;
import com.mallx.search.vo.ProductSearchFailureVO;
import com.mallx.search.vo.ProductSearchIndexStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Elasticsearch 商品索引运维入口，统一由后台模块管理。 */
@RestController
@RequestMapping("/admin/search")
@RequiredArgsConstructor
@SaCheckPermission("search:manage")
public class AdminSearchController {
    private final AdminOperationLogService operationLogService;
    private final ProductSearchIndexService indexService;
    private final ProductSearchFailureService failureService;

    @GetMapping("/index/status")
    public Result<ProductSearchIndexStatusVO> status() {
        return Result.success(indexService.status());
    }

    @PostMapping("/index/rebuild")
    public Result<Long> rebuild() {
        return Result.success(operationLogService.execute(
                "search:rebuild-product-index", "product_index", indexService::rebuild));
    }


    @PostMapping("/index/products/{spuId}/sync")
    public Result<Void> synchronize(@PathVariable Long spuId) {
        operationLogService.execute(
                "search:sync-product", spuId.toString(), () -> indexService.synchronize(spuId));
        return Result.success();
    }

    @GetMapping("/failures")
    public Result<List<ProductSearchFailureVO>> failures() {
        return Result.success(failureService.list());
    }

    @PostMapping("/failures/{eventId}/replay")
    public Result<Void> replay(@PathVariable String eventId) {
        operationLogService.execute(
                "search:replay-failure", eventId, () -> failureService.replay(eventId));
        return Result.success();
    }
}
