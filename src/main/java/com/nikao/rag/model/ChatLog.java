package com.nikao.rag.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "chat_log")
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatLogId;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private String message;

    private String response;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public Long getChatLogId() {
        return chatLogId;
    }

    public void setChatLogId(Long chatLogId) {
        this.chatLogId = chatLogId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}