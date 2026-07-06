package org.example.netdisk.Controller;

import org.example.PCOI.ResponseDTO.R_SearchDTO;
import org.example.PCOI.ResponseDTO.Result;
import org.example.PCOI.Service.Inter.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class SearchController {
    @Autowired
    private SearchService searchService;

    @PostMapping("/searchById")
    public Result<R_SearchDTO> searchById(
            @RequestParam("keyword") String keyword){
        R_SearchDTO data = searchService.searchById(keyword);
        return Result.success(data);
    }

    @PostMapping("/searchByName")
    public Result<R_SearchDTO> searchByName(
            @RequestParam("keyword") String keyword){
        R_SearchDTO data = searchService.searchByName(keyword);
        return Result.success(data);
    }

    @PostMapping("/searchByTag")
    public Result<R_SearchDTO> searchByTag(
            @RequestParam("keyword") String keyword){
        R_SearchDTO data = searchService.searchByTag(keyword);
        return Result.success(data);
    }


}

