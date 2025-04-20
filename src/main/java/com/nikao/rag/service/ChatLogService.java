package com.nikao.rag.service;

import com.nikao.rag.model.ChatLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatLogService {

    private final ChatLogRepository chatLogRepository;

    public ChatLogService(ChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    public ChatLog create(ChatLog chatLog) {
        return chatLogRepository.save(chatLog);
    }

    public Optional<ChatLog> findById(Long id) {
        return chatLogRepository.findById(id);
    }

    public List<ChatLog> findAll() {
        return chatLogRepository.findAll();
    }

    public ChatLog update(ChatLog chatLog) {
        return chatLogRepository.save(chatLog);
    }

    public void delete(Long id) {
        chatLogRepository.deleteById(id);
    }
}