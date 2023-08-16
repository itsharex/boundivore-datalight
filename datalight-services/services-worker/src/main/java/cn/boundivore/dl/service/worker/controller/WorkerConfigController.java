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
package cn.boundivore.dl.service.worker.controller;

import cn.boundivore.dl.api.worker.define.IWorkerConfigAPI;
import cn.boundivore.dl.api.worker.define.IWorkerExecAPI;
import cn.boundivore.dl.base.request.impl.common.TestRequest;
import cn.boundivore.dl.base.request.impl.worker.ConfigFileRequest;
import cn.boundivore.dl.base.request.impl.worker.ExecRequest;
import cn.boundivore.dl.base.result.Result;
import cn.boundivore.dl.service.worker.service.WorkerConfigService;
import cn.boundivore.dl.service.worker.service.WorkerExecService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: WorkerConfigController
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/5/5
 * Modification description:
 * Modified by: 
 * Modification time: 
 * Version: V1.0
 */
@RestController
@RequiredArgsConstructor
public class WorkerConfigController implements IWorkerConfigAPI {

    private final WorkerConfigService workerConfigService;

    @Override
    public Result<String> config(ConfigFileRequest request) {
        return workerConfigService.config(request);
    }
}
