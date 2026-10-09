package org.lostwind.netlab.controller;

import org.lostwind.netlab.entity.*;
import org.lostwind.netlab.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class FaultRecordController {
    @Autowired
    FaultRecordService faultRecordService;
    @Autowired
    BorrowService borrowService;
    @Autowired
    ReservationService reservationService;
    @Autowired
    AccountService accountService;
    @Autowired
    private DeviceService deviceService;

    // 用户查看自己提交的故障单的页面
    @GetMapping("/my/fault-records")
    public String userRecordList(Authentication authentication,
                                 Model model) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            List<FaultRecord> records = faultRecordService.getUserFaultRecordByUserId(account.getId());
            model.addAttribute("records", records);
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "fault_record/my_list";
    }

    // 用户提交故障单的界面
    @GetMapping("/my/reservations/{reservationId}/report-fault")
    public String userReportPage(@PathVariable Integer reservationId,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            Reservation reservation = reservationService.getUserReservationById(account.getId(), reservationId);
            BorrowRecord record = borrowService.getRecordByReservationId(reservationId);

            model.addAttribute("reservation", reservation);
            model.addAttribute("record", record);
            return "fault_record/create";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my/reservations";
        }
    }

    // 用户提交故障单的接口
    @PostMapping("/my/reservations/{reservationId}/report-fault")
    public String userReportFault(@PathVariable Integer reservationId,
                              @RequestParam String description,
                              Authentication authentication,
                              RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            BorrowRecord record = borrowService.getRecordByReservationId(reservationId);
            faultRecordService.createFaultRecordByUser(record.getId(), account.getId(), description);
            attributes.addFlashAttribute("message", "故障上报成功");
            return "redirect:/my/reservations";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my/reservations/" + reservationId + "/report-fault";
        }
    }

    // 管理员查看所有的故障单
    @GetMapping("/admin/fault-records")
    public String adminRecordList(Model model) {
        List<FaultRecord> records = faultRecordService.getAllList();
        model.addAttribute("records", records);
        return "fault_record/list";
    }

    // 管理员提交故障单的界面
    @GetMapping("/admin/devices/{deviceId}/report-fault")
    public String adminReportPage(@PathVariable Integer deviceId,
                                  Model model,
                                  RedirectAttributes attributes) {
        try {
            Device device = deviceService.getDeviceById(deviceId);
            model.addAttribute("device", device);
            return "fault_record/admin_create";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/devices";
        }
    }

    // 管理员提交故障单的接口
    @PostMapping("/admin/devices/{deviceId}/report-fault")
    public String adminReportFault(@PathVariable Integer deviceId,
                                   @RequestParam String description,
                                   Authentication authentication,
                                   RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            faultRecordService.createFaultRecordByAdmin(deviceId, account.getId(), description);
            attributes.addFlashAttribute("message", "故障上报成功");
            return "redirect:/devices";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/devices/" + deviceId + "/report-fault";
        }
    }
}
