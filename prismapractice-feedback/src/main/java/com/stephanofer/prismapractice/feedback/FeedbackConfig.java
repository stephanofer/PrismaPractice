package com.stephanofer.prismapractice.feedback;

import com.stephanofer.prismapractice.config.ConfigException;

import java.util.Map;

public record FeedbackConfig(Map<String, FeedbackTemplate> templates) {

    public FeedbackTemplate template(String key) {
        FeedbackTemplate template = templates.get(key);
        if (template == null) {
            throw new ConfigException("Unknown feedback template '" + key + "'");
        }
        return template;
    }
}
