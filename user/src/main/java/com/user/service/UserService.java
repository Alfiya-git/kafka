package com.user.service;

import com.user.ResponseBean.ResponseBean;
import com.user.dto.request.UserRequestBean;
import com.user.dto.response.UserResponseBean;
import com.user.entity.User;
import com.user.mapper.UserMapper;
import com.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Value("${kafka.name}")
    private String topicName;

    private UserRepository userRepository;

    private UserMapper userMapper;

    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ResponseBean addUser(UserRequestBean userRequestBean) {
        Boolean exists = this.userRepository.existsByUserNameAndUserGender(userRequestBean.getUserName(), userRequestBean.getGender());
        if (!exists) {
            User user = this.userMapper.requestEntityMapperCreate(userRequestBean);
            User userSave = this.userRepository.save(user);
            log.info("{}", userSave.toString());
            UserResponseBean userResponseBean = this.userMapper.entityResponseMapper(userSave);
            this.kafkaTemplate.send(this.topicName,"User Data Saved Successfully");
            return ResponseBean.builder().status(Boolean.TRUE).data(userResponseBean).build();
        }
        this.kafkaTemplate.send(this.topicName,"User Data Saved failed");
        return ResponseBean.builder().status(Boolean.TRUE).message(userRequestBean.getUserName() + " Already Exist").build();
    }

    public ResponseBean findAllUsers() {
        List<User> users = this.userRepository.findAll();
        log.info("{}", users.toString());
        List<UserResponseBean> userResponseBeans = users.stream()
                .map(user -> this.userMapper.entityResponseMapper(user))
                .collect(Collectors.toList());
        this.kafkaTemplate.send(this.topicName,"All User Data Received");
        return ResponseBean.builder().status(Boolean.TRUE).data(userResponseBeans).build();
    }

    public ResponseBean findUserById(Long userId) {
        Optional<User> userOptional = this.userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            this.kafkaTemplate.send(this.topicName,"User with used Id "+ userId +" Received Failed");
            return ResponseBean.builder().status(Boolean.TRUE).message("No user found with id " + userId).build();
        }
        User user = userOptional.get();
        UserResponseBean userResponseBean = this.userMapper.entityResponseMapper(user);
        this.kafkaTemplate.send(this.topicName,"User with used Id "+ userId +" Received Successfully");
        return ResponseBean.builder().data(userResponseBean).status(Boolean.TRUE).build();
    }

    public ResponseBean updateUser(UserRequestBean userRequestBean) {
        //fetching Data
        User user = this.userRepository.findByUserName(userRequestBean.getUserName()).get();
        User userUpdated = this.userMapper.requestEntityMapperUpdate(userRequestBean, user);
        log.info("{}",user);


        // Updating User
        log.info("{}",userUpdated);
        User UserSaved = this.userRepository.save(userUpdated);

        // Mapping to userResponse Bean
        UserResponseBean userResponseBean = this.userMapper.entityResponseMapper(UserSaved);
        this.kafkaTemplate.send(this.topicName,"User with used Id "+ user.getUserName() +" Updated Successfully");
        return ResponseBean.builder().status(Boolean.TRUE).data(userResponseBean).build();
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

}
