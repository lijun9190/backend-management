package com.example.auth.service;

import com.example.auth.dto.LoginDTO;
import com.example.auth.dto.UpdatePasswordDTO;
import com.example.auth.vo.LoginVO;
import com.example.auth.vo.UserProfileVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证服务接口。
 */
public interface AuthService {

    LoginVO login(LoginDTO dto, HttpServletRequest request);

    void logout(String token);

    UserProfileVO getCurrentUserProfile();

    void updatePassword(UpdatePasswordDTO dto);
}
