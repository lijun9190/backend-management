package com.example.auth.service;

import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.RefreshTokenDTO;
import com.example.auth.dto.UpdatePasswordDTO;
import com.example.auth.vo.LoginVO;
import com.example.auth.vo.UserProfileVO;

import javax.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginVO login(LoginDTO dto, HttpServletRequest request);

    LoginVO refreshToken(RefreshTokenDTO dto);

    void logout(String authorization);

    UserProfileVO getCurrentUserProfile();

    void updatePassword(UpdatePasswordDTO dto);
}
