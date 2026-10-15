package org.lostwind.netlab.controller;

import org.lostwind.netlab.entity.Account;
import org.lostwind.netlab.entity.DashboardStatistics;
import org.lostwind.netlab.entity.Review;
import org.lostwind.netlab.service.AccountService;
import org.lostwind.netlab.service.DashboardService;
import org.lostwind.netlab.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {
    @Autowired
    DashboardService dashboardService;
    @Autowired
    AccountService accountService;
    @Autowired
    ReviewService reviewService;

    @GetMapping({"/","/index"})
    public String dashboard(Authentication authentication, Model model) {
        Account account = accountService.getAccountByUsername(authentication.getName());
        List<Review> reviews = reviewService.getReviews();
        DashboardStatistics statistics ="ADMIN".equals(account.getRole()) ? dashboardService.getAdminDashboard() : dashboardService.getUserDashboard(account.getId());
        model.addAttribute("statistics", statistics);
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentAccountId", account.getId());
        return "index";
    }

    @PostMapping("/add-review")
    public String addReview(@RequestParam("content") String content,
                            Authentication authentication,
                            RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            reviewService.addReview(account.getId(), content);
        } catch (IllegalStateException | IllegalArgumentException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/index";
    }

    @PostMapping("/delete-review")
    public String deleteReview(@RequestParam("review_id") Integer reviewId,
                            Authentication authentication,
                            RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            reviewService.deleteReview(account.getId(), reviewId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/index";
    }

    @GetMapping("/about-us")
    public String aboutUsPage() {
        return "about-us";
    }
}
