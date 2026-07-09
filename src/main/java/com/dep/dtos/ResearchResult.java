package com.dep.dtos;
import java.util.List;
import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ResearchResult {
	List<Finding> findings;

	@Description("Set to true ONLY when the question cannot be answered with the available tools. Never invent findings to avoid this.")
	private boolean unableToAnswer;

	@Description("When unableToAnswer is true: a polite explanation for the user of why the question cannot be answered and what kind of questions can be.")
	private String unableToAnswerReason;
}
