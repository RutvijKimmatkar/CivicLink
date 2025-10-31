package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintCategory;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.model.User;
import com.demo.demo.service.ComplaintService;
import com.demo.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public ComplaintController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    // --- Show complaint creation form ---
    @GetMapping("/complaints/new")
    public String newComplaintForm(HttpSession session, Model model) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) return "redirect:/login";

        model.addAttribute("categories", ComplaintCategory.values());
        return "complaint_new";
    }

    // --- View complaint details page ---
    @GetMapping("/complaints/{id}")
    public String viewComplaintDetails(@PathVariable Long id, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Complaint complaint = complaintService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        // Optional: prevent viewing others' complaints
        if (!complaint.getUser().getId().equals(userId)) {
            return "redirect:/dashboard";
        }

        // Decide color class based on status
        String statusClass = "bg-slate-50 text-slate-700";
        if (complaint.getStatus() != null) {
            switch (complaint.getStatus()) {
                case COMPLETED -> statusClass = "bg-green-50 text-green-700";
                case IN_PROGRESS -> statusClass = "bg-blue-50 text-blue-700";
                case SUBMITTED -> statusClass = "bg-amber-50 text-amber-700";
                default -> statusClass = "bg-slate-50 text-slate-700";
            }
        }

        model.addAttribute("statusClass", statusClass);
        model.addAttribute("complaint", complaint);
        return "complaint-details";
    }

    // --- Handle form submit for new complaint ---
    @PostMapping("/complaints")
    public String createComplaint(@RequestParam ComplaintCategory category,
                                  @RequestParam String description,
                                  @RequestParam(required = false) String location,
                                  @RequestParam(required = false) String locationDescription,
                                  @RequestParam(required = false) MultipartFile photoFile,
                                  @RequestParam(required = false) Double latitude,
                                  @RequestParam(required = false) Double longitude,
                                  HttpSession session,
                                  Model model) {

        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) return "redirect:/login";

        Long userId;
        try {
            userId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            session.invalidate();
            return "redirect:/login";
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null) return "redirect:/login";

        if (description == null || description.isBlank() || location == null || location.isBlank()) {
            model.addAttribute("error", "Description and location are required.");
            model.addAttribute("categories", ComplaintCategory.values());
            return "complaint_new";
        }

        try {
            complaintService.createComplaint(user, category, description, photoFile,
                    location, latitude, longitude, locationDescription);
            return "redirect:/dashboard";
        } catch (IOException ex) {
            model.addAttribute("error", "Failed to upload image: " + ex.getMessage());
            model.addAttribute("categories", ComplaintCategory.values());
            return "complaint_new";
        }
    }
}