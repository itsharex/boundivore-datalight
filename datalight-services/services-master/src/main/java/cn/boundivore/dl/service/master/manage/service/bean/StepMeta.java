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
package cn.boundivore.dl.service.master.manage.service.bean;

import cn.boundivore.dl.base.enumeration.impl.ExecStateEnum;
import cn.boundivore.dl.base.enumeration.impl.StepTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Description: StepMeta
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/8
 * Modification description:
 * Modified by: 
 * Modification time: 
 * Version: V1.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@ToString(
        exclude = {
                "taskMeta"
        }
)
public class StepMeta extends TimeMeta {
    private StepTypeEnum type;

    private String name;

    private String jar;

    private String clazz;

    private String shell;

    private List<String> args;

    private List<String> interactions;

    private Integer exits;

    private Long timeout;

    private Long sleep;

    private TaskMeta taskMeta;

    private Long id;

    private ExecStateEnum execStateEnum;

    private StepResult stepResult;

    /**
     * Description: 异步 Step 执行结果信息
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/8
     * Modification description:
     * Modified by:
     * Modification time:
     * Version: V1.0
     */

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public final static class StepResult {
        private boolean isSuccess;
    }
}
