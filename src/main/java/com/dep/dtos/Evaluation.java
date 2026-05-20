package com.dep.dtos;
import java.util.List;
import com.dep.enums.IssueType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Evaluation {
	private boolean acceptable;
	private List<Issue> issues;

	public List<String> getIssuesByType( IssueType type) {
		return issues.stream()
				.filter(issue -> issue.getType() == type)
				.map(Issue::getDescription)
				.toList();
	}
}
