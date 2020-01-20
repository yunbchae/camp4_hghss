package com.smilegate.auth.service;

import com.smilegate.auth.exceptions.ExistEmailException;
import com.smilegate.auth.exceptions.NonExistEmailException;
import com.smilegate.auth.utils.JwtUtil;
import com.smilegate.auth.domain.User;
import com.smilegate.auth.dto.SigninRequestDto;
import com.smilegate.auth.dto.SigninResponseDto;
import com.smilegate.auth.dto.SignupRequestDto;
import com.smilegate.auth.exceptions.EmailNotExistException;
import com.smilegate.auth.exceptions.PasswordWrongException;
import com.smilegate.auth.repository.UserRepository;
import com.smilegate.auth.utils.MailUtil;
import com.smilegate.auth.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailUtil mailUtil;
    private final RedisUtil redisUtil;

    public SigninResponseDto signin(SigninRequestDto signinRequestDto) {
        String email = signinRequestDto.getEmail();
        String password = signinRequestDto.getPassword();

        // email 체크
        User user = (User) userRepository.findByEmail(email);
        if(user==null) throw new EmailNotExistException(email);

        // password 체크
        if(!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new PasswordWrongException();
        }

        // token 발급
        String accessToken = jwtUtil.createToken(user.getEmail(), new ArrayList<>(Collections.singleton(user.getGrade())), 30);
        String refreshToken = jwtUtil.createToken(user.getEmail(), new ArrayList<>(Collections.singleton(user.getGrade())), 60*24*14);

        return new SigninResponseDto(accessToken, refreshToken);
    }

    public void sendSignupMail(SignupRequestDto signupRequestDto) {
        String email = signupRequestDto.getEmail();
        String hashedPassword = passwordEncoder.encode(signupRequestDto.getPassword());

        if(userRepository.countUser(email) > 0) {
            throw new ExistEmailException(email);
        }

        String key = UUID.randomUUID().toString();
        User user = User.builder()
                        .email(email)
                        .hashedPassword(hashedPassword)
                        .grade("USER")
                        .build();

        if(mailUtil.sendSignupMail(key, user)) {
            redisUtil.set(key, user, 10);
        }
    }

    public void registerUser(String key) {
        User user = (User)redisUtil.get(key);
        // TODO : Random Nickname
        user.setNickname("test");
        if(userRepository.registerUser(user) > 0) {
            redisUtil.delete(key);
        }
    }

    public void sendPasswordMail(String email) {
        if(userRepository.countUser(email) == 0) {
            throw new NonExistEmailException(email);
        }

        String key = UUID.randomUUID().toString();

        if(mailUtil.sendPasswordMail(key, email)) {
            redisUtil.set(key, email, 10);
        }
    }

    public String getUpdatePasswordToken(String key) {
        String email = (String) redisUtil.get(key);
        List<String> roles = new ArrayList<>();
        String grade = "UPDATE_PASSWORD";
        roles.add(grade);

        String token = jwtUtil.createToken(email, roles, 10);

        redisUtil.delete(key);

        return token;
    }

    public void updatePassword(String email, String password) {
        if(userRepository.countUser(email) == 0) {
            throw new NonExistEmailException(email);
        }

        String hashedPassword = passwordEncoder.encode(password);

        userRepository.updatePassword(
                User.builder()
                    .email(email)
                    .hashedPassword(hashedPassword)
                    .build()
        );
    }

    public List<User> getUserList() {
        return userRepository.findUsers();
    }
}
