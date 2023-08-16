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
package cn.boundivore.dl.plugin.base.config.event;

import cn.boundivore.dl.plugin.base.bean.PluginConfigEvent;
import cn.boundivore.dl.plugin.base.bean.PluginConfigResult;
import cn.boundivore.dl.plugin.base.bean.PluginConfigSelf;
import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Description: 抽象公共方法和变量
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/20
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Slf4j
public abstract class AbstractConfigEventHandler implements IConfigEventHandler {

    protected PluginConfigEvent pluginConfigEvent;

    @Override
    public void init(PluginConfigEvent pluginConfigEvent) {
        this.pluginConfigEvent = pluginConfigEvent;
    }

    @Override
    public List<String> getRelativeConfigPathList(List<String> configPathList) {
        return new ArrayList<>();
    }

    @Override
    public PluginConfigResult configByEvent(PluginConfigSelf pluginConfigSelf) {
        log.info(
                "{}-{} 插件中发现事件变动: {}-{}",
                pluginConfigSelf.getClusterId(),
                pluginConfigSelf.getServiceName(),
                this.pluginConfigEvent.getClusterId(),
                this.pluginConfigEvent.getServiceName()
        );

        // 此处不做任何修改
        // 子类中的实现需要配合修改配置文件
        // 修改被影响的服务的配置文件，需要考虑 MIXED 集群和 COMPUTE 集群
        return new PluginConfigResult(
                pluginConfigSelf.getClusterId(),
                pluginConfigSelf.getServiceName(),
                new LinkedHashMap<>()
        );
    }

    /**
     * Description: 将指定内容转换为 SHA256 字符串
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/14
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param content 原字符串内容
     * @return SHA256 摘要字符串
     */
    protected String sha256(String content) {
        return SecureUtil.sha256(content);
    }

    /**
     * Description: 将指定内容转换为 Base64 字符串
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/14
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param content 原字符串内容
     * @return Base64 字符串
     */
    protected String base64(String content) {
        return Base64.encode(content);
    }

    /**
     * Description: 解析 Base64 字符串
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/14
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param content Base64 原字符串内容
     * @return 原字符串
     */
    protected String deBase64(String content) {
        return Base64.decodeStr(content);
    }
}
