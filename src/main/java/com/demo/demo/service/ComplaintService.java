package com.demo.demo.service;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.model.User;
import com.demo.demo.repo.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {

    private final ComplaintRepository repo;

    public ComplaintService(ComplaintRepository repo) {
        this.repo = repo;
    }

    /**
     * Create and persist a complaint.
     */
    @Transactional
    public Complaint createComplaint(User user,
                                     com.demo.demo.model.ComplaintCategory category,
                                     String description,
                                     String photo,
                                     String location,
                                     String locationDescription) {
        if (user == null) throw new IllegalArgumentException("user required");
        if (category == null) throw new IllegalArgumentException("category required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description required");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("location required");

        Complaint c = new Complaint();
        c.setUser(user);
        c.setCategory(category); // ✅ FIXED: use the enum directly, not string
        c.setDescription(description);
        c.setPhoto(photo);
        c.setLocation(location);
        c.setLocationDescription(locationDescription);
        c.setStatus(ComplaintStatus.SUBMITTED);
        c.setCreatedAt(java.time.LocalDateTime.now());

        return repo.save(c);
    }

    /* ----------------- simple query helpers ----------------- */

    public long countByUserId(Long userId) {
        return repo.countByUser_Id(userId);
    }

    public List<Complaint> findByUserId(Long userId) {
        return repo.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public List<Complaint> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }
    // ✅ Add this missing method (this is what your AdminController calls)
    public List<Complaint> findAllByOrderByCreatedAtDesc() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Complaint> findById(Long id) {
        return repo.findById(id);
    }

    public List<Complaint> findByStatus(ComplaintStatus status) {
        return repo.findByStatusOrderByCreatedAtDesc(status);
    }

    /* ----------------- admin/vendor actions ----------------- */

    @Transactional
    public void assignVendor(Long complaintId, Long vendorId) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setAssignedVendorId(vendorId);
        // When assigned, we usually move to IN_PROGRESS if not already
        if (c.getStatus() == null || c.getStatus() == ComplaintStatus.SUBMITTED) {
            c.setStatus(ComplaintStatus.IN_PROGRESS);
        }
        repo.save(c);
    }

    @Transactional
    public void updateStatus(Long complaintId, ComplaintStatus status) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setStatus(status);
        repo.save(c);
    }

    @Transactional
    public void addAdminNotes(Long complaintId, String notes) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setAdminNotes(notes);
        repo.save(c);
    }

    @Transactional
    public void markInProgressWithNotes(Long complaintId, String notes) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setStatus(ComplaintStatus.IN_PROGRESS);
        if (notes != null && !notes.isBlank()) c.setAdminNotes(notes);
        repo.save(c);
    }

    @Transactional
    public void reject(Long complaintId, String reason) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setStatus(ComplaintStatus.REJECTED);
        if (reason != null && !reason.isBlank()) c.setAdminNotes(reason);
        repo.save(c);
    }

    @Transactional
    public void markCompleted(Long complaintId, String notes) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setStatus(ComplaintStatus.COMPLETED);
        if (notes != null && !notes.isBlank()) c.setAdminNotes(notes);
        repo.save(c);
    }
}