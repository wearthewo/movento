package com.movento.contentservice.client;

import com.movento.contentservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
}
