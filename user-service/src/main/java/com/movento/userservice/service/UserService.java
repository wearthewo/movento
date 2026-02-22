package com.movento.userservice.service;

import com.movento.userservice.model.User;

public interface UserService {
    User getUserById(Long id);
    // Add other user-related methods as needed
}
