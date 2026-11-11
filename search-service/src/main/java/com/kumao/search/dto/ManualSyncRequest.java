package com.kumao.search.dto;

import com.kumao.api.dto.SearchDocDTO;
import lombok.Data;

import java.util.List;

@Data
public class ManualSyncRequest {
    private List<SearchDocDTO> docs;
}
