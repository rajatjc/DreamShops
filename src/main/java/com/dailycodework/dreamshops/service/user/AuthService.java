package com.dailycodework.dreamshops.service.user;

import com.dailycodework.dreamshops.dto.TokenDto;
import com.dailycodework.dreamshops.dto.UserDto;
import com.dailycodework.dreamshops.exceptions.AlreadyExistsException;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Role;
import com.dailycodework.dreamshops.model.User;
import com.dailycodework.dreamshops.repository.UserRepository;
import com.dailycodework.dreamshops.request.LoginRequest;
import com.dailycodework.dreamshops.request.RegisterRequest;
import com.dailycodework.dreamshops.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Compared against when the email is unknown, so a miss takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        return UserDto.from(createUser(request.firstName(), request.lastName(), request.email(), request.password(), Set.of(Role.USER)));
    }

    @Transactional
    public User createUser(String firstName, String lastName, String email, String password, Set<Role> roles) {
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new AlreadyExistsException("An account with email " + normalized + " already exists");
        }
        return userRepository.save(new User(firstName.trim(), lastName.trim(), normalized, passwordEncoder.encode(password), roles));
    }

    @Transactional(readOnly = true)
    public TokenDto login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(request.email().trim());
        boolean matches = passwordEncoder.matches(request.password(), user.map(User::getPassword).orElse(dummyHash));
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return tokenService.issue(user.get());
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return userRepository.findById(id).map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }
}
