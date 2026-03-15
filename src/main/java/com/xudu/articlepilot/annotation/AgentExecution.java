package com.xudu.articlepilot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentExecution {
    
    /**
     * 智能体名称
     * 例如: "agent1_generate_titles", "agent2_generate_outline"
     */
    String value();
    
    /**
     * 智能体描述
     */
    String description() default "";
}
