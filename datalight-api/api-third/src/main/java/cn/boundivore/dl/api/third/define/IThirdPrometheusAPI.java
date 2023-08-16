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
package cn.boundivore.dl.api.third.define;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import static cn.boundivore.dl.base.constants.IUrlPrefixConstants.NONE_PREFIX;


/**
 * Description: Prometheus 节点执行指定脚本代码
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/5/13
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Api(value = "IWorkerPrometheusAPI", tags = {"Prometheus 接口：直接调用 Prometheus 接口"})
@FeignClient(
        name = "IWorkerPrometheusAPI",
        contextId = "IWorkerPrometheusAPI",
        path = NONE_PREFIX
)
public interface IThirdPrometheusAPI {
    @PostMapping(value = "/-/reload")
    @ApiOperation(notes = "重新加载 Prometheus 配置", value = "重新加载 Prometheus 配置")
    String reloadPrometheus();

}
