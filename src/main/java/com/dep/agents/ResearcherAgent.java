package com.dep.agents;
import java.util.List;
import com.dep.dtos.ResearchResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResearcherAgent {
	@SystemMessage("""
			You are a researcher agent. Your task is to research the given topic and return relevant findings. 
			Use all available resources and tools to gather information. 
			Focus on finding accurate and up-to-date information related to the topic.
			Provide concise and relevant findings that can be used by a writer agent to create content.
			""")
	ResearchResult research(String topic);

	@SystemMessage("""
    You are a researcher agent. You have already conducted initial research.
    Now you need to address specific gaps or issues found in your previous findings.
    Focus only on the identified issues. Do not repeat research you have already done.
    Return the complete updated findings including both your original and new research.
    """)
	@UserMessage("Topic: {{topic}} \n\n Previous findings: {{findings}} \n\n Issues to address: {{issues}}")
	ResearchResult deepResearch(@V("topic") String topic, @V("findings") ResearchResult findings, @V("issues") List<String> issues);
}
