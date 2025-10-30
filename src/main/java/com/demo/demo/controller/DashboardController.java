package com.demo.demo.controller;

import com.demo.demo.model.Complaint;
import com.demo.demo.model.ComplaintStatus;
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

        // load user's complaints (already ordered in your service/repo)
        List<Complaint> complaints = complaintService.findByUserId(userId);

        // total complaints for the user
        long complaintCount = (complaints == null) ? 0L : complaints.size();

        // compute counts by status without touching repository/service (safe and quick)
        long complaintInProgressCount = 0L;
        long complaintClosedCount = 0L;

        if (complaints != null) {
            for (Complaint c : complaints) {
                if (c == null || c.getStatus() == null) continue;
                if (c.getStatus() == ComplaintStatus.IN_PROGRESS) complaintInProgressCount++;
                else if (c.getStatus() == ComplaintStatus.COMPLETED) complaintClosedCount++;
            }
        }

        model.addAttribute("name", usernameObj.toString());
        model.addAttribute("complaintCount", complaintCount);
        model.addAttribute("complaintInProgressCount", complaintInProgressCount);
        model.addAttribute("complaintClosedCount", complaintClosedCount);
        model.addAttribute("complaints", complaints);

        return "dashboard";
    }
}