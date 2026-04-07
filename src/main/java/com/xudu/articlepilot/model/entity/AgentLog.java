package com.xudu.articlepilot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体执行日志表
 * @TableName agent_log
 */
@TableName(value ="agent_log")
@Data
@Builder
public class AgentLog implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField(value = "taskId")
    private String taskId;

    /**
     * 智能体名称
     */
    @TableField(value = "agentName")
    private String agentName;

    /**
     * 开始时间
     */
    @TableField(value = "startTime")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "endTime")
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    @TableField(value = "durationMs")
    private Integer durationMs;

    /**
     * 状态：SUCCESS/FAILED
     */
    @TableField(value = "status")
    private String status;

    /**
     * 错误信息
     */
    @TableField(value = "errorMessage")
    private String errorMessage;

    /**
     * 使用的Prompt
     */
    @TableField(value = "prompt")
    private String prompt;

    /**
     * 输入数据（JSON格式）
     */
    @TableField(value = "inputData")
    private String inputData;

    /**
     * 输出数据（JSON格式）
     */
    @TableField(value = "outputData")
    private String outputData;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}