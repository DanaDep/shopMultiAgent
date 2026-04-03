package com.dep.agents;

import com.dep.tools.OrderTool;
import com.dep.tools.RefundTool;
import com.dep.tools.ReturnTool;
import com.dep.tools.ReviewTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;

@Factory
public class AgentFactory {

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
}
