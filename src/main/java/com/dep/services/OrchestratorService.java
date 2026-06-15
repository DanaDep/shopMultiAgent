package com.dep.services;
import java.util.List;
import com.dep.agents.CriticAgent;
import com.dep.agents.ResearcherAgent;
import com.dep.agents.WriterAgent;
import com.dep.dtos.Evaluation;
import com.dep.dtos.ResearchResult;
import com.dep.enums.IssueType;
import io.micronaut.tracing.annotation.NewSpan;
import io.opentelemetry.api.trace.Span;
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

	@NewSpan
	public String executePipeline(String topic) {
		Span.current().setAttribute("openinference.span.kind", "CHAIN");
		ResearchResult findings = research(topic);
		String result = write(findings);

		for (int i = 0; i < 3; i++) {
			Evaluation evaluation = evaluate(topic, result, findings);
			if (evaluation.isAcceptable()) return result;

			List<String> researchIssues = evaluation.getIssuesByType(IssueType.RESEARCH);
			if (!researchIssues.isEmpty()) {
				findings = deepResearch(topic, findings, researchIssues);
				result = write(findings);
			} else {
				result = revise(findings, result, evaluation.getIssuesByType(IssueType.WRITING));
			}
		}
		return result;
	}

	@NewSpan
	protected ResearchResult research(String topic) {
		Span.current().setAttribute("openinference.span.kind", "AGENT");
		Span.current().setAttribute("agent.name", "researcher");
		return researcherAgent.research(topic);
	}

	@NewSpan
	protected ResearchResult deepResearch(String topic, ResearchResult prev, List<String> issues) {
		Span.current().setAttribute("openinference.span.kind", "AGENT");
		Span.current().setAttribute("agent.name", "researcher");
		return researcherAgent.deepResearch(topic, prev, issues);
	}

	@NewSpan
	protected String write(ResearchResult findings) {
		Span.current().setAttribute("openinference.span.kind", "AGENT");
		Span.current().setAttribute("agent.name", "writer");
		return writerAgent.write(findings);
	}

	@NewSpan
	protected String revise(ResearchResult findings, String draft, List<String> issues) {
		Span.current().setAttribute("openinference.span.kind", "AGENT");
		Span.current().setAttribute("agent.name", "writer");
		return writerAgent.revise(findings, draft, issues);
	}

	@NewSpan
	protected Evaluation evaluate(String topic, String result, ResearchResult findings) {
		Span.current().setAttribute("openinference.span.kind", "AGENT");
		Span.current().setAttribute("agent.name", "critic");
		return criticAgent.evaluate(topic, result, findings);
	}
}
