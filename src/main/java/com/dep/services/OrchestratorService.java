package com.dep.services;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.dep.agents.CriticAgent;
import com.dep.agents.ResearcherAgent;
import com.dep.agents.WriterAgent;
import com.dep.dtos.Evaluation;
import com.dep.dtos.ResearchResult;
import com.dep.enums.IssueType;
import dev.langchain4j.service.output.OutputParsingException;
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
		Span span = Span.current();
		span.setAttribute("openinference.span.kind", "CHAIN");
		span.setAttribute("input.value", topic);
		span.setAttribute("input.mime_type", "text/plain");

		ResearchResult findings;
		try {
			findings = research(topic);
		} catch (OutputParsingException e) {
			// Backstop: the schema now has an explicit refusal path, but a probabilistic model
			// can still break the JSON contract entirely — that must never surface as a 500.
			log.info("Researcher broke the output contract; returning its raw reply as the response");
			return refuse(span, extractRawModelReply(e).orElse(FALLBACK_REFUSAL));
		}
		if (findings.isUnableToAnswer()) {
			log.info("Researcher reported the question as unanswerable with the available tools");
			return refuse(span, Optional.ofNullable(findings.getUnableToAnswerReason())
					.filter(r -> !r.isBlank()).orElse(FALLBACK_REFUSAL));
		}
		String result = write(findings);

		for (int i = 0; i < 3; i++) {
			Evaluation evaluation = evaluate(topic, result, findings);
			if (evaluation.isAcceptable()) break;

			List<String> researchIssues = evaluation.getIssuesByType(IssueType.RESEARCH);
			if (!researchIssues.isEmpty()) {
				findings = deepResearch(topic, findings, researchIssues);
				result = write(findings);
			} else {
				result = revise(findings, result, evaluation.getIssuesByType(IssueType.WRITING));
			}
		}

		span.setAttribute("output.value", result);
		span.setAttribute("output.mime_type", "text/plain");
		return result;
	}

	private static final String FALLBACK_REFUSAL =
			"I'm sorry, I could not answer this request with the available shop data tools.";

	private String refuse(Span span, String message) {
		span.setAttribute("output.value", message);
		span.setAttribute("output.mime_type", "text/plain");
		return message;
	}

	// OutputParsingException has no getter for the raw model reply; the message embeds it as
	// `Failed to parse "<text>" (base64: "<b64>") into <type>` — the base64 group is the only
	// quote-safe way to recover what the model actually said (usually a polite refusal).
	private static final Pattern RAW_REPLY_BASE64 = Pattern.compile("\\(base64: \"([A-Za-z0-9+/=]+)\"\\)");

	private static Optional<String> extractRawModelReply(OutputParsingException e) {
		String message = e.getMessage();
		if (message == null) return Optional.empty();
		Matcher m = RAW_REPLY_BASE64.matcher(message);
		if (!m.find()) return Optional.empty();
		try {
			return Optional.of(new String(Base64.getDecoder().decode(m.group(1)), StandardCharsets.UTF_8));
		} catch (IllegalArgumentException invalidBase64) {
			return Optional.empty();
		}
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
