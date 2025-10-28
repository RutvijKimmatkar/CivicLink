package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintCategory;
import com.demo.demo.model.User;
import com.demo.demo.service.ComplaintService;
import com.demo.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;

    public ComplaintController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    // Show the complaint creation form
    @GetMapping("/complaints/new")
    public String newComplaintForm(HttpSession session, Model model) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) return "redirect:/login";

        // Provide enum values for the <select>
        model.addAttribute("categories", ComplaintCategory.values());
        return "complaint_new";
    }

    // Handle form submit
    @PostMapping("/complaints")
    public String createComplaint(@RequestParam ComplaintCategory category,
                                  @RequestParam String description,
                                  @RequestParam(required = false) String photo,
                                  @RequestParam String location,
                                  @RequestParam(required = false) String locationDescription,
                                  HttpSession session,
                                  Model model) {
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) return "redirect:/login";

        Long userId;
        if (userIdObj instanceof Long) userId = (Long) userIdObj;
        else {
            try { userId = Long.valueOf(userIdObj.toString()); }
            catch (NumberFormatException e) { session.invalidate(); return "redirect:/login"; }
        }

        // Resolve user entity
        User user = userService.findById(userId).orElse(null);
        if (user == null) return "redirect:/login";

        // Minimal validation
        if (description == null || description.isBlank() || location == null || location.isBlank()) {
            model.addAttribute("error", "Description and location are required.");
            model.addAttribute("categories", ComplaintCategory.values());
            return "complaint_new";
        }

        // Create complaint
        complaintService.createComplaint(user, category, description, photo, location, locationDescription);

        // Redirect back to dashboard which shows the updated count/list
        return "redirect:/dashboard";
    }
}