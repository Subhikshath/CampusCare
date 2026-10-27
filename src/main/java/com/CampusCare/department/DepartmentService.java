package com.CampusCare.department;

import com.CampusCare.common.DuplicateResourceException;
import com.CampusCare.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	@Transactional
	public DepartmentResponse create(DepartmentRequest request) {
		String name = normalize(request.getName());
		String code = normalize(request.getCode());

		if (departmentRepository.existsByName(name)) {
			throw new DuplicateResourceException("Department name already exists");
		}
		if (departmentRepository.existsByCode(code)) {
			throw new DuplicateResourceException("Department code already exists");
		}

		Department department = Department.builder()
				.name(name)
				.code(code)
				.enabled(true)
				.build();

		return DepartmentResponse.from(departmentRepository.save(department));
	}

	@Transactional(readOnly = true)
	public List<DepartmentResponse> findAll() {
		return departmentRepository.findAll().stream()
				.map(DepartmentResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DepartmentResponse> findEnabled() {
		return departmentRepository.findAll().stream()
				.filter(Department::isEnabled)
				.map(DepartmentResponse::from)
				.toList();
	}

	@Transactional
	public DepartmentResponse update(Long id, DepartmentRequest request) {
		Department department = getById(id);
		String name = normalize(request.getName());
		String code = normalize(request.getCode());

		if (departmentRepository.existsByNameAndIdNot(name, id)) {
			throw new DuplicateResourceException("Department name already exists");
		}
		if (departmentRepository.existsByCodeAndIdNot(code, id)) {
			throw new DuplicateResourceException("Department code already exists");
		}

		department.setName(name);
		department.setCode(code);
		return DepartmentResponse.from(departmentRepository.save(department));
	}

	@Transactional
	public void disable(Long id) {
		Department department = getById(id);
		department.setEnabled(false);
		departmentRepository.save(department);
	}

	private Department getById(Long id) {
		return departmentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
	}

	private String normalize(String value) {
		return value.trim();
	}
}
