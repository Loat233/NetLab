package org.lostwind.netlab.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;

@Validated
@Controller
public class LogController {
    private static final String CODE_KEY = "LOGIN_CODE";    // 验证码
    private static final String EMAIL_KEY = "LOGIN_CODE_EMAIL"; // 接收验证码的邮箱
    private static final String EXPIRES_KEY = "LOGIN_CODE_EXPIRES_AT";  // 自动销毁的时间
    private static final String LAST_SEND_KEY = "LOGIN_CODE_LAST_SENT_AT"; // 上次发送验证码的时间


    private static final long CODE_VALID_MILLIS = 5 * 60 * 1000L; // 验证码有效期
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L; // 发送的间隔时间

    // 发送邮件的地址
    @Value("${spring.mail.username}")
    String fromAddr;

    // 验证码生成器
    final SecureRandom random = new SecureRandom();
    // 邮件发送器
    @Autowired
    JavaMailSender sender;


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @ResponseBody
    @PostMapping(value = "/auth/verification-code/send")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestParam(required = false)
                                                            @NotBlank(message = "请输入邮箱")
                                                            @Email(message = "邮箱格式不正确")
                                                            String username,
                                                        HttpSession session) {
        // 限制同一会话60s内重复发送
        Long now = System.currentTimeMillis();
        Long lastSendTime = (Long) session.getAttribute(LAST_SEND_KEY);
        if (lastSendTime != null && now - lastSendTime < SEND_INTERVAL_MILLIS) {
            long retryAfter = (SEND_INTERVAL_MILLIS - (now - lastSendTime)) / 1000;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "success", false,
                            "message", "发送过于频繁,请在" + retryAfter + "秒后重试"
                    ));
        }

        String code = String.valueOf(random.nextInt(90000) + 10000);
        session.setAttribute("code", code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddr);
        message.setTo(username);
        message.setSubject("您的验证码");
        message.setText("""
            您正在登录 NetLab 网络实验室设备预约平台。

            本次验证码：%s

            验证码 5 分钟内有效，请勿向他人泄露。
            如果不是您本人操作，请忽略此邮件。
            """.formatted(code)
        );

        try {
            session.setAttribute(LAST_SEND_KEY, now);

            sender.send(message);

            session.setAttribute(CODE_KEY, code);
            session.setAttribute(EMAIL_KEY, username);
            session.setAttribute(EXPIRES_KEY, now + CODE_VALID_MILLIS);

            return ResponseEntity.ok(Map.of("success", true, "message", "验证码发送成功"));
        } catch (MailException e) {
            session.removeAttribute(LAST_SEND_KEY);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "验证码发送失败，请稍后重试"));
        }
    }


    @RequestMapping({"/", "/index"})
    public String indexPage() {
        return "index";
    }
}
