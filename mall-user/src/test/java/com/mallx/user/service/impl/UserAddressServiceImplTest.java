package com.mallx.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.user.dto.UserAddressRequest;
import com.mallx.user.entity.UserAddress;
import com.mallx.user.mapper.UserAddressMapper;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserAddressVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户地址服务核心规则单元测试。 */
@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    /** 纯单元测试中手动初始化 LambdaWrapper 所需的实体字段缓存。 */
    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "test"), UserAddress.class);
    }

    @Mock
    private UserAddressMapper userAddressMapper;

    @Mock
    private UserProfileService userProfileService;

    private UserAddressServiceImpl userAddressService;

    /** 模拟 Spring 注入 Mapper，并为 Lambda Chain 提供真实泛型元数据。 */
    @BeforeEach
    void injectBaseMapper() {
        userAddressService = new UserAddressServiceImpl(userProfileService) {
            @Override
            public Class<UserAddress> getEntityClass() {
                return UserAddress.class;
            }

            @Override
            public Class<UserAddressMapper> getMapperClass() {
                return UserAddressMapper.class;
            }
        };
        ReflectionTestUtils.setField(userAddressService, "baseMapper", userAddressMapper);
    }

    /** 验证第一条收货地址即使未显式指定，也会自动成为默认地址。 */
    @Test
    void firstAddressShouldBecomeDefaultAutomatically() {
        Long userId = 1001L;
        when(userAddressMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(2001L);
            return 1;
        }).when(userAddressMapper).insert(any(UserAddress.class));
        when(userAddressMapper.selectOne(any())).thenAnswer(invocation -> {
            UserAddress address = new UserAddress();
            address.setId(2001L);
            address.setUserId(userId);
            address.setRecipientName("张三");
            address.setRecipientMobile("13800138000");
            address.setProvinceCode("110000");
            address.setProvinceName("北京市");
            address.setCityCode("110100");
            address.setCityName("北京市");
            address.setDistrictCode("110105");
            address.setDistrictName("朝阳区");
            address.setDetailAddress("测试路 1 号");
            address.setIsDefault(1);
            return address;
        });

        UserAddressVO result = userAddressService.createAddress(userId, addressRequest());

        assertEquals(2001L, result.getId());
        assertTrue(result.isDefaultAddress());
        verify(userProfileService).lockProfile(eq(userId));
    }

    /** 构造通过参数校验的标准地址请求。 */
    private UserAddressRequest addressRequest() {
        UserAddressRequest request = new UserAddressRequest();
        request.setRecipientName("张三");
        request.setRecipientMobile("13800138000");
        request.setProvinceCode("110000");
        request.setProvinceName("北京市");
        request.setCityCode("110100");
        request.setCityName("北京市");
        request.setDistrictCode("110105");
        request.setDistrictName("朝阳区");
        request.setDetailAddress("测试路 1 号");
        return request;
    }
}
