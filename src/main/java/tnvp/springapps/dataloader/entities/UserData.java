package tnvp.springapps.dataloader.entities;

import lombok.Data;

import java.util.Date;

@Data
public class UserData {
    private long id;
    private String name;
    private int age;
    private String email;
    private Date signup_date;
}
