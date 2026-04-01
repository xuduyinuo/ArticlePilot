package com.xudu.articlepilot.model.dto.article;

import lombok.Data;

import java.io.Serializable;

@Data
public class ArticleCreateRequest implements Serializable {

    /**
     * 选题
     */
    private String topic;

    private static final long serialVersionUID = 1L;
}