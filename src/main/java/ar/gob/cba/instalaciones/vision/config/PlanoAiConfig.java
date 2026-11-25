package ar.gob.cba.instalaciones.vision.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class PlanoAiConfig {

    @Bean
    public ChatClient planoVisionChatClient(
            ChatClient.Builder builder,
            @Value("classpath:prompts/extraccion-plano-system.st") Resource systemPromptResource
    ) {
        return builder
                .defaultSystem(systemPromptResource)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
