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
package cn.boundivore.dl.base.response.impl.master;

import cn.boundivore.dl.base.response.IVo;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Description: 获取当前服务下所有配置信息概览列表
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/19
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(
        value = "ConfigListVo",
        description = "ConfigListVo: 配置文件列表"
)
public class ConfigSummaryListVo implements IVo {

    @ApiModelProperty(name = "ClusterId", value = "集群 ID", required = true)
    @JsonProperty(value = "ClusterId", required = true)
    private Long clusterId;

    @ApiModelProperty(name = "ServiceName", value = "服务名称", required = true)
    @JsonProperty(value = "ServiceName", required = true)
    private String serviceName;

    @ApiModelProperty(name = "ConfigSummaryList", value = "当前服务配置信息概览列表", required = true)
    @JsonProperty(value = "ConfigSummaryList", required = true)
    private List<ConfigSummaryVo> configSummaryList;


    @Data
    @Builder
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(
            callSuper = false,
            exclude = { "fileName" }
    )
    @ApiModel(
            value = "ConfigPreVo.ConfigSummaryVo",
            description = "ConfigPreVo.ConfigSummaryVo: 当前配置信息概览"
    )
    public static class ConfigSummaryVo implements IVo {

        @ApiModelProperty(name = "FileName", value = "配置文件名称", required = true)
        @JsonProperty(value = "FileName", required = true)
        private String fileName;

        @ApiModelProperty(name = "ConfigPath", value = "配置文件路径", required = true)
        @JsonProperty(value = "ConfigPath", required = true)
        private String configPath;

    }

}
