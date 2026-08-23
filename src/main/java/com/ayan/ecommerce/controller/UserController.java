package com.ayan.ecommerce.controller;

import com.ayan.ecommerce.dto.UpdateUserDTO;
import com.ayan.ecommerce.entity.User;
import com.ayan.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public User getProfile(){
        return userService.getLoggedInUser();
    }

    @PutMapping("/update")
    public String updateProfile(
            @Valid @RequestBody UpdateUserDTO dto
            ){
        userService.getProfileUpdate(dto);
        return "Profile Update";
    }

    @DeleteMapping("/delete")
    public String deleteAccount() {
        userService.deleteAccount();
        return "Account Deleted Successfully";
    }
}
