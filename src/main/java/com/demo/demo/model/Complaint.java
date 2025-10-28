package com.demo.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "c_id")
    private Long id;

    // ---- This is important: use JoinColumn for ManyToOne, NOT @Column ----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "u_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "c_category", nullable = false)
    private ComplaintCategory category;

    @Column(name = "c_desc", nullable = false, length = 2000)
    private String description;

    @Column(name = "c_photo")
    private String photo;

    @Column(name = "c_location", nullable = false)
    private String location;

    @Column(name = "c_loc_desc")
    private String locationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "c_status", nullable = false)
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "assigned_vendor_id")
    private Long assignedVendorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Notes provided by vendor after marking work done
    @Column(name = "vendor_notes", columnDefinition = "TEXT")
    private String vendorNotes;



    public Complaint() {}

    public Complaint(User user,
                     ComplaintCategory category,
                     String description,
                     String photo,
                     String location,
                     String locationDescription) {
        this.user = user;
        this.category = category;
        this.description = description;
        this.photo = photo;
        this.location = location;
        this.locationDescription = locationDescription;
        this.status = ComplaintStatus.SUBMITTED;
        this.createdAt = LocalDateTime.now();
    }

    // getters/setters

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public Long getAssignedVendorId() { return assignedVendorId; }
    public void setAssignedVendorId(Long assignedVendorId) { this.assignedVendorId = assignedVendorId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getVendorNotes() {
        return vendorNotes;
    }

    public void setVendorNotes(String vendorNotes) {
        this.vendorNotes = vendorNotes;
    }
}