package com.dep.listeners;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class OtelChatModelListener implements ChatModelListener {

	private static final String SPAN_KEY = "otel.span";
	private static final String SCOPE_KEY = "otel.scope";

	private final Tracer tracer;

	public OtelChatModelListener(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public void onRequest( ChatModelRequestContext ctx) {
		System.out.println(">>> OtelChatModelListener.onRequest FIRED");
		Span span = tracer.spanBuilder("chat " + ctx.chatRequest().modelName())
				.setAttribute("gen_ai.system", "aws.bedrock")
				.setAttribute("gen_ai.request.model", String.valueOf(ctx.chatRequest().modelName()))
				.setAttribute("gen_ai.operation.name", "chat")
				.setAttribute("gen_ai.prompt", ctx.chatRequest().messages().toString())
				.setAttribute("input.value", ctx.chatRequest().messages().toString())
				.setAttribute("input.mime_type", "text/plain")
				.setAttribute("openinference.span.kind", "LLM")
				.startSpan();
		Scope scope = span.makeCurrent();
		ctx.attributes().put(SPAN_KEY, span);
		ctx.attributes().put(SCOPE_KEY, scope);
	}

	@Override
	public void onResponse( ChatModelResponseContext ctx) {
		Span span = (Span) ctx.attributes().get(SPAN_KEY);
		Scope scope = (Scope) ctx.attributes().get(SCOPE_KEY);
		if (span == null) return;
		try {
			var usage = ctx.chatResponse().tokenUsage();
			if (usage != null) {
				span.setAttribute("gen_ai.usage.input_tokens", usage.inputTokenCount());
				span.setAttribute("gen_ai.usage.output_tokens", usage.outputTokenCount());
				span.setAttribute("output.value", String.valueOf(ctx.chatResponse().aiMessage().text()));
				span.setAttribute("output.mime_type", "text/plain");
			}
			span.setAttribute("gen_ai.completion", String.valueOf(ctx.chatResponse().aiMessage().text()));
		} finally {
			if (scope != null) scope.close();
			span.end();
		}
	}

	@Override
	public void onError( ChatModelErrorContext ctx) {
		Span span = (Span) ctx.attributes().get(SPAN_KEY);
		Scope scope = (Scope) ctx.attributes().get(SCOPE_KEY);
		if (span == null) return;
		try {
			span.recordException(ctx.error());
			span.setStatus( StatusCode.ERROR);
		} finally {
			if (scope != null) scope.close();
			span.end();
		}
	}

}
