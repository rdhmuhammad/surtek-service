package com.sabarceria.surtek.exceptions;

public class DataNotFoundException extends RuntimeException{

    public DataNotFoundException(){
        super();
    }

    public DataNotFoundException(String msg){
        super(msg);
    }
}
