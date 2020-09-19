package com.jiang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-19 20:29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person {

    private Integer id;

    private String name;

    private Integer age;

}