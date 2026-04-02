package com.xudu.articlepilot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章表
 * @TableName article
 */
@TableName(value ="article")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 任务ID（UUID）
     */
    @TableField(value = "taskId")
    private String taskId;

    /**
     * 用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 选题
     */
    @TableField(value = "topic")
    private String topic;

    /**
     * 文章风格：tech/emotional/educational/humorous，可为空
     */
    private String style;

    /**
     * 主标题
     */
    @TableField(value = "mainTitle")
    private String mainTitle;

    /**
     * 副标题
     */
    @TableField(value = "subTitle")
    private String subTitle;

    /**
     * 大纲（JSON格式）
     */
    @TableField(value = "outline")
    private String outline;

    /**
     * 正文（Markdown格式）
     */
    @TableField(value = "content")
    private String content;

    /**
     * 完整图文（Markdown格式，含配图）
     */
    @TableField(value = "fullContent")
    private String fullContent;

    /**
     * 封面图 URL
     */
    @TableField(value = "coverImage")
    private String coverImage;

    /**
     * 配图列表（JSON数组，包含封面图 position=1）
     */
    @TableField(value = "images")
    private String images;

    /**
     * 状态：PENDING/PROCESSING/COMPLETED/FAILED
     */
    @TableField(value = "status")
    private String status;

    /**
     * 错误信息
     */
    @TableField(value = "errorMessage")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    @TableField(value = "completedTime")
    private LocalDateTime completedTime;

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