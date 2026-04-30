package com.stephanofer.prismapractice.feedback;

import java.util.List;

public record FeedbackTemplate(
        String key,
        List<FeedbackDelivery> deliveries
) {
}
