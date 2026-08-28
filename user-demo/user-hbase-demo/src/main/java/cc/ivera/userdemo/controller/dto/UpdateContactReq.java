package cc.ivera.userdemo.controller.dto;

import lombok.Data;

@Data
public class UpdateContactReq {
  private String phone;
  private String email;
}
