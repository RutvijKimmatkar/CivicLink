package com.demo.demo.controller;

import com.demo.demo.dto.PublicComplaintDto;
import com.demo.demo.service.ComplaintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/public")
public class PublicController {

    private final ComplaintService complaintService;

    public PublicController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // fetch small summary and pass server-rendered counts for cards
        Map<String, Long> summary = complaintService.countByCategory();
        model.addAttribute("categorySummary", summary);
        // optionally pass initial complaint list for server-side render
        List<PublicComplaintDto> list = complaintService.findRecentPublicComplaints(20);
        model.addAttribute("initialComplaints", list);
        return "public_dashboard";
    }

    @GetMapping("/complaints.json")
    @ResponseBody
    public List<PublicComplaintDto> complaintsJson(@RequestParam(defaultValue = "50") int limit) {
        return complaintService.findRecentPublicComplaints(limit);
    }
}