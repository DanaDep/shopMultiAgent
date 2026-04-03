package com.dep.services;
import com.dep.agents.ResearcherAgent;
import com.dep.agents.WriterAgent;
import com.dep.dtos.ResearchResult;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class PipelineService {

	private final ResearcherAgent researcherAgent;
	private final WriterAgent writerAgent;

	public String executePipeline(String topic) {
		ResearchResult findings = researcherAgent.research(topic);
		log.debug("Research completed. Findings: size{}", findings.getFindings().size());

		String result = writerAgent.write(findings);
		log.debug("Writing completed. Result: {}", result);

		return result;
	}
}
