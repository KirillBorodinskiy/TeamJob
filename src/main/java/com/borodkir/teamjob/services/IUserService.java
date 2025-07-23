package com.borodkir.teamjob.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface IUserService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
} 