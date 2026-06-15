package com.dep.agents;

import java.util.List;
import com.dep.listeners.OtelChatModelListener;
import com.dep.tools.OrderTool;
import com.dep.tools.RefundTool;
import com.dep.tools.ReturnTool;
import com.dep.tools.ReviewTool;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Singleton;
import software.amazon.awssdk.regions.Region;

@Factory
public class AgentFactory {

	@Singleton
	@Replaces(ChatModel.class)
	public ChatModel chatModel(
			@Value("${langchain4j.bedrock.chat-model.model-id}") String modelId,
			@Value("${aws.region}") String region,
			OpenTelemetry openTelemetry) {

		Tracer tracer = openTelemetry.getTracer("langchain4j");

		return BedrockChatModel.builder()
				.modelId(modelId)
				.region( Region.of(region))
				.listeners( List.of(new OtelChatModelListener(tracer)))
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
