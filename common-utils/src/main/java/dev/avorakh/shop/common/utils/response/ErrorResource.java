package dev.avorakh.shop.common.utils.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResource {

    String message;
    Integer errorCode;
    List<ErrorDetail> errors;
}
