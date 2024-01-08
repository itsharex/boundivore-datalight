/**
 * Copyright (C) <2023> <Boundivore> <boundivore@foxmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Apache License, Version 2.0
 * as published by the Apache Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License, Version 2.0 for more details.
 * <p>
 * You should have received a copy of the Apache License, Version 2.0
 * along with this program; if not, you can obtain a copy at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package cn.boundivore.dl.service.master.service;

import cn.boundivore.dl.base.constants.ICommonConstant;
import cn.boundivore.dl.base.enumeration.impl.IdentityTypeEnum;
import cn.boundivore.dl.base.request.impl.master.AbstractUserRequest;
import cn.boundivore.dl.base.response.impl.master.UserInfoVo;
import cn.boundivore.dl.base.result.Result;
import cn.boundivore.dl.exception.BException;
import cn.boundivore.dl.exception.DatabaseException;
import cn.boundivore.dl.exception.LoginFailException;
import cn.boundivore.dl.orm.po.single.TDlLoginEvent;
import cn.boundivore.dl.orm.po.single.TDlUser;
import cn.boundivore.dl.orm.po.single.TDlUserAuth;
import cn.boundivore.dl.orm.service.single.impl.TDlLoginEventServiceImpl;
import cn.boundivore.dl.orm.service.single.impl.TDlUserAuthServiceImpl;
import cn.boundivore.dl.orm.service.single.impl.TDlUserServiceImpl;
import cn.boundivore.dl.service.master.converter.IUserConverter;
import cn.boundivore.dl.service.master.env.DataLightEnv;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Description: 用户操作相关逻辑
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/15
 * Modification description:
 * Modified by:
 * Modification time:
 * Throws:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MasterUserService {

    protected final TDlUserServiceImpl tDlUserService;
    protected final TDlUserAuthServiceImpl tDlUserAuthService;
    protected final TDlLoginEventServiceImpl tDlLoginEventService;

    protected final IUserConverter iUserConverter;

    protected final BCryptPasswordEncoder passwordEncoder;

    protected final DataLightEnv dataLightEnv;

    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public Result<UserInfoVo> register(AbstractUserRequest.UserRegisterRequest request) throws Exception {
        AbstractUserRequest.UserAuthRequest userAuth = request.getUserAuth();
        AbstractUserRequest.UserBaseRequest userBase = request.getUserBase();

        // 检查注册信息是否合法
        this.checkUserAuthRegisterRequest(userAuth);

        //保存用户基础信息
        TDlUser tUser = this.iUserConverter.convert2TUsers(userBase);
        Assert.isTrue(
                this.tDlUserService.save(tUser),
                () -> new DatabaseException("用户基础数据保存失败")
        );

        //保存用户认证信息
        String encodePassword = this.passwordEncoder.encode(userAuth.getCredential());

        TDlUserAuth tUserAuth = this.iUserConverter.convert2TUsersAuth(request.getUserAuth());
        tUserAuth.setUserId(tUser.getId());
        tUserAuth.setCredential(encodePassword);

        Assert.isTrue(
                this.tDlUserAuthService.save(tUserAuth),
                () -> new DatabaseException("用户凭证数据保存失败")
        );

        // 保存用户登录信息
        TDlLoginEvent tDlLoginEvent = new TDlLoginEvent();
        tDlLoginEvent.setUserId(tUserAuth.getUserId());
        tDlLoginEvent.setLastLogin(-1L);

        Assert.isTrue(
                this.tDlLoginEventService.save(tDlLoginEvent),
                () -> new DatabaseException("保存初始化登录信息失败")
        );

        return Result.success(iUserConverter.convert2UserInfoVo(tUser));
    }

    /**
     * Description: 检查注册时用户认证信息是否合法
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws: BException
     *
     * @param request 注册时用户认证信息
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public void checkUserAuthRegisterRequest(AbstractUserRequest.UserAuthRequest request) {
        boolean exists = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getPrincipal, request.getPrincipal())
                .exists();

        Assert.isFalse(
                exists,
                () -> new BException("用户已存在")
        );
    }


    /**
     * Description: 用户登录
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/4
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param request 登录验证请求体
     * @return Result<UserInfoVo> 登录后的用户信息
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public Result<UserInfoVo> login(AbstractUserRequest.UserAuthRequest request) throws Exception {

        // 检查认证主体格式是否合法
        this.checkPrincipalIllegal(request.getIdentityType(), request.getPrincipal());

        TDlUserAuth tDlUserAuth = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getIdentityType, request.getIdentityType())
                .eq(TDlUserAuth::getPrincipal, request.getPrincipal())
                .one();

        Assert.notNull(
                tDlUserAuth,
                () -> new LoginFailException("账号或密码错误")
        );

        // 验证密码是否匹配
        Assert.isTrue(
                this.passwordEncoder.matches(
                        request.getCredential(),
                        tDlUserAuth.getCredential()
                ),
                () -> new LoginFailException("账号或密码错误")
        );

        // 读取用户基本数据
        TDlUser tDlUser = this.tDlUserService.getById(tDlUserAuth.getUserId());
        // 读取用户登录数据
        TDlLoginEvent tDlLoginEvent = this.tDlLoginEventService.lambdaQuery()
                .select()
                .eq(TDlLoginEvent::getUserId, tDlUserAuth.getUserId())
                .one();

        if (tDlLoginEvent == null) {
            tDlLoginEvent = new TDlLoginEvent();
            tDlLoginEvent.setUserId(tDlUserAuth.getUserId());
            tDlLoginEvent.setLastLogin(-1L);
        }

        // 登录
        StpUtil.login(tDlUserAuth.getUserId());

        // 组装返回实体
        UserInfoVo userInfoVo = this.iUserConverter.convert2UserInfoVo(tDlUser);
        userInfoVo.setLastLogin(tDlLoginEvent.getLastLogin());
        userInfoVo.setToken(StpUtil.getTokenValue());
        userInfoVo.setTokenTimeout(StpUtil.getTokenTimeout(StpUtil.getTokenValue()));

        // 更新登录时间
        Assert.isTrue(
                tDlLoginEvent.setLastLogin(System.currentTimeMillis()).updateById(),
                () -> new DatabaseException("更新登录时间失败")
        );

        return Result.success(userInfoVo);
    }

    /**
     * Description: 检查 Principal 格式是否合法
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws: BException
     *
     * @param identityType 认证主体的格式
     * @param principal    认证主体
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public void checkPrincipalIllegal(IdentityTypeEnum identityType, String principal) {
        switch (identityType) {
            case EMAIL:
                Assert.isTrue(
                        principal.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"),
                        () -> new BException("登录主体需要符合邮件格式")
                );
                break;
            case PHONE:
                Assert.isTrue(
                        principal.matches("1[3-9]\\d{9}"),
                        () -> new BException("登录主体需要符合手机号码格式")
                );
                break;
            case USERNAME:
                Assert.isTrue(
                        !principal.matches(".*[^a-zA-Z0-9_-].*"),
                        () -> new BException("登录主体不应包含特殊字符")
                );
                break;
        }
    }


    /**
     * Description: 退出登录
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/4
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return Result<String> 成功或失败
     */
    public Result<String> logout(Long userId) throws Exception {
        StpUtil.logout(userId);
        return Result.success();
    }

    /**
     * Description: 判断是否已登录
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/4
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return Result<Boolean> 登录或未登录
     */
    public Result<Boolean> isLogin(Long userId) throws Exception {
        return Result.success(StpUtil.isLogin(userId));
    }

    /**
     * Description: 修改登录密码
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param request 修改密码请求体
     * @return Result<String> 成功或失败
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public Result<String> changePassword(AbstractUserRequest.UserChangePasswordRequest request) {
        // 获取当前已登录的账户信息
        TDlUserAuth loginTDlUserAuth = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getUserId, StpUtil.getLoginId())
                .one();
        String loginPrincipal = loginTDlUserAuth.getPrincipal();
        String changePasswordPrincipal = request.getPrincipal();

        // 判断是否为修改自己的密码，如果不是，则判断是否为管理员代修改普通用户密码，如果也不是，则抛出异常
        Assert.isTrue(
                !loginPrincipal.equals(dataLightEnv.getSuperUser()) &&
                        !loginPrincipal.equals(changePasswordPrincipal),
                () -> new BException("普通用户只可修改自身密码")
        );

        TDlUserAuth tDlUserAuth = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getPrincipal, request.getPrincipal())
                .one();

        // 如果为超级用户，则跳过旧密码验证
        if (!loginPrincipal.equals(dataLightEnv.getSuperUser())) {
            Assert.isTrue(
                    this.passwordEncoder.matches(
                            request.getOldCredential(),
                            tDlUserAuth.getCredential()
                    ),
                    () -> new BException("旧密码错误")
            );
        }

        // 新旧密码不能相同
        Assert.isFalse(
                request.getOldCredential().equals(request.getNewCredential()),
                () -> new BException("新旧密码不能相同")
        );

        // 更新数据库
        Assert.isTrue(
                tDlUserAuth.setCredential(
                        this.passwordEncoder.encode(
                                request.getNewCredential()
                        )
                ).updateById(),
                () -> new DatabaseException("更新密码失败")
        );

        // 密码更新成功后，退出被修改密码用户的登录
        StpUtil.logout(tDlUserAuth.getUserId());

        return Result.success();
    }

    /**
     * Description: 判断超级用户是否变更过初始密码
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return Result<Boolean> true or false
     */
    public Result<Boolean> isNeed2ChangeSuperPassword() {
        TDlUserAuth tDlUserAuth = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getPrincipal, dataLightEnv.getSuperUser())
                .one();

        boolean isNeed2ChangeSuperPassword = this.passwordEncoder.matches(
                DigestUtil.md5Hex(dataLightEnv.getSuperUserDefaultPassword()),
                tDlUserAuth.getCredential()
        );

        return Result.success(isNeed2ChangeSuperPassword);
    }

    /**
     * Description: 检查超级用户数据是否已缓存到数据库，如果没有，则缓存
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2024/1/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    @Transactional(
            timeout = ICommonConstant.TIMEOUT_TRANSACTION_SECONDS,
            rollbackFor = DatabaseException.class
    )
    public void checkInitSuperUser() throws Exception {
        boolean exists = this.tDlUserAuthService.lambdaQuery()
                .select()
                .eq(TDlUserAuth::getPrincipal, dataLightEnv.getSuperUser())
                .exists();

        if (exists) return;

        AbstractUserRequest.UserAuthRequest userAuthRequest = new AbstractUserRequest.UserAuthRequest(
                IdentityTypeEnum.USERNAME,
                dataLightEnv.getSuperUser(),
                DigestUtil.md5Hex(dataLightEnv.getSuperUserDefaultPassword())
        );
        AbstractUserRequest.UserBaseRequest userBaseRequest = new AbstractUserRequest.UserBaseRequest(
                dataLightEnv.getSuperUser(),
                dataLightEnv.getSuperUser(),
                ""
        );
        AbstractUserRequest.UserRegisterRequest request = new AbstractUserRequest.UserRegisterRequest(
                userAuthRequest,
                userBaseRequest
        );

        this.register(request);
    }
}
