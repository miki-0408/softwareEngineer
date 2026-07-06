package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Entity.Contribution;
import org.example.PCOI.Entity.User;
import org.example.PCOI.Mapper.ContributionMapper;
import org.example.PCOI.Mapper.UserMapper;
import org.example.PCOI.ResponseDTO.R_OverviewContribution;
import org.example.PCOI.ResponseDTO.R_SearchDTO;
import org.example.PCOI.ResponseDTO.R_User;
import org.example.PCOI.Service.Inter.SearchService;
import org.example.PCOI.Service.Support.TransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.example.PCOI.Service.Support.Enum.*;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private ContributionMapper contributionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TransformService transformService;


    @Override
    public R_SearchDTO searchById(String keyword) {
        List<R_OverviewContribution> illustrations = new ArrayList<>();
        List<R_OverviewContribution> mangas = new ArrayList<>();
        List<R_User> rUsers = new ArrayList<>();
        R_SearchDTO rSearchDTO = new R_SearchDTO();
        User user = userMapper.selectUserById(keyword);
        if(user != null)
        {
            R_User rUser = transformService.transformUserToRUser(user);
            rUsers.add(rUser);
        }
        Contribution contribution = contributionMapper.selectContributionById(keyword);
        if(contribution!=null) {
            User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
            if (contribution.getType().equals(illustration))
                illustrations.add(rOverviewContribution);
            else if (contribution.getType().equals(manga))
                mangas.add(rOverviewContribution);
        }
        rSearchDTO.setIllustrations(illustrations);
        rSearchDTO.setMangas(mangas);
        rSearchDTO.setUsers(rUsers);
        return rSearchDTO;
    }

    @Override
    public R_SearchDTO searchByName(String keyword) {
        List<R_OverviewContribution> illustrations = new ArrayList<>();
        List<R_OverviewContribution> mangas = new ArrayList<>();
        List<R_User> rUsers = new ArrayList<>();
        R_SearchDTO rSearchDTO = new R_SearchDTO();
        List<Contribution> contributions = contributionMapper.selectContributionsByTitle(maxSearchLimit,keyword);
        List<User> users = userMapper.selectUsersByName(maxSearchLimit,keyword);
        if(!users.isEmpty())
        {
            for(User user : users){
                R_User rUser = transformService.transformUserToRUser(user);
                rUsers.add(rUser);
            }
        }
        if(!contributions.isEmpty()){
            for(Contribution contribution : contributions){
                User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
                R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
                if (contribution.getType().equals(illustration))
                    illustrations.add(rOverviewContribution);
                else if (contribution.getType().equals(manga))
                    mangas.add(rOverviewContribution);
            }
        }
        rSearchDTO.setIllustrations(illustrations);
        rSearchDTO.setMangas(mangas);
        rSearchDTO.setUsers(rUsers);
        return rSearchDTO;
    }

    @Override
    public R_SearchDTO searchByTag(String keyword) {
        List<R_OverviewContribution> illustrations = new ArrayList<>();
        List<R_OverviewContribution> mangas = new ArrayList<>();
        List<R_User> rUsers = new ArrayList<>();
        R_SearchDTO rSearchDTO = new R_SearchDTO();
        List<Contribution> contributions = contributionMapper.selectContributionsByTag(keyword);
        if(!contributions.isEmpty()){
            for(Contribution contribution : contributions){
                User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
                R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
                if (contribution.getType().equals(illustration))
                    illustrations.add(rOverviewContribution);
                else if (contribution.getType().equals(manga))
                    mangas.add(rOverviewContribution);
            }
        }
        rSearchDTO.setIllustrations(illustrations);
        rSearchDTO.setMangas(mangas);
        rSearchDTO.setUsers(rUsers);
        return rSearchDTO;
    }

}
