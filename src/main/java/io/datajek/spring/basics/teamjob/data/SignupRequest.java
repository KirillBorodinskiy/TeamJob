package io.datajek.spring.basics.teamjob.data;

import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String email;
    private String password;

}