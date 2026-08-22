package com.yahya.uptime_monitor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "websites")
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String url;

    private String status;
    

    public Website() {
    }

    public Website(String name, String url) {
        this.name = name;
        this.url = url;
    }
public String getStatus() {
    return status;
}
public void setStatus(String status) {
    this.status = status;
}
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}