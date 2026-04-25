package com.dep.dtos;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Finding {
	private String source;
	private String summary;
	private String rawData;

}
