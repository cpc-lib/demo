package com.example.orderdemo.common;

public class ApiException extends RuntimeException {
  private final int code;

  public ApiException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static ApiException badRequest(String msg) {
    return new ApiException(400, msg);
  }

  public static ApiException notFound(String msg) {
    return new ApiException(404, msg);
  }

  public static ApiException conflict(String msg) {
    return new ApiException(409, msg);
  }
}
