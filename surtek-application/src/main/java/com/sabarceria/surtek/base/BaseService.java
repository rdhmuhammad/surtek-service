package com.sabarceria.surtek.base;


import com.sabarceria.surtek.exceptions.DataNotFoundException;
import com.sabarceria.surtek.exceptions.InvalidDataException;
import com.sabarceria.surtek.exceptions.UnautorizedExecption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

public class BaseService {

    private String SUCCESS;

    private String ERROR;

    private final Integer SUCCESS_CODE = 200;

    protected void setSuccess(String msg) {
        SUCCESS = msg;
    }

    protected void setError(String msg){
        ERROR = msg;
    }


    @Autowired
    protected Validator validator;

    public BaseService() {
    }

    protected <T> void validateValue(T request) {
        Set<ConstraintViolation<T>> oks = this.validator.validate(request);
        if (!oks.isEmpty()) {
            throw new ConstraintViolationException(oks);
        }
    }

    public <T> ServiceResolver<T> success() {
        return new ServiceResolver<>(SUCCESS, SUCCESS_CODE);
    }

    public <T> ServiceResolver<T> success(String msg) {
        return new ServiceResolver<>(msg, SUCCESS_CODE);
    }

    public <T> ServiceResolver<T> success(String msg, T data) {
        return new ServiceResolver<>(data, msg, SUCCESS_CODE);
    }

    public <T> ServiceResolver<T> success(T data) {
        return new ServiceResolver<>(data, SUCCESS, SUCCESS_CODE);
    }

    public <T extends Pageable, E extends Pageable> ServiceResolver<E> success(T data, ResponseConverter<T, E> converter) {
        return new ServiceResolver<>(converter.convert(data), SUCCESS, SUCCESS_CODE);
    }

    public <T extends Pageable, E extends Pageable> ServiceResolver<E> success(T data, ResponseConverter<T, E> converter, String msg) {
        return new ServiceResolver<>(converter.convert(data), msg, SUCCESS_CODE);
    }

    public void errorData(String message){
        throw new InvalidDataException(message);
    }
    public void unauthorized(String message){
        throw new UnautorizedExecption(message);
    }

    public void dataNotFound(String message){
        throw new DataNotFoundException(message);
    }

}
