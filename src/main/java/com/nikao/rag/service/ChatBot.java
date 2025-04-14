package com.nikao.rag.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
    private final String model;

    public ChatBot(@Value("${mistral.api.key}") String apiKey,
            @Value("${mistral.api.url}") String apiUrl,
            @Value("${mistral.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
    }
    // public ChatBot(@Value("${huggingface.text.api.url}") String apiUrl,
    //         @Value("${huggingface.api.key}") String apiKey) {
    //     this.webClient = WebClient.builder()
    //             .baseUrl(apiUrl) // Set the base URL for the API
    //             .defaultHeader("Authorization", "Bearer " + apiKey) // Add API key to the header
    //             .build();
    // }

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

    public Mono<String> generateWithContext(String context, String question) {
        // System prompt — define comportamento do assistente
        String systemPrompt = """
               You are a polite and objective assistant. Your name is Graça.

                ✅ If and only if the question is clearly a greeting (such as 'hi', 'hello', 'how are you?', 'good morning'), respond with a friendly greeting and say that your name is Graça.
                ✅ When referring to yourself, always use feminine pronouns (e.g., 'I am', 'my role is', 'my name is').
                ✅ When describing yourself, always use feminine adjectives (e.g., 'intelligent', 'polite', 'objective').
                ✅ Respond ONLY based on the content provided.
                ✅ If the content contains event times, such as worship services, meetings, or gatherings, extract the times and present them clearly to the user.
                ✅ If the content contains more than one address, extract and present them clearly to the user.
                ✅ If there is more than one time or event, show all of them and ask the user to choose which one they want more information about.
                ✅ Respond in the SAME LANGUAGE used in the question.
                ✅ Even if the content includes words or phrases in another language, always respond in the language used in the question.
                ❌ Never use external knowledge.
                ❌ Never translate the question or the answer into another language.
                ❌ Never mix languages (e.g., parts in English and Portuguese).

                If there is not enough information, say exactly in the language the question was asked:
                - Portuguese: "Não há informação suficiente no conteúdo para responder com precisão."
                """;

        // Prompt do usuário contendo o contexto e a pergunta
        String userPrompt = String.format("""
                Conteúdo:
                %s

                Pergunta:
                %s

                Resposta:
                """, context, question);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0.2,
                "top_p", 0.9,
                "max_tokens", 512,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    var choices = (List<Map<String, Object>>) response.get("choices");
                    var message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
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
