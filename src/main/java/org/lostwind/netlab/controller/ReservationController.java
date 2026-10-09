package org.lostwind.netlab.controller;

import org.lostwind.netlab.dto.ReservationRequest;
import org.lostwind.netlab.entity.Account;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.entity.Reservation;
import org.lostwind.netlab.enums.ReservationStatus;
import org.lostwind.netlab.mapper.AccountMapper;
import org.lostwind.netlab.service.AccountService;
import org.lostwind.netlab.service.BorrowService;
import org.lostwind.netlab.service.DeviceService;
import org.lostwind.netlab.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ReservationController {
    @Autowired
    ReservationService reservationService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    AccountService accountService;
    @Autowired
    private BorrowService borrowService;


    /*
        用户提交预约业务
    */
    @GetMapping("/my/reservations")
    public String myReservations(Authentication authentication, Model model) {
        Account account = accountService.getAccountByUsername(authentication.getName());
        List<Reservation> reservations = reservationService.getReservationsByApplicantId(account.getId());
        model.addAttribute("reservations", reservations);
        return "reservations/my_list";
    }

    @GetMapping("/devices/{deviceId}/reserve")
    public String reservePage(@PathVariable Integer deviceId, Model model) {
        Device device = deviceService.getDeviceById(deviceId);
        model.addAttribute("device", device);

        // 首次访问预约填写页面时，创建新request(抛出错误重返该页面时，不用新建request)
        if (!model.containsAttribute("reservationRequest")) {
            ReservationRequest request = new ReservationRequest();
            request.setDeviceId(deviceId);
            model.addAttribute("reservationRequest", request);
        }
       	return "reservations/create";
    }

    @PostMapping("/reservations")
    public String createReservation(@ModelAttribute("reservationRequest") ReservationRequest request,
                                    Authentication authentication,
                                    RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            reservationService.createReservation(account.getId(), request);
            attributes.addFlashAttribute("message", "预约申请已提交");
            return "redirect:/my/reservations";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            attributes.addFlashAttribute("reservationRequest", request);
            return "redirect:/devices/" + request.getDeviceId() + "/reserve";
        }
    }
    /*
        用户取消预约业务
    */
    @GetMapping("/my/reservations/{id}/cancel")
    public String cancelPage(@PathVariable Integer id,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes attributes) {
        try {
            Integer accountId = accountService.getAccountByUsername(authentication.getName()).getId();
            Reservation reservation = reservationService.getCancellableUserReservationById(accountId, id);
            model.addAttribute("reservation", reservation);
            return "reservations/cancel";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my/reservations";
        }
    }

    @PostMapping("/my/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Integer id,
                                    Authentication authentication,
                                    @RequestParam String reason,
                                    RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            reservationService.cancelReservation(id, account.getId(), reason);
            attributes.addFlashAttribute("message", "预约已取消");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/reservations";
    }



    /*
        管理员确认借出后，用户查看可申请归还的预约表
    */
    @GetMapping("/my/reservations/{id}/return")
    public String borrowedPage(@PathVariable Integer id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            Reservation reservation = reservationService.getReturnableUserReservationById(account.getId(), id);
            model.addAttribute("reservation", reservation);
            return "reservations/return_pending";
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addAttribute("error", e.getMessage());
            return "redirect:/my/reservations";
        }
    }
    /*
        用户申请归还的业务
    */
    @PostMapping("/my/reservations/{id}/return")
    public String requestReturn(@PathVariable Integer id,
                                Authentication authentication,
                                RedirectAttributes attributes) {
        try {
            Account account = accountService.getAccountByUsername(authentication.getName());
            borrowService.returnPending(id, account.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/reservations";
    }

    /*
        管理员查看所有预约
    */
    @GetMapping("/admin/reservations")
    public String adminReservationList(@RequestParam(required = false) ReservationStatus status, Model model) {
        List<Reservation> reservations = reservationService.getAdminReservationList(status);

        model.addAttribute("reservations", reservations);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", List.of(
                ReservationStatus.PENDING,
                ReservationStatus.APPROVED,
                ReservationStatus.RETURN_PENDING
        ));
        return "reservations/list";
    }

    /*
        管理员审核某预约的页面
    */
    @GetMapping("/admin/reservations/{reservationId}/check")
    public String checkPendingReservationsPage(@PathVariable Integer reservationId,
                                          Model model){
        Reservation reservation = reservationService.getReservationById(reservationId);
        Account account = accountService.getAccountById(reservation.getApplicantId());
        Device device = deviceService.getDeviceById(reservation.getDeviceId());

        model.addAttribute("reservation", reservation);
        model.addAttribute("account", account);
        model.addAttribute("device", device);
        return "reservations/check";
    }

    /*
        管理员批准预约业务
    */
    @PostMapping("/admin/reservations/{reservationId}/approved")
    public String approvePendingReservation(@PathVariable Integer reservationId,
                                            Authentication authentication,
                                            RedirectAttributes attributes) {
        try {
            Account reviewer = accountService.getAccountByUsername(authentication.getName());
            if (reviewer == null) {
                throw new IllegalArgumentException("当前登录账号不存在");
            }
            reservationService.approveReservation(reservationId, reviewer.getId());
            attributes.addFlashAttribute("message", "批准成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservations?status=PENDING";
    }

    /*
       管理员拒绝预约业务
    */
    @PostMapping("/admin/reservations/{reservationId}/reject")
    public String rejectPendingReservation(@PathVariable Integer reservationId,
                                            @RequestParam(required = false) String reason,
                                            Authentication authentication,
                                            RedirectAttributes attributes) {
        try {
            Account reviewer = accountService.getAccountByUsername(authentication.getName());
            if (reviewer == null) {
                throw new IllegalArgumentException("当前登录账号不存在");
            }
            reason = reason == null ? null : reason.trim();
            reservationService.rejectReservation(reservationId, reviewer.getId(), reason);
            attributes.addFlashAttribute("message", "拒绝成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservations?status=PENDING";
    }


    /*
        管理员确认借出业务
    */
    @PostMapping("/admin/reservations/{reservationId}/borrow")
    public String confirmBorrow(@PathVariable Integer reservationId,
                                Authentication authentication,
                                RedirectAttributes attributes) {
        try {
            Account operator = accountService.getAccountByUsername(authentication.getName());
            borrowService.confirmBorrow(reservationId, operator.getId());
            attributes.addFlashAttribute("message", "设备借出成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservations?status=APPROVED";
    }


   /*
       管理员确认归还业务
   */
   @PostMapping("/admin/reservations/{reservationId}/confirmReturn")
   public String confirmReturn(@PathVariable Integer reservationId,
                               @RequestParam(required = false) String remark,
                               Authentication authentication,
                               RedirectAttributes attributes) {
       try {
           Account operator = accountService.getAccountByUsername(authentication.getName());
           borrowService.confirmReturn(reservationId, operator.getId(),remark);
           attributes.addFlashAttribute("message", "确认归还成功");
       } catch (IllegalArgumentException | IllegalStateException e) {
           attributes.addFlashAttribute("error", e.getMessage());
       }
       return "redirect:/admin/reservations?status=RETURN_PENDING";
   }
}
