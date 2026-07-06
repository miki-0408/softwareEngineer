package org.example.netdisk.Service.Inter;

import org.example.PCOI.ResponseDTO.R_SearchDTO;


public interface SearchService {
    R_SearchDTO searchById(String keyword);
    R_SearchDTO searchByName(String keyword);
    R_SearchDTO searchByTag(String keyword);
}
