package com.example.backend.domain.user.dto;

import com.example.backend.domain.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserResponseDTO {

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseDTO {
        private String username;
        private Boolean isSocial;
        private String nickname;
        private String email;

        public static ResponseDTO of(UserEntity entity) {
            return ResponseDTO.builder()
                    .username(entity.getUsername())
                    .isSocial(entity.getIsSocial())
                    .nickname(entity.getNickname())
                    .email(entity.getEmail())
                    .build();
        }
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdatedUserDTO {
        private Long id;

        public static UpdatedUserDTO of(UserEntity entity) {
            return UpdatedUserDTO.builder()
                    .id(entity.getId())
                    .build();
        }

    }


    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Join {
        private Long id;

        public static Join of(Long id) {
            return Join.builder()
                    .id(id)
                    .build();
        }
    }

}
