package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.user.dto.UserAddressRequest;
import com.mallx.user.entity.UserAddress;
import com.mallx.user.mapper.UserAddressMapper;
import com.mallx.user.service.UserAddressService;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 用户收货地址服务实现。 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress>
        implements UserAddressService {

    /** 单个用户允许保存的有效地址上限。 */
    private static final int MAX_ADDRESS_COUNT = 20;
    /** 非默认地址数据库值。 */
    private static final int NOT_DEFAULT = 0;
    /** 默认地址数据库值。 */
    private static final int DEFAULT = 1;
    /** 有效数据逻辑删除值。 */
    private static final int NOT_DELETED = 0;
    /** 已删除数据逻辑删除值。 */
    private static final int DELETED = 1;

    /** 用户资料服务，同时提供按用户串行化的行锁。 */
    private final UserProfileService userProfileService;

    /** 按“默认优先、最近更新优先”的顺序返回地址。 */
    @Override
    public List<UserAddressVO> listAddresses(Long userId) {
        return lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdatedAt)
                .list()
                .stream().map(this::toAddressVO).toList();
    }

    /**
     * 新增地址与默认地址切换必须在一个事务中完成。
     */
    @Override
    @Transactional
    public UserAddressVO createAddress(Long userId, UserAddressRequest request) {
        // 锁定用户资料行，使同一用户的新增、删除和默认地址切换串行执行。
        userProfileService.lockProfile(userId);
        long count = lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .count();
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "每位用户最多保存 20 个收货地址");
        }

        // 第一条地址强制设为默认；之后仅在请求明确指定时切换默认地址。
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.getDefaultAddress());
        if (makeDefault && count > 0) {
            clearDefaultAddress(userId);
        }
        UserAddress address = buildAddress(userId, request);
        address.setIsDefault(makeDefault ? DEFAULT : NOT_DEFAULT);
        address.setDeleted(NOT_DELETED);
        save(address);
        return toAddressVO(requireOwnedAddress(userId, address.getId()));
    }

    /**
     * 更新地址时使用 userId + addressId 查询，防止修改其他用户的地址。
     */
    @Override
    @Transactional
    public UserAddressVO updateAddress(Long userId, Long addressId, UserAddressRequest request) {
        userProfileService.lockProfile(userId);
        UserAddress address = requireOwnedAddress(userId, addressId);
        // defaultAddress=false 表示“不切换”，避免把唯一默认地址直接取消后无人接替。
        boolean makeDefault = Boolean.TRUE.equals(request.getDefaultAddress());
        if (makeDefault) {
            clearDefaultAddress(userId);
            address.setIsDefault(DEFAULT);
        }
        copyEditableFields(request, address);
        updateById(address);
        return toAddressVO(requireOwnedAddress(userId, addressId));
    }

    /**
     * 逻辑删除地址；如果删除的是默认地址，则在同一事务中选出替代地址。
     */
    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        userProfileService.lockProfile(userId);
        UserAddress address = requireOwnedAddress(userId, addressId);
        boolean updated = lambdaUpdate()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .set(UserAddress::getDeleted, DELETED)
                .set(UserAddress::getIsDefault, NOT_DEFAULT)
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        if (Integer.valueOf(DEFAULT).equals(address.getIsDefault())) {
            // 选择最近更新的有效地址作为新的默认地址，保证仍有地址时默认值不缺失。
            UserAddress replacement = lambdaQuery()
                    .eq(UserAddress::getUserId, userId)
                    .eq(UserAddress::getDeleted, NOT_DELETED)
                    .orderByDesc(UserAddress::getUpdatedAt)
                    .last("LIMIT 1")
                    .one();
            if (replacement != null) {
                markDefault(replacement.getId(), userId);
            }
        }
    }

    /** 清除原默认值和设置新默认值处于同一个数据库事务中。 */
    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        userProfileService.lockProfile(userId);
        requireOwnedAddress(userId, addressId);
        clearDefaultAddress(userId);
        if (!markDefault(addressId, userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
    }

    /**
     * 查询用户拥有的指定有效地址，不存在或不属于该用户时统一返回“地址不存在”。
     * 这种响应不会泄露其他用户是否拥有该地址。
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     * @return 地址实体
     */
    private UserAddress requireOwnedAddress(Long userId, Long addressId) {
        UserAddress address = lambdaQuery()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .one();
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    /**
     * 清除用户当前有效地址中的默认标记。
     *
     * @param userId 用户ID
     */
    private void clearDefaultAddress(Long userId) {
        lambdaUpdate()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .eq(UserAddress::getIsDefault, DEFAULT)
                .set(UserAddress::getIsDefault, NOT_DEFAULT)
                .update();
    }

    /**
     * 将属于用户的指定有效地址设为默认地址。
     *
     * @param addressId 地址ID
     * @param userId 用户ID
     * @return 更新成功时返回 {@code true}
     */
    private boolean markDefault(Long addressId, Long userId) {
        return lambdaUpdate()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDeleted, NOT_DELETED)
                .set(UserAddress::getIsDefault, DEFAULT)
                .update();
    }

    /**
     * 根据当前用户和请求创建尚未持久化的地址实体。
     *
     * @param userId 地址所属用户
     * @param request 地址请求
     * @return 地址实体
     */
    private UserAddress buildAddress(Long userId, UserAddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        copyEditableFields(request, address);
        return address;
    }

    /**
     * 复制允许用户修改的地址字段，不覆盖主键、用户归属和默认状态。
     *
     * @param request 地址请求
     * @param address 目标地址实体
     */
    private void copyEditableFields(UserAddressRequest request, UserAddress address) {
        address.setRecipientName(request.getRecipientName().trim());
        address.setRecipientMobile(request.getRecipientMobile().trim());
        address.setProvinceCode(request.getProvinceCode().trim());
        address.setProvinceName(request.getProvinceName().trim());
        address.setCityCode(request.getCityCode().trim());
        address.setCityName(request.getCityName().trim());
        address.setDistrictCode(request.getDistrictCode().trim());
        address.setDistrictName(request.getDistrictName().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setPostalCode(trimToNull(request.getPostalCode()));
        address.setAddressTag(trimToNull(request.getAddressTag()));
    }

    /**
     * 使用 Builder 将地址实体转换为响应对象，并隐藏用户ID和删除标记。
     *
     * @param address 地址实体
     * @return 地址响应
     */
    private UserAddressVO toAddressVO(UserAddress address) {
        return UserAddressVO.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientMobile(address.getRecipientMobile())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .cityCode(address.getCityCode())
                .cityName(address.getCityName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .detailAddress(address.getDetailAddress())
                .postalCode(address.getPostalCode())
                .addressTag(address.getAddressTag())
                .defaultAddress(Integer.valueOf(DEFAULT).equals(address.getIsDefault()))
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    /**
     * 去除可选字符串首尾空白，并把空字符串转为 {@code null}。
     *
     * @param value 原始字符串
     * @return 清理后的字符串或 {@code null}
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
