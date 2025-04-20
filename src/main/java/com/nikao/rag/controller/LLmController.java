package com.nikao.rag.controller;

import java.io.IOException;
import com.nikao.rag.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.nikao.rag.service.ChatBot;
import com.nikao.rag.service.ClusterService;
import com.nikao.rag.service.FileProcessingService;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequestMapping("/llm")
public class LLmController {

    private final ChatBot chatBot;

    private final ImageGenerator imageGenerator;
    private final FileProcessingService fileProcessingService;
    private final ClusterService clusterService;
    private final RagService ragService;

    public LLmController(ImageGenerator imageGenerator, ChatBot chatBot, FileProcessingService fileProcessingService,
            ClusterService clusterService, RagService ragService) {
        this.imageGenerator = imageGenerator;
        this.chatBot = chatBot;
        this.fileProcessingService = fileProcessingService;
        this.clusterService = clusterService;
        this.ragService = ragService;
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @PostMapping("/text")
    public ResponseEntity<String> generateText(@RequestParam String prompt) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("this endpoint was deprecated");
    }


    @PostMapping("/image")    
    public ResponseEntity<String> generateImage(@RequestParam String prompt) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("this endpoint was deprecated");

    }

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
    public ResponseEntity<String> generateFromOptionalFileAndUrl(


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
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("this endpoint was deprecated");
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("this endpoint was deprecated");
        }
    }


    /*
    /**
     * Endpoint principal para responder usando cluster mais relevante.
     */
    @PostMapping("/pergunta")
    public Mono<String> responder(@RequestParam("prompt") String prompt, @RequestParam Long companyId) {

        return clusterService.buscarChunksMaisRelevantes(prompt, companyId)
                .flatMap(chunks -> {
                    String contexto = String.join("\n\n", chunks);
                    if (contexto.isBlank()) {
                        return Mono.just("Não há informação suficiente no conteúdo para responder com precisão.");
                    }
                    return chatBot.generateWithContext(contexto, prompt);
                });
    }

    /*
     * Endpoint de diagnóstico: retorna os chunks usados e cluster escolhido.
     */
    @GetMapping("/debug-cluster")
    public Mono<String> diagnostico(@RequestParam("prompt") String prompt, @RequestParam Long companyId) {
        return clusterService.buscarChunksMaisRelevantes(prompt, companyId)
                .map(chunks -> {
                    String resultado = chunks.stream()
                            .map(chunk -> "🔹 " + chunk.replace("\n", " ").substring(0, Math.min(250, chunk.length()))
                                    + "...")
                            .collect(Collectors.joining("\n\n"));
                    return "🧠 Diagnóstico do cluster para o prompt:\n\n" +
                            prompt + "\n\n🔎 Chunks usados:\n\n" + resultado;
                });
    }

    @PostMapping(value = "/company/{companyId}/fonte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> processCompanyFontes(
            @RequestParam(required = false) List<String> urls,
            @RequestPart(required = false) List<MultipartFile> arquivos,
            @RequestParam(required = false) String textoManual,
            @PathVariable Long companyId
    ) {

        return ragService.processCompanyFontes(companyId, urls, arquivos, textoManual)
                .thenReturn("✅ Fontes processadas e clusters atualizados com sucesso!");
    }

}
