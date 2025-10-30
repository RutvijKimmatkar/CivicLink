package com.demo.demo.dto;

import java.time.LocalDateTime;

public class PublicComplaintDto {
    private Long id;
    private String category; // enum.toString()
    private String description; // truncated by frontend if needed
    private Double latitude;
    private Double longitude;
    private String status;
    private LocalDateTime createdAt;
    private String photo; // optional public URL (or null)

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}