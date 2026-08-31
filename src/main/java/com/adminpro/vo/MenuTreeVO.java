package com.adminpro.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeVO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String perms;
    private Integer menuType;
    private String path;
    private Integer sort;
    private Integer status;
    private List<MenuTreeVO> children = new ArrayList<>();
}
