package com.nabarup.college;

public class HelperClass {
    String name, email, phone_no, password;

    public HelperClass(String name, String email, String phone_no, String password) {
        this.name = name;
        this.email = email;
        this.phone_no = phone_no;
        this.password = password;
    }

    public HelperClass() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone_no() {
        return phone_no;
    }

    public void setPhone_no(String phone_no) {
        this.phone_no = phone_no;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
