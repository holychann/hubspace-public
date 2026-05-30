package com.example.backend.domain.user.controller;

import com.example.backend.domain.user.dto.UserRequestDTO;
import com.example.backend.domain.user.dto.UserResponseDTO;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 자체 로그인 유저 존재 확인
    @PostMapping("/exists")
    public ResponseEntity<Boolean> exists(
            @Validated(UserRequestDTO.existGroup.class) @RequestBody UserRequestDTO dto
    ){

        return ResponseEntity.ok(userService.existsByUsername(dto));

    }

    // 회원가입
    @PostMapping
    public ResponseEntity<UserResponseDTO.Join> join(
            @Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto) {

        UserResponseDTO.Join responseBody = userService.addUser(dto);

        return ResponseEntity.status(201).body(responseBody);
    }


    // 유저 정보
    @GetMapping
    public ResponseEntity<UserResponseDTO.ResponseDTO> readUser() {
        return ResponseEntity.ok(userService.readUser());
    }


    // 유저 수정 (자체 로그인 유저만)
    @PutMapping
    public ResponseEntity<UserResponseDTO.UpdatedUserDTO> updateUser(
            @Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto) throws AccessDeniedException {
        UserResponseDTO.UpdatedUserDTO updatedUserDTO = userService.updateUser(dto);

        return ResponseEntity.ok(updatedUserDTO);

    }

    // 유저 제거 (자체/소셜)
    @DeleteMapping
    public ResponseEntity<Boolean> deleteUserApi(
            @Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto
    ) throws AccessDeniedException {

        userService.deleteUser(dto);
        return ResponseEntity.status(200).body(true);
    }


}
