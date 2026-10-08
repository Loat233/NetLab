package org.lostwind.netlab.controller;

import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.enums.DeviceStatus;
import org.lostwind.netlab.service.CategoryService;
import org.lostwind.netlab.service.DeviceService;
import org.lostwind.netlab.service.LaboratoryService;
import org.springframework.beans.factory.annotation.Autowired;
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



    /*
        查询信息的端口
    */
    @GetMapping("/devices")
    public String deviceList(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Integer categoryId,
                             @RequestParam(required = false) Integer laboratoryId,
                             @RequestParam(required = false) DeviceStatus status,
                             Model model) {
        List<Device> devices = deviceService.getDeviceList(keyword, categoryId, laboratoryId, status);
        model.addAttribute("devices", devices);
        return "devices/list";
    }

    @GetMapping("/devices/{id}")
    public String deviceDetail(@PathVariable Integer id, Model model) {
        Device device = deviceService.getDeviceById(id);
        model.addAttribute("device", device);
        return "devices/detail";
    }



    /*
        管理员添加设备业务
    */
    @GetMapping("/admin/devices/add")
    public String addDevicePage(Model model) {
        model.addAttribute("device", new Device());

        // 用于页面中的分类和实验室下拉框
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("laboratories", laboratoryService.getAllLaboratories());

        return "devices/add_form";
    }

    @PostMapping("/admin/devices/add")
    public String addDevice(@ModelAttribute Device device, RedirectAttributes attributes) {
        device.setStatus(DeviceStatus.AVAILABLE);
        deviceService.addDevice(device);
        attributes.addFlashAttribute("message", "设备添加成功");
        return "redirect:/devices";
    }



    @GetMapping("/admin/devices/{id}/edit")
    public String editDevicePage(@PathVariable Integer id, Model model){
        Device device = deviceService.getDeviceById(id);
        model.addAttribute("device", device);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("laboratories", laboratoryService.getAllLaboratories());
        return "devices/edit_form";
    }

    @PostMapping("/admin/devices/{id}/edit")
    public String editDevice(@PathVariable Integer id,
                             @ModelAttribute Device device,
                             RedirectAttributes attributes){
        // 以路径的id为准，防止篡改
        device.setId(id);
        deviceService.updateDevice(device);
        attributes.addFlashAttribute("message", "设备信息修改成功");
        return "redirect:/devices";
    }



    @GetMapping("/admin/devices/{id}/status")
    public String editStatusPage(@PathVariable Integer id, Model model) {
        Device device = deviceService.getDeviceById(id);
        model.addAttribute("device", device);
        model.addAttribute("statuses", DeviceStatus.values());
        return "devices/status_form";
    }

    @PostMapping("/admin/devices/{id}/status")
    public String editStatus(@PathVariable Integer id,
                                   @RequestParam DeviceStatus status,
                                   RedirectAttributes attributes) {
        deviceService.updateStatus(id, status);
        attributes.addFlashAttribute("message", "设备状态修改成功");
        return "redirect:/devices";
    }
}
