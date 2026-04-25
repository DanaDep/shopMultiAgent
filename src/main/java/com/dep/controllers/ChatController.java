package com.dep.controllers;
import com.dep.services.OrchestratorService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller("/api")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

	private final OrchestratorService orchestratorService;

	@Post("/chat")
	public HttpResponse<String> chat(@Body String message) {
		String topic = message != null ? message.trim() : null;

		log.info("Message received: {}", message);

		if (topic == null || topic.isBlank()) {
			log.warn("Message cannot be empty");
			return HttpResponse.badRequest("Message cannot be empty");
		}

		try {
			String result = orchestratorService.executePipeline(topic);
			return HttpResponse.ok(result);
		} catch (Exception e) {
			log.error("Error processing chat request", e);
			return HttpResponse.serverError("An error occurred while processing the request");
		}
	}

}
