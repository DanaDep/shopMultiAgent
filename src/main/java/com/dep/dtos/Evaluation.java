package com.dep.dtos;
import java.util.List;
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
	private List<String> issues;
}
