package com.dep.dtos;
import com.dep.enums.IssueType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Issue {
	private IssueType type;
	private String description;
}
