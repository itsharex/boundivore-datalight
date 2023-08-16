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
package cn.boundivore.dl.service.master.manage.service.job;

import cn.boundivore.dl.base.enumeration.impl.ExecStateEnum;
import cn.boundivore.dl.base.enumeration.impl.SCStateEnum;
import cn.boundivore.dl.base.result.Result;
import cn.boundivore.dl.cloud.utils.SpringContextUtil;
import cn.boundivore.dl.exception.BException;
import cn.boundivore.dl.orm.po.single.TDlComponent;
import cn.boundivore.dl.orm.po.single.TDlNode;
import cn.boundivore.dl.service.master.converter.IStepConverter;
import cn.boundivore.dl.service.master.manage.service.bean.JobMeta;
import cn.boundivore.dl.service.master.manage.service.bean.StageMeta;
import cn.boundivore.dl.service.master.manage.service.bean.StepMeta;
import cn.boundivore.dl.service.master.manage.service.bean.TaskMeta;
import cn.boundivore.dl.service.master.manage.service.stage.IStage;
import cn.boundivore.dl.service.master.manage.service.stage.impl.Stage;
import cn.boundivore.dl.service.master.manage.service.task.ITask;
import cn.boundivore.dl.service.master.manage.service.task.impl.Task;
import cn.boundivore.dl.service.master.resolver.ResolverYamlServiceDetail;
import cn.boundivore.dl.service.master.resolver.yaml.YamlServiceDetail;
import cn.boundivore.dl.service.master.service.RemoteInvokeGrafanaService;
import cn.boundivore.dl.service.master.service.RemoteInvokePrometheusService;
import cn.boundivore.dl.service.master.service.RemoteInvokeWorkerService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description: 为当前服务组装一个 Job
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/5/30
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
@Slf4j
public class Job extends Thread {

    @Getter
    private JobMeta jobMeta;

    @Getter
    private final Intention intention;

    @Getter
    private final Plan plan;

    private final JobService jobService = SpringContextUtil.getBean(JobService.class);

    private final IStepConverter iStepConverter = SpringContextUtil.getBean(IStepConverter.class);

    private boolean isInit;

    protected final RemoteInvokePrometheusService remoteInvokePrometheusService = SpringContextUtil.getBean(RemoteInvokePrometheusService.class);

    public Job(Intention intention) {
        this.intention = intention;
        this.plan = new Plan(this.intention);
    }


    /**
     * Description: 1、初始化各类异步任务 Meta 信息；2、根据初始化好的 Meta 信息，初始化执行计划
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @return Job
     */
    public Job init() throws InterruptedException {
        this.initJobMeta();
        this.plan(this.jobMeta);

        this.jobService.updateJobDatabase(this.jobMeta);
        JobCache.getInstance().cache(this);

        this.plan.initExecTotal(this.jobMeta);
        this.isInit = true;
        return this;
    }

    /**
     * Description: 初始化 JobMeta，并组装 Stage、Task
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    private JobMeta initJobMeta() throws InterruptedException {
        long jobMetaId = IdUtil.getSnowflakeNextId();

        Assert.isTrue(
                JobCache.getInstance().setActiveJobId(jobMetaId),
                () -> new BException(
                        String.format(
                                "安全起见，不允许同时对集群服务组件进行变更，已有其他活跃的任务正在运行: %s",
                                JobCache.getInstance().getActiveJobId()
                        )
                )
        );

        this.jobMeta = new JobMeta()
                .setTag(IdUtil.fastSimpleUUID())
                .setId(jobMetaId)
                .setName(intention.getActionTypeEnum().name())
                .setStageMetaMap(new LinkedHashMap<>())
                .setExecStateEnum(ExecStateEnum.SUSPEND)
                .setJobResult(new JobMeta.JobResult(false))
                // 当前集群以及所依赖的集群信息（如果为 MIXED 集群，则所依赖集群信息为 null）
                .setClusterMeta(intention.getClusterMeta())
                .setActionTypeEnum(intention.getActionTypeEnum());

        intention.getServiceList()
                .forEach(i -> {
                            final StageMeta stageMeta = this.initStageMeta(this.jobMeta, i);
                            this.jobMeta.getStageMetaMap().put(stageMeta.getId(), stageMeta);
                        }
                );

        return this.jobMeta;

    }


    /**
     * Description: 初始化当前 StageMeta，StageMeta 会切分不同的 Service
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param jobMeta          Job 元数据
     * @param intentionService 操作意图封装的 Service
     * @return StageMeta
     */
    private StageMeta initStageMeta(final JobMeta jobMeta,
                                    final Intention.Service intentionService) {
        String serviceName = intentionService.getServiceName();
        Long priority = intentionService.getPriority();
        List<Intention.Component> componentList = intentionService.getComponentList();


        //初始化 StageMeta
        final StageMeta stageMeta = new StageMeta()
                .setJobMeta(jobMeta)
                .setId(IdUtil.getSnowflakeNextId())
                .setName(
                        String.format(
                                "%s:%s",
                                jobMeta.getName(),
                                intentionService.getServiceName()
                        )
                )
                .setServiceName(serviceName)
                .setPriority(priority)
                .setStageStateEnum(ExecStateEnum.SUSPEND)
                .setStageResult(new StageMeta.StageResult(false))
                .setTaskMetaMap(new LinkedHashMap<>());


        //根据意图中待部署的组件列表，初始化若干 TaskMeta（同一个组件，在不同节点上的部署，属于不同 TaskMeta 实例）
        componentList.forEach(i -> {
            List<TaskMeta> taskMetaList = this.initTaskMeta(
                    stageMeta,
                    i
            );
            taskMetaList.forEach(
                    taskMeta -> stageMeta.getTaskMetaMap().put(taskMeta.getId(), taskMeta)
            );
        });

        return stageMeta;
    }


    /**
     * Description: 初始化当前 TaskMeta，TaskMeta 会切分不同的 Component 或 Node
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param stageMeta          Stage 元数据信息
     * @param intentionComponent 意图中的组件信息
     * @return List<TaskMeta>
     */
    private List<TaskMeta> initTaskMeta(final StageMeta stageMeta,
                                        final Intention.Component intentionComponent) {
        String serviceName = stageMeta.getServiceName();
        String componentName = intentionComponent.getComponentName();
        Long priority = intentionComponent.getPriority();
        List<Intention.Node> nodeList = intentionComponent.getNodeList();

        YamlServiceDetail.Component component = ResolverYamlServiceDetail.COMPONENT_MAP.get(
                intentionComponent.getComponentName()
        );

        //获取异步任务中，配置文件中设定的组件的状态变化值
        SCStateEnum startState = null;
        SCStateEnum failState = null;
        SCStateEnum successState = null;

        for (YamlServiceDetail.Action action : component.getActions()) {
            if (action.getType() != this.intention.getActionTypeEnum()) continue;

            startState = action.getStartState();
            failState = action.getFailState();
            successState = action.getSuccessState();

        }

        SCStateEnum finalStartState = startState;
        SCStateEnum finalFailState = failState;
        SCStateEnum finalSuccessState = successState;

        //实例化组装每一个 TaskMeta
        return nodeList.stream()
                .map(i -> {
                            TaskMeta taskMeta = new TaskMeta()
                                    .setStageMeta(stageMeta)
                                    .setWait(false)//TODO 传入必要参数，比如滚动重启这样的需求
                                    .setId(IdUtil.getSnowflakeNextId())
                                    .setName(String.format(
                                                    "%s:%s[%s]",
                                                    stageMeta.getName(),
                                                    componentName,
                                                    i.getHostname()
                                            )
                                    )
                                    .setHostname(i.getHostname())
                                    .setNodeIp(i.getNodeIp())
                                    .setNodeId(i.getNodeId())

                                    .setActionTypeEnum(jobMeta.getActionTypeEnum())
                                    .setPriority(priority)

                                    .setServiceName(serviceName)
                                    .setComponentName(componentName)
                                    .setStartState(finalStartState)
                                    .setFailState(finalFailState)
                                    .setSuccessState(finalSuccessState)

                                    .setTaskStateEnum(ExecStateEnum.SUSPEND)
                                    .setTaskResult(new TaskMeta.TaskResult(false))
                                    .setStepMetaMap(new LinkedHashMap<>());

                            //同一个 TaskMeta 中，会根据 ActionTypeEnum，封装一组 StepMeta
                            List<StepMeta> stepMetaList = this.initStepMeta(taskMeta);
                            stepMetaList.forEach(s -> taskMeta.getStepMetaMap().put(s.getId(), s));

                            //更新计划执行进度
                            this.plan.planProcess();

                            return taskMeta;
                        }
                )
                .collect(Collectors.toList());
    }


    /**
     * Description: 初始化当前 StepMeta，StepMeta 为具体某个Service，某个 Compoent，某个 Node，某个 Action 的一个操作；
     * 因为 TaskMeta 会包含一组 StepMeta，因此返回 List<StepMeta>
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12 15:08
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param taskMeta Task 中的元数据信息
     * @return List<StepMeta>
     */
    private List<StepMeta> initStepMeta(TaskMeta taskMeta) {

        YamlServiceDetail.Component component = ResolverYamlServiceDetail.COMPONENT_MAP.get(
                taskMeta.getComponentName()
        );

        YamlServiceDetail.Action action = CollUtil.findOne(
                component.getActions(),
                i -> i.getType() == taskMeta.getActionTypeEnum()
        );

        return action.getSteps()
                .stream()
                //转换器转换部分属性值，其余属性值通过 set 方法设定
                .map(i -> iStepConverter.convert2StepMeta(i)
                        .setTaskMeta(taskMeta)
                        .setId(IdUtil.getSnowflakeNextId())
                        .setName(String.format(
                                        "%s:%s",
                                        taskMeta.getName(),
                                        i.getName()
                                )
                        )
                        .setExecStateEnum(ExecStateEnum.SUSPEND)
                        .setStepResult(new StepMeta.StepResult(false)))
                .collect(Collectors.toList());
    }


    /**
     * Description: 根据元数据信息，为当前 Job 生成任务执行计划
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/5/30 19:07
     * Modification description:
     * Modified by:
     * Modification time:
     *
     * @param jobMeta 工作元数据信息
     */
    private void plan(JobMeta jobMeta) {

        jobMeta.getStageMetaMap()
                .forEach((kStage, vStage) -> {
                            log.info(
                                    "Stage: {}, Id: {}, Tag: {}",
                                    vStage.getName(),
                                    vStage.getId(),
                                    vStage.getJobMeta().getTag()
                            );

                            IStage iStage = new Stage(vStage);

                            vStage.getTaskMetaMap()
                                    .forEach((kTask, vTask) -> {
                                                log.info(
                                                        "Task: {}, Id: {}, Tag: {}",
                                                        vTask.getName(),
                                                        vTask.getId(),
                                                        vTask.getStageMeta().getJobMeta().getTag()
                                                );

                                                ITask iTask = new Task(vTask);
                                                iStage.offerTask(iTask);

                                                //更新计划执行进度
                                                this.plan.planProcess();
                                            }
                                    );
                            this.plan.offerStage(iStage);
                        }
                );
    }

    /**
     * Description: 执行当前 Job
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/5/30 19:08
     * Modification description:
     * Modified by:
     * Modification time:
     */
    private void execute() {
        //记录 Job 起始时间
        this.jobMeta.setStartTime(System.currentTimeMillis());

        //更新当前 Job 执行状态到内存缓存和数据库
        this.updateJobExecutionStatus(ExecStateEnum.RUNNING);

        // 依次执行每一个 Stage 并得到 StageResult 多个 Stage 为串行
        // Stage 中的多个 Task 为并行或串行（取决于组件之间的依赖关系，同一 priority 为并发，不同 priority 为串行）
        // Task 中的多个 Step 为串行
        for (IStage stage : this.plan.getStages()) {
            try {
                StageMeta stageMeta = this.jobService.submit(stage).get();
                this.jobMeta.getJobResult().setSuccess(
                        stageMeta.getStageResult().isSuccess()
                );

                //如果中途出现任何的失败，则终止整个 Job，防止强制执行后整个集群出现过多异常，增加回滚成本
                if (!this.jobMeta.getJobResult().isSuccess()) break;

            } catch (Exception e) {
                log.error(ExceptionUtil.stacktraceToString(e));
            }
        }

        // 异步任务最后，重新加载 Prometheus 配置
        this.invokePrometheusReload(
                this.jobMeta
                        .getClusterMeta()
                        .getCurrentClusterId()
        );


        //记录 Job 结束时间(自动计算耗时)
        this.jobMeta.setEndTime(System.currentTimeMillis());

        log.info(
                "结束 Job: {}, 耗时: {} ms",
                jobMeta.getName(),
                jobMeta.getDuration()
        );


        ExecStateEnum execStateEnum = this.jobMeta.getJobResult().isSuccess() ?
                ExecStateEnum.OK :
                ExecStateEnum.ERROR;

        //更新当前 Job 执行状态到内存缓存和数据库
        this.updateJobExecutionStatus(execStateEnum);

        //清除所有可能残留的异步任务
        this.plan.clear();

        JobCache.getInstance().releaseActiveJobId(this.jobMeta.getId());
    }

    /**
     * Description: 调用 job.start() 后，将按照执行计划开始执行当前 Job
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/6/12 15:46
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     */
    @Override
    public void run() {
        Assert.isTrue(
                this.isInit,
                () -> new BException("执行 Job 前需要先调用 init() 初始化任务计划")
        );

        this.execute();
    }

    /**
     * Description: 更新当前 Job 执行状态到内存缓存和数据库
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/7/4
     * Modification description:
     * Modified by:
     * Modification time:
     * Throws:
     *
     * @param execStateEnum 当前状态
     */
    private void updateJobExecutionStatus(ExecStateEnum execStateEnum) {
        // 更新当前作业的执行状态到内存缓存
        this.jobService.updateJobMemory(this.jobMeta, execStateEnum);
        // 更新当前作业的执行状态到数据库
        this.jobService.updateJobDatabase(this.jobMeta);
    }

    /**
     * Description: 重新加载 Prometheus
     * Created by: Boundivore
     * E-mail: boundivore@foxmail.com
     * Creation time: 2023/5/31
     * Modification description:
     * Modified by:
     * Modification time:
     */
    private void invokePrometheusReload(Long clusterId) {

        try {
            TDlComponent prometheusServerTDlComponent = this.jobService
                    .getTDlComponentListByServiceName(
                            clusterId,
                            "MONITOR"
                    )
                    .stream()
                    .filter(i -> i.getComponentName().equals("Prometheus"))
                    .collect(Collectors.toList())
                    .get(0);

            if (prometheusServerTDlComponent != null && prometheusServerTDlComponent.getComponentState() == SCStateEnum.STARTED) {
                TDlNode tDlNodePrometheus = this.jobService.masterNodeService.getNodeListInNodeIds(
                                clusterId,
                                CollUtil.newArrayList(prometheusServerTDlComponent.getNodeId())
                        )
                        .get(0);

                this.remoteInvokePrometheusService.iThirdPrometheusAPI(
                                tDlNodePrometheus.getIpv4(),
                                "9090"
                        )
                        .reloadPrometheus();
            }
        } catch (Exception e) {
            log.error(ExceptionUtil.stacktraceToString(e));
        }
    }
}
