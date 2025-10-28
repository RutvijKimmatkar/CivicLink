package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final ComplaintService complaintService;

    public DashboardController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // debug info - remove after troubleshooting
        System.out.println("DEBUG: HttpSession id = " + session.getId());
        System.out.println("DEBUG: session attributes username=" + session.getAttribute("username")
                + ", userId=" + session.getAttribute("userId"));

        Object usernameObj = session.getAttribute("username");
        Object userIdObj = session.getAttribute("userId");
        if (usernameObj == null || userIdObj == null) {
            // no valid session — redirect to login
            return "redirect:/login";
        }

        Long userId;
        if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        } else {
            try {
                userId = Long.valueOf(userIdObj.toString());
            } catch (NumberFormatException ex) {
                session.invalidate();
                return "redirect:/login";
            }
        }

        long complaintCount = complaintService.countByUserId(userId);
        List<Complaint> complaints = complaintService.findByUserId(userId);

        model.addAttribute("name", usernameObj.toString());
        model.addAttribute("complaintCount", complaintCount);
        model.addAttribute("complaints", complaints);

        return "dashboard";
    }
}