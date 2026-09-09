package com.mallx.seckill.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mallx.common.result.Result;
import com.mallx.seckill.dto.SeckillActivityRequest;
import com.mallx.seckill.dto.SeckillItemRequest;
import com.mallx.seckill.service.SeckillActivityService;
import com.mallx.seckill.vo.SeckillActivityVO;
import com.mallx.seckill.vo.SeckillItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 秒杀活动后台管理接口；所有操作统一要求 {@code seckill:manage} 权限。 */
@RestController
@RequestMapping("/admin/seckill-activities")
@RequiredArgsConstructor
@SaCheckPermission("seckill:manage")
public class AdminSeckillController {
    private final SeckillActivityService activityService;

    /** 创建草稿活动；草稿尚不会写入 Redis。 */
    @PostMapping
    public Result<SeckillActivityVO> create(@Valid @RequestBody SeckillActivityRequest request) {
        return Result.success(activityService.createActivity(request));
    }

    /** 修改草稿活动的名称和时间窗口。 */
    @PutMapping("/{activityId}")
    public Result<SeckillActivityVO> update(
            @PathVariable Long activityId, @Valid @RequestBody SeckillActivityRequest request) {
        return Result.success(activityService.updateActivity(activityId, request));
    }

    /** 向草稿活动加入一个已上架 SKU。 */
    @PostMapping("/{activityId}/items")
    public Result<SeckillItemVO> addItem(
            @PathVariable Long activityId, @Valid @RequestBody SeckillItemRequest request) {
        return Result.success(activityService.addItem(activityId, request));
    }

    /** 修改草稿活动中的 SKU、价格、资格库存或限购数。 */
    @PutMapping("/{activityId}/items/{itemId}")
    public Result<SeckillItemVO> updateItem(@PathVariable Long activityId, @PathVariable Long itemId,
                                             @Valid @RequestBody SeckillItemRequest request) {
        return Result.success(activityService.updateItem(activityId, itemId, request));
    }

    /** 删除草稿活动中的秒杀商品。 */
    @DeleteMapping("/{activityId}/items/{itemId}")
    public Result<Void> removeItem(@PathVariable Long activityId, @PathVariable Long itemId) {
        activityService.removeItem(activityId, itemId);
        return Result.success();
    }

    /** 校验商品和真实库存后，把活动数据预热到 Redis 并开放抢购。 */
    @PostMapping("/{activityId}/prepare")
    public Result<SeckillActivityVO> prepare(@PathVariable Long activityId) {
        return Result.success(activityService.prepareActivity(activityId));
    }

    /** 关闭活动并先更新数据库状态，再禁止 Redis 中的秒杀商品继续抢购。 */
    @PostMapping("/{activityId}/close")
    public Result<Void> close(@PathVariable Long activityId) {
        activityService.closeActivity(activityId);
        return Result.success();
    }

    /** 按可选状态查询后台活动列表。 */
    @GetMapping
    public Result<List<SeckillActivityVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(activityService.listActivities(status));
    }

    /** 查询活动及其全部秒杀商品配置。 */
    @GetMapping("/{activityId}")
    public Result<SeckillActivityVO> detail(@PathVariable Long activityId) {
        return Result.success(activityService.getActivity(activityId));
    }
}
