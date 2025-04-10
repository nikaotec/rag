package com.nikao.rag.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nikao.rag.service.ChatBot;
import com.nikao.rag.service.FileProcessingService;
import com.nikao.rag.service.ImageGenerator;

import reactor.core.publisher.Mono;

import org.springframework.ui.Model;

@RestController
@RequestMapping("/llm")
public class LLmController {

    @Autowired
    private ChatBot chatBot;

    @Autowired
    private final ImageGenerator imageGenerator;

    @Autowired
    private final FileProcessingService fileProcessingService;

    public LLmController(ImageGenerator imageGenerator, ChatBot chatBot, FileProcessingService fileProcessingService) {
        this.imageGenerator = imageGenerator;
        this.chatBot = chatBot;
        this.fileProcessingService = fileProcessingService;
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    // This class is a placeholder for the LLM (Large Language Model) controller.
    // You can add methods to handle requests related to LLM functionalities here.

    // Example method to handle a request
    @PostMapping("/text")
    public Mono<ResponseEntity<String>> generateText(@RequestParam String prompt) {
        return chatBot.generateText(prompt)
                .map(response -> ResponseEntity.ok("Resposta do modelo: " + response))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(500)
                        .body("Erro ao gerar resposta: " + error.getMessage())));
    }

    // @PostMapping("/text")
    // public Mono<String> generate(Model model, @RequestParam String prompt) {
    // return chatBot.generateText(prompt)
    // .doOnNext(response -> model.addAttribute("response", response))
    // .thenReturn("index");
    // }

    @PostMapping("/image")
    public Mono<ResponseEntity<byte[]>> generateImage(@RequestParam String prompt) {
        return imageGenerator.generateImage(prompt)
                .map(image -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=image.png")
                        .contentType(MediaType.IMAGE_PNG)
                        .body(image))
                .defaultIfEmpty(ResponseEntity.status(500).body(null));
    }

    // @PostMapping("/image")
    // public Mono<ResponseEntity<byte[]>> generateImage(@RequestParam String
    // prompt) {
    // return imageGenerator.generateImage(prompt)
    // .map(image -> ResponseEntity.ok()
    // .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=image.png")
    // .contentType(MediaType.IMAGE_PNG)
    // .body(image))
    // .defaultIfEmpty(ResponseEntity.status(500).body(null));
    // }

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> uploadAndAsk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt) {

        try {
            String context = fileProcessingService.extractText(file);
            return chatBot.generateWithContext(context, prompt)
                    .map(response -> ResponseEntity.ok("Resposta baseada no conteúdo do arquivo: " + response));
        } catch (IOException e) {
            return Mono.just(ResponseEntity.status(500).body("Erro ao processar arquivo: " + e.getMessage()));
        }
    }

    @PostMapping("/url")
    public Mono<String> generateFromUrl(@RequestParam("url") String url,
            @RequestParam("prompt") String prompt) {
        try {
            String context = fileProcessingService.extractTextFromUrl(url);

            // (Opcional) limitar tamanho do conteúdo:
            context = context.length() > 3000 ? context.substring(0, 3000) : context;

            return chatBot.generateWithContext(context, prompt);
        } catch (IOException e) {
            return Mono.just("Erro ao acessar a URL: " + e.getMessage());
        }
    }

    @PostMapping("/url/deep")
    public Mono<String> generateFromDeepUrl(@RequestParam("url") String url,
            @RequestParam("prompt") String prompt) {
        try {
            String context = fileProcessingService.crawlWebsite(url, 5); // limite de páginas
            context = context.length() > 3000 ? context.substring(0, 3000) : context;
            return chatBot.generateWithContext(context, prompt);
        } catch (IOException e) {
            return Mono.just("Erro ao acessar as páginas: " + e.getMessage());
        }
    }

    @PostMapping("/rag/multi")
    public Mono<String> generateFromOptionalFileAndUrl(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam("prompt") String prompt) {
        StringBuilder fullContext = new StringBuilder();

        try {
            if (file != null && !file.isEmpty()) {
                String fileText = fileProcessingService.extractText(file);
                fullContext.append(fileText).append("\n\n");
            }

            if (url != null && !url.isBlank()) {
                String urlText = fileProcessingService.crawlWebsite(url, 5);
                fullContext.append(urlText).append("\n\n");
            }

            if (fullContext.isEmpty()) {
                return Mono.just("Nenhuma fonte de informação fornecida (arquivo ou URL).");
            }

            String context = fullContext.toString();
            context = context.length() > 3000 ? context.substring(0, 3000) : context;

            return chatBot.generateWithContext(context, prompt);
        } catch (IOException e) {
            return Mono.just("Erro ao processar conteúdo: " + e.getMessage());
        }
    }

    @PostMapping("/rag/multi-cloud")
    public Mono<String> generateFromCloudAndUrl(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "cloudFileUrl", required = false) String cloudFileUrl) {

        StringBuilder context = new StringBuilder();

        cloudFileUrl = "https://raw.githubusercontent.com/nikaotec/teste_chat/main/adventista.pdf";

        try {
            // Busca o conteúdo do arquivo via URL (GitHub, Dropbox, etc)
            if (cloudFileUrl != null && !cloudFileUrl.isBlank()) {
                InputStream inputStream = new URL(cloudFileUrl).openStream();
                String fileText = fileProcessingService.extractText(inputStream, cloudFileUrl);
                context.append(fileText).append("\n\n");
            }

            // Também adiciona conteúdo de uma URL (scraping)
            if (url != null && !url.isBlank()) {
                String webText = fileProcessingService.crawlWebsite(url, 5);
                context.append(webText).append("\n\n");
            }

            if (context.isEmpty()) {
                return Mono.just("Nenhuma fonte válida foi fornecida.");
            }

            String safeContext = context.length() > 3000 ? context.substring(0, 3000) : context.toString();
            return chatBot.generateWithContext(safeContext, prompt);

        } catch (Exception e) {
            return Mono.just("Erro ao processar: " + e.getMessage());
        }
    }

}
