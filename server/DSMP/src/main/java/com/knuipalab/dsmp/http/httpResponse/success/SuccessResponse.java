package com.knuipalab.dsmp.http.httpResponse.success;

import com.knuipalab.dsmp.http.httpResponse.BasicResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class SuccessResponse extends BasicResponse {

    private LocalDateTime timestamp ;

    private int status;

    public SuccessResponse() {
        this.timestamp =  LocalDateTime.now();
        this.status = HttpStatus.OK.value();
    }
}