package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.service.ComplaintService;
import com.demo.demo.service.VendorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin UI/controller for reviewing and acting on complaints.
 *
 * - Keeps backward-compatible POST mappings (approve <-> inprogress) because templates
 *   in your repo sometimes post to /inprogress while others used /approve.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ComplaintService complaintService;
    private final VendorService vendorService;

    public AdminController(ComplaintService complaintService, VendorService vendorService) {
        this.complaintService = complaintService;
        this.vendorService = vendorService;
    }

    private boolean isAdmin(HttpSession session) {
        Object o = session.getAttribute("isAdmin");
        return o instanceof Boolean && ((Boolean) o);
    }

    @GetMapping("/complaints")
    public String listComplaints(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        // must exist on ComplaintService: findAllByOrderByCreatedAtDesc()
        List<Complaint> complaints = complaintService.findAllByOrderByCreatedAtDesc();
        model.addAttribute("complaints", complaints);
        model.addAttribute("adminName", session.getAttribute("adminName"));
        return "admin/complaints_list";
    }

    @GetMapping("/complaints/{id}")
    public String viewComplaint(@PathVariable Long id,
                                HttpSession session,
                                Model model,
                                RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        var opt = complaintService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Complaint not found");
            return "redirect:/admin/complaints";
        }

        model.addAttribute("complaint", opt.get());
        model.addAttribute("adminName", session.getAttribute("adminName"));
        return "admin/complaint_view";
    }

    // ----- Approve / mark in-progress -----
    // Templates might POST to either /approve or /inprogress — support both.
    @PostMapping({"/complaints/{id}/approve", "/complaints/{id}/inprogress"})
    public String approveComplaint(@PathVariable Long id,
                                   @RequestParam(required = false) String notes,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        try {
            complaintService.markInProgressWithNotes(id, notes);
            ra.addFlashAttribute("message", "Marked In Progress");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to mark In Progress: " + e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    // ----- Reject -----
    @PostMapping("/complaints/{id}/reject")
    public String rejectComplaint(@PathVariable Long id,
                                  @RequestParam(required = false) String reason,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        try {
            complaintService.reject(id, reason);
            ra.addFlashAttribute("message", "Complaint rejected");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Reject failed: " + e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    // ----- Assign (GET form) -----
    @GetMapping("/complaints/{id}/assign")
    public String showAssignForm(@PathVariable Long id,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        var opt = complaintService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Complaint not found");
            return "redirect:/admin/complaints";
        }

        model.addAttribute("complaint", opt.get());
        model.addAttribute("vendors", vendorService.findAll()); // vendorService must implement findAll()
        model.addAttribute("adminName", session.getAttribute("adminName"));
        return "admin/assign";
    }

    // ----- Assign (POST) -----
    @PostMapping("/complaints/{id}/assign")
    public String doAssign(@PathVariable Long id,
                           @RequestParam Long vendorId,
                           HttpSession session,
                           RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        try {
            complaintService.assignVendor(id, vendorId);
            ra.addFlashAttribute("message", "Vendor assigned");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Assign failed: " + e.getMessage());
        }
        return "redirect:/admin/complaints";
    }

    // ----- Complete by admin (force-close) -----
    @PostMapping("/complaints/{id}/complete")
    public String completeByAdmin(@PathVariable Long id,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        try {
            complaintService.markCompleted(id, notes);
            ra.addFlashAttribute("message", "Complaint closed");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Operation failed: " + e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }
}