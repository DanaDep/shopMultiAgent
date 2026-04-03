package com.dep.configurations;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import dev.langchain4j.model.chat.ChatModel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Factory
@Slf4j
public class ChatModelConfig {

	private final BedrockProperties bedrockProperties;

	public ChatModelConfig(BedrockProperties bedrockProperties) {
		this.bedrockProperties = bedrockProperties;
	}

	@Bean
	@Singleton
	public ChatModel model() {
		return BedrockChatModel.builder()
				.modelId(bedrockProperties.getModelId())
				.region(Region.of(bedrockProperties.getRegion()))
				.timeout(bedrockProperties.getTimeout())
				.logRequests(true)
				.logResponses(true)
				.defaultRequestParameters(BedrockChatRequestParameters.builder()
						.maxOutputTokens(bedrockProperties.getMaxOutputTokens())
						.temperature(bedrockProperties.getTemperature())
						.build()
				)
				.build();
	}

}
