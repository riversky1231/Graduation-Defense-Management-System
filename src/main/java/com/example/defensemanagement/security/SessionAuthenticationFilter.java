package com.example.defensemanagement.security;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing == null || !existing.isAuthenticated()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Authentication authentication = buildAuthentication(session);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private Authentication buildAuthentication(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
            if (currentUser.getRole() != null && currentUser.getRole().getName() != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + currentUser.getRole().getName()));
            }
            String principal = currentUser.getUsername() != null ? currentUser.getUsername() : String.valueOf(currentUser.getId());
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);
        }

        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher != null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
            authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
            String principal = currentTeacher.getTeacherNo() != null ? currentTeacher.getTeacherNo() : String.valueOf(currentTeacher.getId());
            return new UsernamePasswordAuthenticationToken(principal, null, authorities);
        }

        return null;
    }
}
