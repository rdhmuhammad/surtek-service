package com.sabarceria.surtek.base;

import com.sabarceria.surtek.commons.BeanCopy;
import com.sabarceria.surtek.constant.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

@NoRepositoryBean
public class BaseResController {

    @Autowired
    protected Validator validator;

    public BaseResController() {
    }

    protected <T> void validateValue(T request) {
        Set<ConstraintViolation<T>> oks = this.validator.validate(request);
        if (!oks.isEmpty()) {
            throw new ConstraintViolationException(oks);
        }
    }


    protected ResponseEntity<Response<?>> response() {
        Response<Object> response = new Response<>(AppConstants.Message.DEFAULT_SUCCESS, true);
        return ResponseEntity.ok(response);
    }

    protected <T> ResponseEntity<Response<T>> response(ServiceResolver<T> body) {
        Response<T> response = new Response<>(AppConstants.Message.DEFAULT_SUCCESS, true, body.getData());
        return ResponseEntity.ok(response);
    }

    protected ResponseEntity<Response> responseMessage(String msg) {
        Response<Object> response = new Response<>(msg, true);
        return ResponseEntity.ok(response);
    }

    protected <Dest, Src> ResponseEntity<Response<Dest>> response(ServiceResolver<Src> resolver, ResponseConverter<Src, Dest> converter){
        Dest convert = converter.convert(resolver.getData());
        Response<Dest> response = new Response<>(AppConstants.Message.DEFAULT_SUCCESS, true, convert);
        return ResponseEntity.ok(response);
    }

    protected <Dest, Src> ResponseEntity<Response<PageResponse<Dest>>> responsePage(ServiceResolver<Page<Src>> resolver, ResponseConverter<Page<Src>, PageResponse<Dest>> converter){
        PageResponse<Dest> convert = converter.convert(resolver.getData());
        Response<PageResponse<Dest>> response = new Response<>(AppConstants.Message.DEFAULT_SUCCESS, true, convert);
        return ResponseEntity.ok(response);
    }

    private <DestDt, SrcDt, Src extends Page<SrcDt>> ResponseConverter<Src, PageResponse<DestDt>> getPageConverter(Class<DestDt> dtClass) {
        return new ResponseConverter<>() {
            @Override
            protected PageResponse<DestDt> convert(Src src) {
                List<DestDt> destDts = BeanCopy.copyCollection(src.getContent(), dtClass);
                return new PageResponse<>(src, destDts);
            }
        };
    }

    private <Dest, Src> ResponseConverter<Src, Dest> getDetailConverter(Class<Dest> destClass) {
        return new ResponseConverter<>() {
            @Override
            protected Dest convert(Src src) {
                return BeanCopy.copy(src, destClass);
            }

        };
    }

    protected  <T, E> ResponseEntity<Response<E>> responseConvertDetail(ServiceResolver<T> resolver, Class<E> tClass)  {
        return response(resolver, getDetailConverter(tClass));
    }

    protected <T, E> ResponseEntity<Response<PageResponse<E>>> responseConvertPage(ServiceResolver<Page<T>> resolver, Class<E> tClass) {
        return responsePage(resolver, getPageConverter(tClass));
    }

}
