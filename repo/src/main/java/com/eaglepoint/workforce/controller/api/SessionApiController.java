package com.eaglepoint.workforce.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/session")
public class SessionApiController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> session(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("authenticated", true);
        info.put("username", auth.getName());
        info.put("roles", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        return ResponseEntity.ok(info);
    }
}
