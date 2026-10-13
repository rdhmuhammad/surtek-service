package com.sabarceria.surtek.base;

public abstract class ResponseConverter<Src, Desc> {
    public ResponseConverter() {
    }

    protected abstract Desc convert(Src src);
}