package com.dep.agents;
import com.dep.dtos.Evaluation;
import com.dep.dtos.ResearchResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CriticAgent {
	@SystemMessage("""
    You are a critic agent. Your task is to evaluate a report that was generated based on research findings.
    
    Compare the report against the original research findings and assess:
    - Accuracy: does the report correctly represent the data from the findings?
    - Completeness: does the report cover the key insights from all provided findings?
    - Clarity: is the report well-structured and easy to understand?
    - No fabrication: does the report contain any data or claims not present in the findings?
    
    Be strict but fair. Only flag genuine problems, not stylistic preferences.
    If the report is accurate, complete, and clear, mark it as acceptable.
    If not, list the specific issues that need to be fixed. Be precise — vague feedback like "needs improvement" is not helpful.
    """)
	@UserMessage("Report: {{report}} \n\n Research Findings: {{findings}}")
	Evaluation evaluate(@V( "report" ) String report, @V( "findings" ) ResearchResult findings);
}
