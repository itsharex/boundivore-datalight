package cn.boundivore.dl.orm.po.single;

import cn.boundivore.dl.base.enumeration.impl.NodeStateEnum;
import cn.boundivore.dl.orm.po.TBasePo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 节点信息表
 * </p>
 *
 * @author Boundivore
 * @since 2023-06-09 11:37:19
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("t_dl_node")
@ApiModel(value = "TDlNode对象", description = "节点信息表")
public class TDlNode extends TBasePo<TDlNode> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("集群 ID")
    @TableField("cluster_id")
    private Long clusterId;

    @ApiModelProperty("主机名")
    @TableField("hostname")
    private String hostname;

    @ApiModelProperty("IPV4 地址")
    @TableField("ipv4")
    private String ipv4;

    @ApiModelProperty("IPV6 地址")
    @TableField("ipv6")
    private String ipv6;

    @ApiModelProperty("SSH 端口 默认为 22 端口，可自定义修改")
    @TableField("ssh_port")
    private Long sshPort;

    @ApiModelProperty("CPU 架构")
    @TableField("cpu_arch")
    private String cpuArch;

    @ApiModelProperty("CPU 核心数 单位：个")
    @TableField("cpu_cores")
    private Long cpuCores;

    @ApiModelProperty("内存总大小 单位：Kbytes")
    @TableField("ram")
    private Long ram;

    @ApiModelProperty("磁盘总容量 单位：Kbytes")
    @TableField("disk")
    private Long disk;

    @ApiModelProperty("状态枚举，见代码")
    @TableField("node_state")
    private NodeStateEnum nodeState;

    @ApiModelProperty("系统版本")
    @TableField("os_version")
    private String osVersion;


}
