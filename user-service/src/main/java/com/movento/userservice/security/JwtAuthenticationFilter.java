package com.movento.userservice.security;

import com.movento.userservice.config.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = parseJwt(request);
        if (jwt != null) {
            // One signature check, no database round trip: the token already carries the account id, email and
            // roles (it lives 15 minutes; disabling an account takes effect when it expires, as at the gateway).
            jwtUtils.parseValidClaims(jwt).ifPresent(claims -> {
                Object rawAccountId = claims.get("accountId");
                if (!(rawAccountId instanceof Number accountId)) return;
                Object rawRoles = claims.get("roles");
                java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                        rawRoles instanceof java.util.Collection<?> roles
                                ? roles.stream().map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(String.valueOf(role))).toList()
                                : java.util.List.of();
                UserDetailsImpl principal = new UserDetailsImpl(accountId.longValue(), claims.getSubject(), null, authorities);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
