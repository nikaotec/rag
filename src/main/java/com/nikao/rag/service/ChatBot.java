package com.nikao.rag.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nikao.rag.records.HuggingFaceResponse;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class ChatBot {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(ChatBot.class);

    public ChatBot(@Value("${huggingface.text.api.url}") String apiUrl,
            @Value("${huggingface.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl) // Set the base URL for the API
                .defaultHeader("Authorization", "Bearer " + apiKey) // Add API key to the header
                .build();
    }

    // Method to generate text using Hugging Face's Inference API
    public Mono<String> generateText(String prompt) {
        // Validate the input prompt
        if (prompt == null || prompt.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Prompt must not be null or empty"));
        }

        // Create the request body with the prompt
        Map<String, String> body = Collections.singletonMap("inputs", prompt);

        // Make a POST request to the Hugging Face API
        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(HuggingFaceResponse.class)
                .next()
                .map(HuggingFaceResponse::generated_text)
                .onErrorResume(e -> Mono.just("Erro: " + e.getMessage()));
    }

    public Mono<String> generateWithContext(String context, String prompt) {
        // Trunca o contexto para evitar excesso de tokens
        context = context.length() > 3000 ? context.substring(0, 3000) : context;

        String fullPrompt = String.format(
                """
                        Você é um assistente de atendimento profissional. Com base unicamente nas informações abaixo, responda à pergunta do usuário.

                        ❗IMPORTANTE: Responda apenas com base no conteúdo fornecido.
                        ❗Se não houver informação suficiente para responder com certeza, diga claramente: "Não há informação suficiente no conteúdo para responder com precisão."

                        [CONTEÚDO]
                        %s

                        [PERGUNTA]
                        %s

                        [RESPOSTA]
                        """,
                context, prompt);

        Map<String, String> body = Collections.singletonMap("inputs", fullPrompt);

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(HuggingFaceResponse.class)
                .next()
                .map(HuggingFaceResponse::generated_text)
                .map(ChatBot::limparResposta)
                .doOnNext(resposta -> logger.info("Resposta final (limpa): {}", resposta))
                .onErrorResume(e -> {
                    logger.error("Erro durante chamada à Hugging Face", e);
                    return Mono.just("Erro: " + e.getMessage());
                });
    }

    public static String limparResposta(String respostaGerada) {
        int index = respostaGerada.lastIndexOf("[RESPOSTA]");
        if (index != -1) {
            return respostaGerada.substring(index + "[RESPOSTA]".length()).strip();
        }
        return respostaGerada.strip();
    }

}
