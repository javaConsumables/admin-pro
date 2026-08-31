package com.adminpro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MenuIdsRequest {
    @NotNull(message = "菜单ID列表不能为空")
    private List<Long> menuIds;
}
