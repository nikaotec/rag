package com.nikao.rag.service;

import com.nikao.rag.model.Company;
import com.nikao.rag.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company create(Company company) {
        return companyRepository.save(company);
    }

    public Optional<Company> findById(Long id) {
        return companyRepository.findById(id);
    }

    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    public Company update(Company company) {
        return companyRepository.save(company);
    }

    public void delete(Long id) {
        companyRepository.deleteById(id);
    }
}