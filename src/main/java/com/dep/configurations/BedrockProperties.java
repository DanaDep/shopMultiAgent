package com.dep.configurations;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("langchain4j.bedrock.chat-model")
public class BedrockProperties {

	private String modelId;
	private String region;
	private Duration timeout;
	private int maxOutputTokens;
	private double temperature;

	public String getModelId() { return modelId; }
	public void setModelId(String modelId) { this.modelId = modelId; }

	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }

	public Duration getTimeout() { return timeout; }
	public void setTimeout(Duration timeout) { this.timeout = timeout; }

	public int getMaxOutputTokens() { return maxOutputTokens; }
	public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

	public double getTemperature() { return temperature; }
	public void setTemperature(double temperature) { this.temperature = temperature; }

}
