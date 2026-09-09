package com.mallx.seckill.controller;

import com.mallx.common.annotation.Public;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.seckill.dto.SeckillPurchaseRequest;
import com.mallx.seckill.service.SeckillActivityService;
import com.mallx.seckill.service.SeckillPurchaseService;
import com.mallx.seckill.vo.SeckillActivityVO;
import com.mallx.seckill.vo.SeckillResultVO;
import com.mallx.seckill.vo.SeckillSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀前台接口。
 *
 * <p>活动列表允许游客访问；抢购和结果查询必须使用当前登录用户身份，避免请求号被他人查询。</p>
 */
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {
    private final SeckillActivityService activityService;
    private final SeckillPurchaseService purchaseService;
    private final CurrentUserProvider currentUserProvider;

    /** 查询已预热且尚未结束的活动，包含即将开始和正在进行的活动。 */
    @Public
    @GetMapping("/activities")
    public Result<List<SeckillActivityVO>> activities() {
        return Result.success(activityService.listAvailableActivities());
    }

    /**
     * 抢占秒杀资格。
     *
     * <p>接口只等待 Redis 原子预扣和首次 MQ 投递，不同步等待数据库订单创建。</p>
     */
    @PostMapping("/items/{itemId}/purchase")
    public Result<SeckillSubmitVO> purchase(
            @PathVariable Long itemId, @Valid @RequestBody SeckillPurchaseRequest request) {
        return Result.success(purchaseService.submit(currentUserProvider.requireUserId(), itemId, request));
    }

    /** 根据提交时返回的请求号查询处理中、成功或失败状态。 */
    @GetMapping("/requests/{requestNo}")
    public Result<SeckillResultVO> result(@PathVariable String requestNo) {
        return Result.success(purchaseService.getResult(currentUserProvider.requireUserId(), requestNo));
    }
}
