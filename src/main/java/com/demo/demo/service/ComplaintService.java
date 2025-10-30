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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.UUID;
import java.io.IOException;

@Service
public class ComplaintService {

    private final ComplaintRepository repo;

    // add config property injection
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ComplaintService(ComplaintRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Complaint createComplaint(User user,
                                     com.demo.demo.model.ComplaintCategory category,
                                     String description,
                                     MultipartFile photoFile,    // <-- changed type
                                     String location,
                                     double latitude,
                                     double longitude,
                                     String locationDescription) throws IOException {
        if (user == null) throw new IllegalArgumentException("user required");
        if (category == null) throw new IllegalArgumentException("category required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description required");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("location required");

        Complaint c = new Complaint();
        c.setUser(user);
        c.setCategory(category);
        c.setDescription(description);
        c.setLocation(location);
        c.setLocationDescription(locationDescription);
        c.setStatus(ComplaintStatus.SUBMITTED);
        c.setCreatedAt(LocalDateTime.now());

        c.setLatitude(latitude);
        c.setLongitude(longitude);

        // handle file upload
        if (photoFile != null && !photoFile.isEmpty()) {
            // simple content-type check
            String contentType = photoFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Uploaded file must be an image");
            }

            // ensure directory exists
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // create safe filename: timestamp + random UUID + original extension
            String original = Paths.get(photoFile.getOriginalFilename() == null ? "" : photoFile.getOriginalFilename()).getFileName().toString();
            String ext = "";
            int i = original.lastIndexOf('.');
            if (i > 0) ext = original.substring(i);

            String filename = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
            Path target = uploadPath.resolve(filename);

            // copy file
            try {
                Files.copy(photoFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IOException("Failed to save uploaded file", e);
            }

            // store the accessible path (we'll expose uploads via /uploads/**)
            c.setPhoto("/uploads/" + filename);
        }

        return repo.save(c);
    }

    // ... other existing service methods ...


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