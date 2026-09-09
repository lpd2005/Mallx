package com.mallx.seckill.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.seckill.dto.SeckillActivityRequest;
import com.mallx.seckill.dto.SeckillItemRequest;
import com.mallx.seckill.entity.SeckillActivity;
import com.mallx.seckill.vo.SeckillActivityVO;
import com.mallx.seckill.vo.SeckillItemVO;

import java.util.List;

/** 秒杀活动及活动商品的生命周期管理服务。 */
public interface SeckillActivityService extends IService<SeckillActivity> {
    /** 创建尚未开放抢购的草稿活动。 */
    SeckillActivityVO createActivity(SeckillActivityRequest request);

    /** 修改草稿活动。 */
    SeckillActivityVO updateActivity(Long activityId, SeckillActivityRequest request);

    /** 为草稿活动添加秒杀商品。 */
    SeckillItemVO addItem(Long activityId, SeckillItemRequest request);

    /** 修改草稿活动中的秒杀商品。 */
    SeckillItemVO updateItem(Long activityId, Long itemId, SeckillItemRequest request);

    /** 从草稿活动中移除秒杀商品。 */
    void removeItem(Long activityId, Long itemId);

    /** 校验并预热活动，使其可以在时间窗口内参与抢购。 */
    SeckillActivityVO prepareActivity(Long activityId);

    /** 关闭活动并禁止新的资格预扣。 */
    void closeActivity(Long activityId);

    /** 自动关闭已经到达结束时间的预热活动。 */
    int closeExpiredActivities();

    /** 定时释放已关闭活动中不再存在待处理请求的剩余预留库存。 */
    int releaseClosedReservations();

    /** 后台按状态查询全部活动。 */
    List<SeckillActivityVO> listActivities(Integer status);

    /** 前台查询已预热且未结束的活动。 */
    List<SeckillActivityVO> listAvailableActivities();

    /** 后台查询活动详情。 */
    SeckillActivityVO getActivity(Long activityId);
}
