package com.mallx.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.user.dto.UserAddressRequest;
import com.mallx.user.entity.UserAddress;
import com.mallx.user.vo.UserAddressVO;

import java.util.List;

/** 当前用户收货地址服务。 */
public interface UserAddressService extends IService<UserAddress> {

    /**
     * 查询用户的全部有效地址。
     *
     * @param userId 用户ID
     * @return 默认地址优先的地址列表
     */
    List<UserAddressVO> listAddresses(Long userId);

    /**
     * 新增地址；第一条地址会自动成为默认地址。
     *
     * @param userId 用户ID
     * @param request 地址信息
     * @return 新增后的地址
     */
    UserAddressVO createAddress(Long userId, UserAddressRequest request);

    /**
     * 更新属于该用户的地址。
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     * @param request 地址信息
     * @return 更新后的地址
     */
    UserAddressVO updateAddress(Long userId, Long addressId, UserAddressRequest request);

    /**
     * 逻辑删除属于该用户的地址；删除默认地址时自动选择替代地址。
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 切换用户的默认地址。
     *
     * @param userId 用户ID
     * @param addressId 地址ID
     */
    void setDefaultAddress(Long userId, Long addressId);
}
