package com.nikao.rag.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Mono;

@Service
public class RagService {

    private final FileProcessingService fileProcessingService;
    private final ClusterService clusterService;

    public RagService(FileProcessingService fps, ClusterService cs) {
        this.fileProcessingService = fps;
        this.clusterService = cs;
    }

    public Mono<Void> processarFontes(List<String> urls, List<MultipartFile> arquivos, String textoManual) {
        return Mono.fromCallable(() -> {
            StringBuilder fullText = new StringBuilder();

            if (urls != null) {
                for (String url : urls) {
                    try {
                        System.out.println("🌐 Processando URL: " + url);
                        fullText.append(fileProcessingService.extractTextFromUrl(url)).append("\n\n");
                    } catch (Exception e) {
                        System.err.println("Erro ao extrair de URL: " + e.getMessage());
                    }
                }
            }

            if (arquivos != null) {
                for (MultipartFile file : arquivos) {
                    try {
                        System.out.println("📎 Processando arquivo: " + file.getOriginalFilename());
                        fullText.append(fileProcessingService.extractText(file)).append("\n\n");
                    } catch (Exception e) {
                        System.err.println("Erro ao extrair de arquivo: " + e.getMessage());
                    }
                }
            }

            if (textoManual != null && !textoManual.isBlank()) {
                System.out.println("📝 Processando texto manual");
                fullText.append(textoManual).append("\n\n");
            }

            String textoUnificado = fullText.toString().strip();

            if (!textoUnificado.isBlank()) {
                clusterService.recriarClustersComTexto(textoUnificado);
            }

            return true;
        }).then();
    }
}
