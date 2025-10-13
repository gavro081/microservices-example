package com.github.gavro081.orderservice.clients;

import com.github.gavro081.common.dto.UserDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/users/by-username/{username}")
    UserDetailDto getUserByUsername(@PathVariable("username") String username);
}
