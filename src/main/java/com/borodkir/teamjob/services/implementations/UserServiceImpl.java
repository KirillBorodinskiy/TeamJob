package com.borodkir.teamjob.services.implementations;

import com.borodkir.teamjob.UserDetailsImpl;
import com.borodkir.teamjob.data.User;
import com.borodkir.teamjob.data.repositories.UserRepository;
import com.borodkir.teamjob.services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService, UserDetailsService {
    UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(String.format("User %s not found", username)));
        return UserDetailsImpl.build(user);
    }
}
