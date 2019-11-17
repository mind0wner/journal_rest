package com.journal.business.service.impl;

import com.journal.business.service.MailSender;
import com.journal.business.service.UserService;
import com.journal.data.dto.UpdateUserDto;
import com.journal.data.dto.UserDto;
import com.journal.data.entities.Group;
import com.journal.data.entities.Role;
import com.journal.data.entities.User;
import com.journal.data.exceptions.UserNotFoundException;
import com.journal.data.repository.GroupRepository;
import com.journal.data.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MailSender mailSender;

    public List<UserDto> findAll() {
        log.info("Retrieving all users");
        return userRepository.findAllOrderedByNumberInGroupList().stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean register(User user) {

        User userFromDB = userRepository.findByUsername(user.getUsername());

        if (userFromDB != null) {
            return false;
        }

        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Collections.singleton(Role.ANON));
        user.setActivationCode(UUID.randomUUID().toString());
        userRepository.saveAndFlush(user);
        log.info("Saved new user {}", user.toString());

        if (!StringUtils.isEmpty(user.getEmail())) {
            String message = String.format(
                    "Hello, %s! \n" +
                            "Welcome to CI-161's group site. \n" +
                            "If you don`t want just ignore message. \n" +
                            "Please, visit next link: http://localhost:8080/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );
            mailSender.send(user.getEmail(), "Activation code", message);
        }

        return true;
    }

    public boolean activateUser(String code) {
        User user = userRepository.findByActivationCode(code);

        if (user == null) {
            return false;
        }

        user.setGroup(groupRepository.getOne(1L));
        user.setActivationCode(null);

        user.getRoles().clear();
        user.getRoles().add(Role.STUDENT);

        userRepository.saveAndFlush(user);

        return true;
    }

    @Override
    public void update(UpdateUserDto dto, Long id) {
        User user = findById(id);
        user.getRoles().clear();
        user.setRoles(new HashSet<>(dto.getRoles()));
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setNumberInGroupList(dto.getId());

        userRepository.saveAndFlush(user);
    }

    @Override
    public User findById(Long id) {
        log.info("Retrieving user by id {}", id);

        return userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User with id " + id + " not found!"));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting user with id {}", id);
        userRepository.deleteById(id);
    }

    public User findUserByUsername(String username) {
        log.info("Retrieving user by username {}", username);
        return userRepository.findByUsername(username);
    }

}