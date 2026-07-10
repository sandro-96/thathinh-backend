package vn.thathinh.exception;

import lombok.Getter;
import vn.thathinh.constant.ApiCode;

@Getter
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(ApiCode code) {
        super(code);
    }
}
