package org.lostwind.netlab.controller;

import org.lostwind.netlab.entity.Account;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.enums.DeviceStatus;
import org.lostwind.netlab.service.AccountService;
import org.lostwind.netlab.service.CategoryService;
import org.lostwind.netlab.service.DeviceService;
import org.lostwind.netlab.service.LaboratoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DeviceController {
    @Autowired
    DeviceService deviceService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    LaboratoryService laboratoryService;
    @Autowired
    AccountService accountService;



    /*
        查询信息的页面
    */
    @GetMapping("/devices")
    public String deviceList(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Integer categoryId,
                             @RequestParam(required = false) Integer laboratoryId,
                             @RequestParam(required = false) DeviceStatus status,
                             Model model) {
        keyword = keyword == null ? null : keyword.trim();
        List<Device> devices = deviceService.getDeviceList(keyword, categoryId, laboratoryId, status);
        model.addAttribute("devices", devices);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", DeviceStatus.values());
        return "devices/list";
    }

    @GetMapping("/devices/{id}")
    public String deviceDetail(@PathVariable Integer id, Model model) {
        Device device = deviceService.getDeviceById(id);
        model.addAttribute("device", device);
        return "devices/detail";
    }



    /*
        管理员添加设备页面
    */
    @GetMapping("/admin/devices/add")
    public String addDevicePage(Model model) {
        model.addAttribute("device", new Device());

        // 用于页面中的分类和实验室下拉框
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("laboratories", laboratoryService.getAllLaboratories());

        return "devices/add_form";
    }

    /*
        管理员添加设备业务
    */
    @PostMapping("/admin/devices/add")
    public String addDevice(@ModelAttribute Device device,
                            Authentication authentication,
                            RedirectAttributes attributes) {
        try {
            device.setStatus(DeviceStatus.AVAILABLE);
            Account account = accountService.getAccountByUsername(authentication.getName());
            deviceService.addDevice(device, account.getId());
            attributes.addFlashAttribute("message", "设备添加成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices";
    }


    /*
        管理员编辑设备页面
    */
    @GetMapping("/admin/devices/{id}/edit")
    public String editDevicePage(@PathVariable Integer id, Model model){
        Device device = deviceService.getDeviceById(id);
        model.addAttribute("device", device);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("laboratories", laboratoryService.getAllLaboratories());
        return "devices/edit_form";
    }

    /*
       管理员编辑设备业务
    */
    @PostMapping("/admin/devices/{id}/edit")
    public String editDevice(@PathVariable Integer id,
                             @ModelAttribute Device device,
                             Authentication authentication,
                             RedirectAttributes attributes){
        try {
            // 以路径的id为准，防止篡改
            device.setId(id);
            Account account = accountService.getAccountByUsername(authentication.getName());
            deviceService.updateDevice(device, account.getId());
            attributes.addFlashAttribute("message", "设备信息修改成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices";
    }
}
