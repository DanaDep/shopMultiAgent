package com.dep.agents;
import com.dep.dtos.ResearchResult;
import dev.langchain4j.service.SystemMessage;

public interface ResearcherAgent {
	@SystemMessage("""
			You are a researcher agent. Your task is to research the given topic and return relevant findings. 
			Use all available resources and tools to gather information. 
			Focus on finding accurate and up-to-date information related to the topic.
			Provide concise and relevant findings that can be used by a writer agent to create content.
			""")
	ResearchResult research(String topic);
}
