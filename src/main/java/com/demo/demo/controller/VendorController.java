package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.model.Vendor;
import com.demo.demo.service.VendorService;
import com.demo.demo.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private final VendorService vendorService;
    private final ComplaintService complaintService;

    public VendorController(VendorService vendorService, ComplaintService complaintService) {
        this.vendorService = vendorService;
        this.complaintService = complaintService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "vendor/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam Long vendorId, HttpSession session, Model model) {
        Optional<Vendor> v = vendorService.findVendorById(vendorId);
        if (v.isEmpty() || !v.get().isActive()) {
            model.addAttribute("error", "Vendor not found or inactive");
            return "vendor/login";
        }
        session.setAttribute("vendorId", vendorId);
        session.setAttribute("vendorName", v.get().getName());
        return "redirect:/vendor/complaints";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("vendorId");
        session.removeAttribute("vendorName");
        return "redirect:/vendor/login";
    }

    /* ---------------- vendor UI ---------------- */

    private boolean checkVendor(HttpSession session) {
        Object o = session.getAttribute("vendorId");
        return o != null;
    }

    // Accept many possible entry URLs so templates/links won't break
    @GetMapping({"", "/", "/dashboard", "/complaints"})
    public String dashboard(HttpSession session, Model model) {
        if (!checkVendor(session)) return "redirect:/vendor/login";

        Long vendorId = (session.getAttribute("vendorId") instanceof Long)
                ? (Long) session.getAttribute("vendorId")
                : Long.valueOf(session.getAttribute("vendorId").toString());

        Optional<Vendor> vendor = vendorService.findVendorById(vendorId);
        List<Complaint> assigned = vendorService.listAssignedComplaints(vendorId);

        model.addAttribute("vendor", vendor.orElse(null));
        model.addAttribute("vendorName", session.getAttribute("vendorName"));
        model.addAttribute("complaints", assigned);
        return "vendor/complaints_list"; // pick one template name and keep it consistent
    }

    // View single complaint (use direct findById and authorization check)
    @GetMapping("/complaints/{id}")
    public String viewComplaint(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        if (!checkVendor(session)) return "redirect:/vendor/login";

        Long vendorId = Long.valueOf(session.getAttribute("vendorId").toString());
        Optional<Complaint> maybe = complaintService.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("error", "Complaint not found");
            return "redirect:/vendor/complaints";
        }
        Complaint c = maybe.get();

        // ensure complaint is assigned to this vendor (or allowed)
        if (c.getAssignedVendorId() == null || !c.getAssignedVendorId().equals(vendorId)) {
            ra.addFlashAttribute("error", "Complaint not assigned to you");
            return "redirect:/vendor/complaints";
        }

        model.addAttribute("complaint", c);
        model.addAttribute("vendorName", session.getAttribute("vendorName"));
        return "vendor/complaint_view";
    }

    @PostMapping("/complaints/{id}/accept")
    public String accept(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!checkVendor(session)) return "redirect:/vendor/login";
        Long vendorId = Long.valueOf(session.getAttribute("vendorId").toString());

        try {
            // VendorService should verify assignment and permission inside
            vendorService.acceptComplaint(vendorId, id);
            ra.addFlashAttribute("message", "Accepted complaint " + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vendor/complaints/" + id;
    }

    @PostMapping("/complaints/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (!checkVendor(session)) return "redirect:/vendor/login";
        Long vendorId = Long.valueOf(session.getAttribute("vendorId").toString());

        try {
            vendorService.rejectComplaint(vendorId, id, reason);
            ra.addFlashAttribute("message", "Rejected complaint " + id);
            return "redirect:/vendor/complaints";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/complaints/" + id;
        }
    }

    @PostMapping("/complaints/{id}/complete")
    public String complete(@PathVariable Long id,
                           @RequestParam(required = false) String notes,
                           HttpSession session,
                           RedirectAttributes ra) {
        if (!checkVendor(session)) return "redirect:/vendor/login";
        Long vendorId = Long.valueOf(session.getAttribute("vendorId").toString());

        try {
            vendorService.completeComplaint(vendorId, id, notes);
            ra.addFlashAttribute("message", "Marked complete. Waiting admin verification.");
            return "redirect:/vendor/complaints/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/complaints/" + id;
        }
    }
}