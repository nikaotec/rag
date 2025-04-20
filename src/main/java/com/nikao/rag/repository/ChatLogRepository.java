package com.nikao.rag.repository;

import com.nikao.rag.model.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {}