package com.dep.agents;
import java.util.List;
import com.dep.dtos.ResearchResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WriterAgent {
	@SystemMessage("""
			You are a writer agent. Your task is to take the research findings provided by the researcher agent and create a well-structured and coherent piece of content. 
			Use the findings to craft an informative and engaging article, report, or summary based on the given topic. 
			Focus on clarity, accuracy, and relevance while writing. 
			Ensure that the content is easy to understand and effectively communicates the key points derived from the research findings.
			""")
	String write ( ResearchResult findings );

	@SystemMessage("""
			You are a writer agent. You have been given a previous report that was reviewed and found to have issues.
			Your task is to revise the report by addressing each of the identified issues.
			
			Rules:
			- Fix every issue listed, do not ignore any
			- Use the original research findings as your source of truth for any missing or incorrect data
			- Keep the parts of the previous report that were not flagged as problematic
			- Do not fabricate data that is not present in the research findings
			- Maintain a clear, professional, and well-structured format
    """)
	@UserMessage("Findings: {{findings}} \n\n Previous Report: {{previousReport}} \n\n Issues: {{issues}}")
	String revise(@V( "findings" ) ResearchResult findings, @V( "previousReport" ) String previousReport, @V( "issues" ) List<String> issues);
}
