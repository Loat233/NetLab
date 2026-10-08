package org.lostwind.netlab.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class VerificationFilter extends OncePerRequestFilter {
    private static final String CODE_KEY = "LOGIN_CODE";    // 验证码
    private static final String EMAIL_KEY = "LOGIN_CODE_EMAIL"; // 接收验证码的邮箱
    private static final String EXPIRES_KEY = "LOGIN_CODE_EXPIRES_AT";  // 自动销毁的时间

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isLoginRequest = "/doLogin".equals(request.getServletPath());
        boolean isPostRequest = "POST".equalsIgnoreCase(request.getMethod());

        // 返回 true 表示跳过当前过滤器
        return !(isLoginRequest && isPostRequest);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String submitEmail = request.getParameter("username");
        String submitCode = request.getParameter("code");
        HttpSession session = request.getSession(false);

        // 如果session失效或没有提交验证码，重定向至报错路径
        if (session == null || !StringUtils.hasText(submitCode)) {
            redirectToLogin(request, response);
            return;
        }

        // 从session里获取信息
        String storedCode = (String) session.getAttribute(CODE_KEY);
        String storedEmail = (String) session.getAttribute(EMAIL_KEY);
        Long expiresAt = (Long) session.getAttribute(EXPIRES_KEY);

        // 如果session内没有信息，重定向至报错路径
        if (!StringUtils.hasText(storedCode) || !StringUtils.hasText(storedEmail) || expiresAt == null) {
            redirectToLogin(request, response);
            return;
        }

        // 验证验证码是否到销毁时间
        if (System.currentTimeMillis() > expiresAt) {
            deleteVerificationInfo(session);
            redirectToLogin(request, response);
            return;
        }

        // 检验提交表单的邮箱，是否与发送验证码的邮箱一致
        if (!Objects.equals(submitEmail, storedEmail)) {
            redirectToLogin(request, response);
            return;
        }

        // 验证码是否一致
        if (!Objects.equals(submitCode, storedCode)) {
            redirectToLogin(request, response);
            return;
        }

        // 验证通过，删除验证码相关信息，继续执行SpringSecurity的用户密码验证
        filterChain.doFilter(request, response);
    }

    // 销毁session中验证码相关信息
    private void deleteVerificationInfo(HttpSession session) {
        session.removeAttribute(CODE_KEY);
        session.removeAttribute(EMAIL_KEY);
        session.removeAttribute(EXPIRES_KEY);
    }

    // 重定向至登录错误的路径
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login?codeError");
    }
}
