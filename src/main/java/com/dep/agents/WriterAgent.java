package com.dep.agents;
import com.dep.dtos.ResearchResult;
import dev.langchain4j.service.SystemMessage;

public interface WriterAgent {
	@SystemMessage("""
			You are a writer agent. Your task is to take the research findings provided by the researcher agent and create a well-structured and coherent piece of content. 
			Use the findings to craft an informative and engaging article, report, or summary based on the given topic. 
			Focus on clarity, accuracy, and relevance while writing. 
			Ensure that the content is easy to understand and effectively communicates the key points derived from the research findings.
			""")
	String write ( ResearchResult findings );
}
