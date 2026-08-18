package com.mallx.user.controller;

import com.mallx.common.result.Result;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.user.dto.UserAddressRequest;
import com.mallx.user.service.UserAddressService;
import com.mallx.user.vo.UserAddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前用户收货地址接口。 */
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    /** 收货地址业务服务。 */
    private final UserAddressService userAddressService;
    /** 当前登录用户身份读取器。 */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 查询当前用户的全部有效地址，默认地址优先返回。
     *
     * @return 当前用户地址列表
     */
    @GetMapping
    public Result<List<UserAddressVO>> listAddresses() {
        return Result.success(userAddressService.listAddresses(currentUserProvider.requireUserId()));
    }

    /**
     * 为当前用户新增收货地址。
     *
     * @param request 新地址信息
     * @return 新增后的地址
     */
    @PostMapping
    public Result<UserAddressVO> createAddress(@Valid @RequestBody UserAddressRequest request) {
        return Result.success(userAddressService.createAddress(currentUserProvider.requireUserId(), request));
    }

    /**
     * 完整更新当前用户拥有的指定地址。
     *
     * @param addressId 地址ID
     * @param request 新的地址信息
     * @return 更新后的地址
     */
    @PutMapping("/{addressId}")
    public Result<UserAddressVO> updateAddress(@PathVariable Long addressId,
                                               @Valid @RequestBody UserAddressRequest request) {
        return Result.success(userAddressService.updateAddress(currentUserProvider.requireUserId(), addressId, request));
    }

    /**
     * 逻辑删除当前用户拥有的指定地址。
     *
     * @param addressId 地址ID
     * @return 空数据成功响应
     */
    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        userAddressService.deleteAddress(currentUserProvider.requireUserId(), addressId);
        return Result.success();
    }

    /**
     * 将指定地址切换为当前用户的默认地址。
     *
     * @param addressId 地址ID
     * @return 空数据成功响应
     */
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long addressId) {
        userAddressService.setDefaultAddress(currentUserProvider.requireUserId(), addressId);
        return Result.success();
    }
}
