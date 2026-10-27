package com.CampusCare.worker;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class WorkerAssignment {
	Long userId;
	String name;
	String collegeEmail;
	List<Long> departmentIds;
	boolean enabled;
}
