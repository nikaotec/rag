package com.nikao.rag.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nikao.rag.model.ChatLog;
import com.nikao.rag.service.ChatLogService;

@RestController
@RequestMapping("/chat-log")
public class ChatLogController {

    private final ChatLogService chatLogService;

    public ChatLogController(ChatLogService chatLogService) {
        this.chatLogService = chatLogService;
    }

    @PostMapping
    public ResponseEntity<ChatLog> createChatLog(@RequestBody ChatLog chatLog) {
        ChatLog createdChatLog = chatLogService.create(chatLog);
        return new ResponseEntity<>(createdChatLog, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatLog> findChatLogById(@PathVariable Long id) {
        Optional<ChatLog> chatLog = chatLogService.findById(id);
        return chatLog.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ChatLog>> findAllChatLogs() {
        List<ChatLog> chatLogs = chatLogService.findAll();
        return new ResponseEntity<>(chatLogs, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChatLog> updateChatLog(@PathVariable Long id, @RequestBody ChatLog chatLog) {
        Optional<ChatLog> existingChatLog = chatLogService.findById(id);
        if (existingChatLog.isPresent()) {
            chatLog.setChatLogId(id);
            ChatLog updatedChatLog = chatLogService.update(chatLog);
            return new ResponseEntity<>(updatedChatLog, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatLog(@PathVariable Long id) {
        Optional<ChatLog> chatLog = chatLogService.findById(id);
        if (chatLog.isPresent()) {
            chatLogService.delete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return ResponseEntity.notFound().build();
        }
    }    
}