package com.nikao.rag.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nikao.rag.service.ChatBot;
import com.nikao.rag.service.ClusterService;
import com.nikao.rag.service.EmbeddingService;
import com.nikao.rag.service.FileProcessingService;
import com.nikao.rag.service.ImageGenerator;
import com.nikao.rag.service.RagService;

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

    @Autowired
    private final ClusterService clusterService;

    @Autowired
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

    // @PostMapping("/rag/multi-cloud")
    // public Mono<String> generateWithEmbeddings(@RequestParam("prompt") String prompt,
    //         @RequestParam(value = "cloudFileUrl", required = false) String cloudFileUrl,
    //         @RequestParam(value = "url", required = false) String url) {
    //     StringBuilder fullContext = new StringBuilder();

    //     // 🧪 Teste com PDF fixo (remova isso em produção)
    //     cloudFileUrl = "https://raw.githubusercontent.com/nikaotec/teste_chat/main/adventista.pdf";

    //     try {
    //         // 🗂️ 1. Carregar conteúdo do arquivo e/ou URL
    //         if (cloudFileUrl != null && !cloudFileUrl.isBlank()) {
    //             InputStream inputStream = new URL(cloudFileUrl).openStream();
    //             String fileText = fileProcessingService.extractText(inputStream, cloudFileUrl);
    //             fullContext.append(fileText).append("\n\n");
    //         }

    //         if (url != null && !url.isBlank()) {
    //             String urlText = fileProcessingService.crawlWebsite(url, 5);
    //             fullContext.append(urlText).append("\n\n");
    //         }

    //         if (fullContext.isEmpty()) {
    //             return Mono.just("Nenhuma fonte de informação fornecida (arquivo ou URL).");
    //         }

    //         // Palavras-chave temáticas para dividir o conteúdo
    //         List<String> palavrasChave = List.of(
    //                 "Doutrina", "Crença", "História", "Endereço",
    //                 "Fundamentos", "Missão", "Objetivo", "Sábado",
    //                 "Estado dos Mortos", "Segunda Vinda", "Profecia", "Cristo");

    //         // Chunking inteligente
    //         List<String> chunks = fileProcessingService.splitSmartChunks(fullContext.toString(), palavrasChave);

    //         // 🧠 3. Gerar embeddings e ranquear com base no prompt
    //         return embeddingService.rankChunks(chunks, prompt)
    //                 .flatMap(relevantChunks -> {
    //                     // 📚 4. Montar contexto a partir dos chunks ranqueados
    //                     String context = relevantChunks.stream()
    //                             .map(EmbeddingService.ScoredChunk::text)
    //                             .collect(Collectors.joining("\n\n"));

    //                     // 🤖 5. Passar contexto + prompt para o modelo
    //                     return chatBot.generateWithContext(context, prompt);
    //                 });

    //     } catch (Exception e) {
    //         return Mono.just("Erro ao processar conteúdo: " + e.getMessage());
    //     }
    // }

    /**
    /**
     * Endpoint principal para responder usando cluster mais relevante.
     */
    @PostMapping("/pergunta")
    public Mono<String> responder(@RequestParam("prompt") String prompt) {
        return clusterService.buscarChunksMaisRelevantes(prompt)
                .flatMap(chunks -> {
                    String contexto = String.join("\n\n", chunks);
                    if (contexto.isBlank()) {
                        return Mono.just("Não há informação suficiente no conteúdo para responder com precisão.");
                    }
                    return chatBot.generateWithContext(contexto, prompt);
                });
    }

    /**
     * Endpoint de diagnóstico: retorna os chunks usados e cluster escolhido.
     */
    @GetMapping("/debug-cluster")
    public Mono<String> diagnostico(@RequestParam("prompt") String prompt) {
        return clusterService.buscarChunksMaisRelevantes(prompt)
                .map(chunks -> {
                    String resultado = chunks.stream()
                            .map(chunk -> "🔹 " + chunk.replace("\n", " ").substring(0, Math.min(250, chunk.length()))
                                    + "...")
                            .collect(Collectors.joining("\n\n"));
                    return "🧠 Diagnóstico do cluster para o prompt:\n\n" +
                            prompt + "\n\n🔎 Chunks usados:\n\n" + resultado;
                });
    }

    @PostMapping(value = "/fonte", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> processarFontes(
            @RequestParam(required = false) List<String> urls,
            @RequestPart(required = false) List<MultipartFile> arquivos,
            @RequestParam(required = false) String textoManual
    ) {

        urls.add("https://raw.githubusercontent.com/nikaotec/teste_chat/main/adventista.pdf");

        return ragService.processarFontes(urls, arquivos, textoManual)
                .thenReturn("✅ Fontes processadas e clusters atualizados com sucesso!");
    }

}
