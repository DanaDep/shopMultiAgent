package com.dep.services;
import com.dep.agents.CriticAgent;
import com.dep.agents.ResearcherAgent;
import com.dep.agents.WriterAgent;
import com.dep.dtos.Evaluation;
import com.dep.dtos.ResearchResult;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {

	private final ResearcherAgent researcherAgent;
	private final WriterAgent writerAgent;
	private final CriticAgent criticAgent;

	public String executePipeline(String topic) {
		ResearchResult findings = researcherAgent.research(topic);
		log.debug("Research completed. Findings: size{}", findings.getFindings().size());

		String result = writerAgent.write(findings);
		log.debug("Writing completed. Result: {}", result);

		int maxRetries = 3;
		for(int i = 0; i < maxRetries; i++) {
			 Evaluation evaluation = criticAgent.evaluate(result, findings);
			 log.info("Evaluation completed. Acceptable: {}, Issues: {}", evaluation.isAcceptable(), evaluation.getIssues());

			 if(evaluation.isAcceptable()) {
				return result;
			 }
			 result = writerAgent.revise(findings, result, evaluation.getIssues());
		}

		return result;
	}
}
