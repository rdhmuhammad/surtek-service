package com.sabarceria.surtek.exceptions;

public class InvalidDataException extends RuntimeException{

    public InvalidDataException(){
        super();
    }

    public InvalidDataException(String msg){
        super(msg);
    }
}
