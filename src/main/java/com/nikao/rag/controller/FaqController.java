package com.nikao.rag.controller;

import java.util.List;

import com.nikao.rag.model.Faq;
import com.nikao.rag.service.FaqService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/faq")
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @PostMapping
    public ResponseEntity<Faq> createFaq(@RequestBody Faq faq) {
        Faq createdFaq = faqService.create(faq);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFaq);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Faq> getFaqById(@PathVariable Long id){
        return faqService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Faq>> getAllFaqs() {
        return ResponseEntity.ok(faqService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Faq> updateFaq(@PathVariable Long id, @RequestBody Faq faq) {
        return faqService.findById(id).map(existing -> ResponseEntity.ok(faqService.update(faq))).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) { faqService.delete(id);
        return ResponseEntity.noContent().build();
    }
}