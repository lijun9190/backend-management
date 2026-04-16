package com.example.system.service;

import com.example.common.model.result.PageResult;
import com.example.system.dto.user.UserQueryDTO;
import com.example.system.dto.user.UserSaveDTO;
import com.example.system.vo.user.UserDetailVO;
import com.example.system.vo.user.UserPageVO;

import java.util.List;

public interface UserService {

    PageResult<UserPageVO> pageQuery(UserQueryDTO dto);

    UserDetailVO detail(Long id);

    void saveUser(UserSaveDTO dto);

    void updateUser(Long id, UserSaveDTO dto);

    void changeStatus(Long id, Integer status);

    void resetPassword(Long id, String password);

    void assignRoles(Long id, List<Long> roleIds);

    void kickout(Long id);

    void removeUser(Long id);
}
