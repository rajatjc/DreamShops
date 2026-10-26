package com.dailycodework.dreamshops.dto;

import com.dailycodework.dreamshops.model.Role;
import com.dailycodework.dreamshops.model.User;

import java.util.Set;

public record UserDto(Long id, String firstName, String lastName, String email, Set<Role> roles) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), Set.copyOf(user.getRoles()));
    }
}
