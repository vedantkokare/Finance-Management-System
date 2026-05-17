package com.finance.finance_management_system.security;

import com.finance.finance_management_system.entity.AuthProvider;
import com.finance.finance_management_system.entity.Role;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        String firstName = (String) attributes.getOrDefault("given_name", "");
        String lastName = (String) attributes.getOrDefault("family_name", "");
        if (firstName.isEmpty()) {
            String name = (String) attributes.getOrDefault("name", "User");
            String[] parts = name.split(" ");
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        }

        String picture = (String) attributes.get("picture");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() == null || user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(AuthProvider.GOOGLE);
            }
            if (picture != null && (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().isEmpty())) {
                user.setProfilePictureUrl(picture);
            }
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .role(Role.ROLE_USER)
                    .provider(AuthProvider.GOOGLE)
                    .profilePictureUrl(picture)
                    .build();
            user = userRepository.save(user);
            log.info("New OAuth2 user registered automatically: {}", email);
        }

        return new CustomUserDetails(user, attributes);
    }
}
