package com.nikao.rag.service;

import com.nikao.rag.model.Faq;
import com.nikao.rag.repository.FaqRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    public Faq create(Faq faq) {
        return faqRepository.save(faq);
    }

    public Optional<Faq> findById(Long id) {
        return faqRepository.findById(id);
    }

    public List<Faq> findAll() {
        return faqRepository.findAll();
    }

    public Faq update(Faq faq) {
        return faqRepository.save(faq);
    }

    public void delete(Long id) {
        faqRepository.deleteById(id);
    }
}