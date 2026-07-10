package com.dep.agents;

import java.util.List;
import com.dep.configurations.BedrockProperties;
import com.dep.listeners.OtelChatModelListener;
import com.dep.tools.OrderTool;
import com.dep.tools.RefundTool;
import com.dep.tools.ReturnTool;
import com.dep.tools.ReviewTool;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Factory
@Slf4j
public class AgentFactory {

	@Singleton
	public ChatModel chatModel(BedrockProperties props, OpenTelemetry openTelemetry) {
		Tracer tracer = openTelemetry.getTracer("langchain4j");

		log.info("Creating Bedrock chat model: modelId={}, timeout={}, temperature={}, maxOutputTokens={}",
				props.getModelId(), props.getTimeout(), props.getTemperature(), props.getMaxOutputTokens());

		return BedrockChatModel.builder()
				.modelId(props.getModelId())
				.region(Region.of(props.getRegion()))
				.timeout(props.getTimeout())
				.logRequests(true)
				.logResponses(true)
				.defaultRequestParameters(BedrockChatRequestParameters.builder()
						.maxOutputTokens(props.getMaxOutputTokens())
						.temperature(props.getTemperature())
						.build())
				.listeners(List.of(new OtelChatModelListener(tracer)))
				.build();
	}

	@Bean
	public ResearcherAgent researcherAgent(ChatModel chatModel,
										   OrderTool orderTool,
										   RefundTool refundTool,
										   ReturnTool returnTool,
										   ReviewTool reviewTool) {
		return AiServices.builder(ResearcherAgent.class)
				.chatModel(chatModel)
				.tools(orderTool, refundTool, returnTool, reviewTool)
				.build();
	}

	@Bean
	public WriterAgent writerAgent( ChatModel chatModel ) {
		return AiServices.builder( WriterAgent.class )
				.chatModel( chatModel )
				.build();
	}

	@Bean
	public CriticAgent criticAgent( ChatModel chatModel ) {
		return AiServices.builder( CriticAgent.class )
				.chatModel( chatModel )
				.build();
	}
}
