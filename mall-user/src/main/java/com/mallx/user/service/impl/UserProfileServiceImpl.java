package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.user.dto.UpdateUserProfileRequest;
import com.mallx.user.entity.UserProfile;
import com.mallx.user.enums.Gender;
import com.mallx.user.mapper.UserProfileMapper;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户资料服务实现。 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile>
        implements UserProfileService {

    /** 仅用于调用幂等插入、行锁查询等自定义 SQL。 */
    private final UserProfileMapper userProfileMapper;



    /**
     * 查询资料前先幂等补建，兼容认证账号已经存在但资料尚未初始化的情况。
     */
    @Override
    @Transactional
    public UserProfileVO getProfile(Long userId) {
        initializeProfile(userId, defaultNickname(userId));
        return toProfileVO(requireProfile(userId));
    }

    /**
     * 更新前再次校验枚举值，避免绕过 Controller 直接调用 Service 时写入非法数据。
     */
    @Override
    @Transactional
    public UserProfileVO updateProfile(Long userId, UpdateUserProfileRequest request) {
        if (!Gender.isValid(request.getGender())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "性别取值不正确");
        }
        initializeProfile(userId, request.getNickname().trim());
        UserProfile profile = requireProfile(userId);
        // PUT 采用完整替换语义；可选字符串中的空白值统一保存为 null。
        profile.setNickname(request.getNickname().trim());
        profile.setAvatarOssKey(trimToNull(request.getAvatarOssKey()));
        profile.setGender(request.getGender());
        profile.setBirthday(request.getBirthday());
        profile.setBio(trimToNull(request.getBio()));
        updateById(profile);

        return toProfileVO(requireProfile(userId));
    }

    /**
     * 通过 INSERT IGNORE 保证注册重试或懒初始化并发执行时不会产生重复资料。
     */
    @Override
    public void initializeProfile(Long userId, String nickname) {
        if (userId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户ID不能为空");
        }
        String normalizedNickname = trimToNull(nickname);
        if (normalizedNickname == null) {
            normalizedNickname = defaultNickname(userId);
        }
        // 数据库昵称字段最长 32 位；内部调用同样需要保护字段边界。
        if (normalizedNickname.length() > 32) {
            normalizedNickname = normalizedNickname.substring(0, 32);
        }
        userProfileMapper.insertIfAbsent(userId, normalizedNickname);

    }

    /**
     * 以用户资料主键行作为同一用户地址变更的锁，避免并发产生多个默认地址。
     */
    @Override
    public UserProfile lockProfile(Long userId) {
        initializeProfile(userId, defaultNickname(userId));
        UserProfile profile = userProfileMapper.selectByIdForUpdate(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户资料不存在");
        }
        return profile;
    }

    /**
     * 查询必须存在的用户资料。
     *
     * @param userId 用户ID
     * @return 用户资料实体
     */
    private UserProfile requireProfile(Long userId) {
        UserProfile profile = getById(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户资料不存在");
        }
        return profile;
    }

    /**
     * 使用用户ID末六位生成稳定、低敏感的默认昵称。
     *
     * @param userId 用户ID
     * @return 默认昵称
     */
    private String defaultNickname(Long userId) {
        String id = String.valueOf(userId);
        return "用户" + id.substring(Math.max(0, id.length() - 6));
    }

    /**
     * 使用 Builder 将资料实体转换为响应对象，字段映射关系更直观。
     *
     * @param profile 用户资料实体
     * @return 用户资料响应
     */
    private UserProfileVO toProfileVO(UserProfile profile) {
        return UserProfileVO.builder()
                .userId(profile.getUserId())
                .nickname(profile.getNickname())
                .avatarOssKey(profile.getAvatarOssKey())
                .gender(profile.getGender())
                .birthday(profile.getBirthday())
                .bio(profile.getBio())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
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
