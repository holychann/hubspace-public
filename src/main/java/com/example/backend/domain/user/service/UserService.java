package com.example.backend.domain.user.service;

import com.example.backend.domain.jwt.service.JwtService;
import com.example.backend.domain.user.dto.CustomOAuth2User;
import com.example.backend.domain.user.dto.UserRequestDTO;
import com.example.backend.domain.user.dto.UserResponseDTO;
import com.example.backend.domain.user.entity.SocialProviderType;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.domain.user.entity.UserRoleType;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    // 자체 로그인 회원 가입 (존재 여부)
    @Transactional(readOnly = true)
    public boolean existsByUsername(UserRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }

    // 자체 로그인 회원 가입
    @Transactional
    public UserResponseDTO.Join addUser(UserRequestDTO dto) {
        // 중복 검증
        if(userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 회원입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        UserEntity userEntity = UserEntity.of(dto, encodedPassword);

        Long id = userRepository.save(userEntity).getId();

        return UserResponseDTO.Join.builder()
                .id(id)
                .build();

    }

    // 자체 로그인
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity userEntity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .roles(userEntity.getRoleType().name())
                .accountLocked(userEntity.getIsLock())
                .build();

    }

    // 자체 로그인 회원 정보 수정
    @Transactional
    public UserResponseDTO.UpdatedUserDTO updateUser(UserRequestDTO dto) throws AccessDeniedException {
        String sessiongUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!sessiongUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("수정할 회원의 정보가 일치하지 않습니다.");
        }

        UserEntity userEntity = userRepository.findByUsernameAndIsLockAndIsSocial(dto.getUsername(), false, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        userEntity.updateUser(dto);

        return UserResponseDTO.UpdatedUserDTO.of(userEntity);
    }

    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {
        SecurityContext context = SecurityContextHolder.getContext();
        String sessiongUsername = context.getAuthentication().getName();
        String sessiongRole = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessiongUsername.equals(dto.getUsername());
        boolean isAdmin = sessiongRole.equals("ROLE_ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제 가능합니다.");
        }

        userRepository.deleteByUsername(dto.getUsername());

        jwtService.removeRefreshByUsername(dto.getUsername());

    }

    // 소셜 로그인 (매 로그인시 : 신규 = 가입, 기존 = 업데이트)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 데이터
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String username;
        String role = UserRoleType.USER.name();
        String email;
        String nickname;


        String SocialType = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        if(SocialType.equals(SocialProviderType.GOOGLE.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            username = SocialType + "_" + attributes.get("sub");
            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();

        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 제공자 입니다.");
        }

        Optional<UserEntity> userEntity = userRepository.findByUsernameAndIsSocial(username, true);

        if(userEntity.isPresent()) {
            role = userEntity.get().getRoleType().name();
            UserRequestDTO userRequestDTO = UserRequestDTO.ofSocial(nickname, email);

            userEntity.get().updateUser(userRequestDTO);
        } else {
            UserRequestDTO userRequestDTO = UserRequestDTO.ofSocial(username, nickname, email);
            UserEntity newEntity = UserEntity.ofSocial(userRequestDTO, SocialProviderType.valueOf(SocialType));

            userRepository.save(newEntity);
        }

        authorities = List.of(new SimpleGrantedAuthority(role));

        return new CustomOAuth2User(attributes, authorities, username);

    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDTO.ResponseDTO readUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity userEntity = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        return UserResponseDTO.ResponseDTO.of(userEntity);
    }

    // username 으로 엔티티 조회
    @Transactional(readOnly = true)
    public UserEntity findUserByUsername(String username) {
        return userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public UserEntity findUserBySocialEmail(String email) {
        return userRepository.findByEmailAndIsSocial(email, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));
    }

    @Transactional
    public void addGoogleAccessToken(String username, String accessToken) {
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        user.updateAccessToken(accessToken);
    }

    @Transactional
    public void updateAccessToken(String username, String accessToken, LocalDateTime accessTokenExpiresAt) {
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        user.updateAccessToken(accessToken);
        user.updateAccessTokenExpiresAt(accessTokenExpiresAt);
    }

    @Transactional
    public void addGoogleRefreshToken(String username, String refreshToken) {
        UserEntity user = userRepository.findByUsernameAndIsLock(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원이 존재하지 않습니다."));

        user.updateGoogleRefreshToken(refreshToken);
    }
}
