package com.demo.demo.service;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.Vendor;
import com.demo.demo.repo.ComplaintRepository;
import com.demo.demo.repo.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

    private final VendorRepository vendorRepo;
    private final ComplaintRepository complaintRepo;

    public VendorService(VendorRepository vendorRepo, ComplaintRepository complaintRepo) {
        this.vendorRepo = vendorRepo;
        this.complaintRepo = complaintRepo;
    }

    public List<Vendor> findAll() {
        return vendorRepo.findAll();
    }

    public Optional<Vendor> findVendorById(Long id) {
        return vendorRepo.findById(id);
    }

    /**
     * Simple helper: list complaints assigned to a vendor.
     * Assumes Complaint has assignedVendorId field.
     */
    public List<Complaint> listAssignedComplaints(Long vendorId) {
        // We don't have a repository method for this in the snippet; easiest is to pull all and filter.
        // For production, add ComplaintRepository.findByAssignedVendorIdOrderByCreatedAtDesc(...)
        return complaintRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(c -> c.getAssignedVendorId() != null && c.getAssignedVendorId().equals(vendorId))
                .toList();
    }

    @Transactional
    public void acceptComplaint(Long vendorId, Long complaintId) {
        Complaint c = complaintRepo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (!vendorId.equals(c.getAssignedVendorId())) throw new IllegalArgumentException("Not assigned to this vendor");
        c.setStatus(com.demo.demo.model.ComplaintStatus.IN_PROGRESS);
        complaintRepo.save(c);
    }

    @Transactional
    public void rejectComplaint(Long vendorId, Long complaintId, String reason) {
        Complaint c = complaintRepo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (!vendorId.equals(c.getAssignedVendorId())) throw new IllegalArgumentException("Not assigned to this vendor");
        c.setStatus(com.demo.demo.model.ComplaintStatus.SUBMITTED); // back to submitted or use REJECTED per workflow
        if (reason != null && !reason.isBlank()) c.setAdminNotes(reason);
        complaintRepo.save(c);
    }

    @Transactional
    public void completeComplaint(Long vendorId, Long complaintId, String notes) {
        Complaint c = complaintRepo.findById(complaintId).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (!vendorId.equals(c.getAssignedVendorId())) throw new IllegalArgumentException("Not assigned to this vendor");
        c.setStatus(com.demo.demo.model.ComplaintStatus.COMPLETED); // vendor marks completed; admin verifies later
        if (notes != null && !notes.isBlank()) c.setAdminNotes(notes);
        complaintRepo.save(c);
    }
}