package com.knuipalab.dsmp.http.httpResponse.error.handler.exception;

import com.knuipalab.dsmp.http.httpResponse.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProjectNotFoundException extends RuntimeException{
    private final ErrorCode errorCode;
}
