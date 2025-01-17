package io.datajek.spring.basics.teamjob.data;

import lombok.Data;


@Data
public class SigninRequest {
    private String username;
    private String password;

}
