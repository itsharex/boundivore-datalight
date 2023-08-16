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
package cn.boundivore.dl.api.master.define;

import cn.boundivore.dl.base.request.impl.master.AbstractUserRequest;
import cn.boundivore.dl.base.response.impl.master.UserInfoVo;
import cn.boundivore.dl.base.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

import static cn.boundivore.dl.base.constants.IUrlPrefixConstants.MASTER_URL_PREFIX;


/**
 * Description: 关于用户的相关接口定义
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/5/13
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Api(value = "IMasterUserAPI", tags = {"Master 接口：用户调用相关"})
@FeignClient(
        name = "IMasterUserAPI",
        contextId = "IMasterUserAPI",
        path = MASTER_URL_PREFIX
)
public interface IMasterUserAPI {

    @PostMapping(value = "/user/register")
    @ApiOperation(notes = "用户注册", value = "用户注册")
    Result<UserInfoVo> register(
            @RequestBody
            @Valid
            AbstractUserRequest.UserRegisterRequest request
    ) throws Exception;

    @PostMapping(value = "/user/login")
    @ApiOperation(notes = "用户登录", value = "用户登录")
    Result<UserInfoVo> login(
            @RequestBody
            @Valid
            AbstractUserRequest.UserAuthRequest request
    ) throws Exception;

    @GetMapping(value = "/user/logout")
    @ApiOperation(notes = "用户登出", value = "用户登出")
    Result<String> logout() throws Exception;

    @GetMapping(value = "/user/isLogin")
    @ApiOperation(notes = "判断当前会话是否登录", value = "判断当前会话是否登录")
    Result<String> isLogin() throws Exception;


}
