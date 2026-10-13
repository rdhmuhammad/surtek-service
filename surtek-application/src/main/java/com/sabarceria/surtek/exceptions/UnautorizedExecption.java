
package com.sabarceria.surtek.exceptions;

public class UnautorizedExecption extends RuntimeException{

    public UnautorizedExecption(){
        super();
    }

    public UnautorizedExecption(String msg){
        super(msg);
    }
}
