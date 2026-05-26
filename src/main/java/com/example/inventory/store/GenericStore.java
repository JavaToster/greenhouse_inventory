package com.example.inventory.store;

public interface GenericStore<T, ID>{
    T findById(ID id);
    T save(T t);
}
