package com.nikao.rag.repository;

import com.nikao.rag.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;public interface EmployeeRepository extends JpaRepository<Employee, Long> {}

