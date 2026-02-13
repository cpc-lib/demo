package cc.ivera.api.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.Instant;

@Data
public class UserPageQuery {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(1000) // extra guard; real cap is in properties
    private Integer size;

    /**
     * keyword searches across uid/phone/email/nickname (partial match)
     */
    private String keyword;

    private String uid;
    private String phone;
    private String email;
    private String nickname;

    /**
     * support multiple statuses: e.g. status=1,0
     */
    private String status;

    /**
     * sort format: field,asc|desc;field2,asc|desc
     */
    private String sort;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedTo;
}
