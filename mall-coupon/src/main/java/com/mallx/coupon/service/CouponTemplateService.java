package com.mallx.coupon.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.coupon.dto.CouponTemplateRequest;
import com.mallx.coupon.entity.CouponTemplate;
import com.mallx.coupon.vo.CouponTemplateVO;

import java.time.LocalDateTime;
import java.util.List;

/** 优惠券模板配置与发放服务。 */
public interface CouponTemplateService extends IService<CouponTemplate> {
    CouponTemplateVO createTemplate(CouponTemplateRequest request);
    CouponTemplateVO updateTemplate(Long templateId, CouponTemplateRequest request);
    CouponTemplateVO publishTemplate(Long templateId);
    CouponTemplateVO stopTemplate(Long templateId);
    CouponTemplateVO getTemplate(Long templateId);
    List<CouponTemplateVO> listTemplates(Integer status);
    List<CouponTemplateVO> listClaimableTemplates();
    CouponTemplate requireTemplate(Long templateId);
    /** 原子占用一份模板发行量。 */
    void occupyStock(Long templateId, LocalDateTime now);
    /** 分批把领取期已结束的发放中模板收敛为已结束状态。 */
    int endExpiredTemplates(int batchSize);
}
