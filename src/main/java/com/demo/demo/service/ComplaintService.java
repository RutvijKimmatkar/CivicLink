package com.demo.demo.service;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.model.User;
import com.demo.demo.repo.ComplaintRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.demo.demo.dto.PublicComplaintDto;

@Service
public class ComplaintService {

    private final ComplaintRepository repo;

    public ComplaintService(ComplaintRepository repo) {
        this.repo = repo;
    }

    // upload dir config (default "uploads")
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /* ----------------- counts & queries used by controllers ----------------- */

    public long countAll() {
        return repo.count();
    }

    public long countByStatus(ComplaintStatus status) {
        // ensure repo has countByStatus(ComplaintStatus)
        return repo.countByStatus(status);
    }

    public long countByUserId(Long userId) {
        return repo.countByUser_Id(userId);
    }

    public List<Complaint> findByUserId(Long userId) {
        return repo.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public List<Complaint> findAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // AdminController expects this exact method name
    public List<Complaint> findAllByOrderByCreatedAtDesc() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Complaint> findById(Long id) {
        return repo.findById(id);
    }

    public List<Complaint> findByStatus(ComplaintStatus status) {
        return repo.findByStatusOrderByCreatedAtDesc(status);
    }

    /* ----------------- create + file upload ----------------- */

    @Transactional
    public Complaint createComplaint(User user,
                                     com.demo.demo.model.ComplaintCategory category,
                                     String description,
                                     MultipartFile photoFile,
                                     String location,
                                     Double latitude,
                                     Double longitude,
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

        if (latitude != null) c.setLatitude(latitude);
        if (longitude != null) c.setLongitude(longitude);

        if (photoFile != null && !photoFile.isEmpty()) {
            String contentType = photoFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Uploaded file must be an image");
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String original = Paths.get(photoFile.getOriginalFilename() == null ? "" : photoFile.getOriginalFilename()).getFileName().toString();
            String ext = "";
            int i = original.lastIndexOf('.');
            if (i > 0) ext = original.substring(i);

            String filename = System.currentTimeMillis() + "-" + UUID.randomUUID() + ext;
            Path target = uploadPath.resolve(filename);

            try {
                Files.copy(photoFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IOException("Failed to save uploaded file", e);
            }

            // store path that will be served by static mapping (e.g. /uploads/**)
            c.setPhoto("/uploads/" + filename);
        }

        return repo.save(c);
    }

    /* ----------------- admin/vendor actions ----------------- */

    @Transactional
    public void assignVendor(Long complaintId, Long vendorId) {
        Complaint c = repo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        c.setAssignedVendorId(vendorId);
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

    // AdminController expects markInProgressWithNotes(Long,String)
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

    /* ----------------- public listing helpers ----------------- */

    public List<PublicComplaintDto> findRecentPublicComplaints(int limit) {
        List<Complaint> recent = repo.findTop50ByOrderByCreatedAtDesc();
        return recent.stream().limit(limit).map(c -> {
            PublicComplaintDto d = new PublicComplaintDto();
            d.setId(c.getId());
            d.setCategory(c.getCategory() != null ? c.getCategory().name() : null);
            d.setDescription(c.getDescription());
            if (c.getLatitude() != null && c.getLongitude() != null) {
                double lat = Math.round(c.getLatitude() * 10000d) / 10000d;
                double lon = Math.round(c.getLongitude() * 10000d) / 10000d;
                d.setLatitude(lat);
                d.setLongitude(lon);
            }
            d.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
            d.setCreatedAt(c.getCreatedAt());
            d.setPhoto(c.getPhoto());
            return d;
        }).collect(Collectors.toList());
    }

    public Map<String, Long> countByCategory() {
        List<Complaint> all = repo.findAll();
        Map<String, Long> counts = all.stream()
                .collect(Collectors.groupingBy(c -> (c.getCategory() != null ? c.getCategory().name() : "UNKNOWN"),
                        Collectors.counting()));
        return new LinkedHashMap<>(counts);
    }
}