package com.endava.wallet.controller;

import com.endava.wallet.entity.Authority;
import com.endava.wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @GetMapping
    public String userList(Model model) {
        logger.info("Call for users list page");
        model.addAttribute("users", userService.findAllUsers());
        return "userList";
    }

    @GetMapping("{userID}")
    public String userEditForm(@PathVariable Long userID, Model model) {
        logger.info("Call for user with id: {} edit page", userID);
        model.addAttribute("user", userService.findUserById(userID));
        model.addAttribute("authorities", Authority.values());
        return "userEdit";
    }

    @PostMapping
    public String userSave(
            @RequestParam String username,
            @RequestParam Map<String, String> form,
            @RequestParam Long userID
    ) {
        userService.updateUser(username, form, userID);
        logger.info("Saved user with id: {}", userID);
        return "redirect:/user";
    }

    @GetMapping("/delete/{userID}")
    public String userDelete(@PathVariable Long userID) {
        userService.deleteUserById(userID);
        logger.info("Deleted user with id: {}", userID);
        return "redirect:/user";
    }
}
