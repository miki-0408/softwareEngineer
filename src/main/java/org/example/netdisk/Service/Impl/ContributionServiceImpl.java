package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.PCOI.Entity.Comment;
import org.example.PCOI.Entity.Contribution;
import org.example.PCOI.Entity.Tag;
import org.example.PCOI.Entity.User;
import org.example.PCOI.Mapper.*;
import org.example.PCOI.ResponseDTO.*;
import org.example.PCOI.Service.Inter.ContributionService;
import org.example.PCOI.Service.Support.FileStorageService;
import org.example.PCOI.Service.Support.TransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.example.PCOI.Service.Support.Enum.*;

@Slf4j
@Service
public class ContributionServiceImpl implements ContributionService {
    @Autowired
    private ContributionMapper contributionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TransformService transformService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LikeMapper likeMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TagRelationMapper tagRelationMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<R_OverviewContribution> getIllustrations() {
        List<Contribution> contributions = contributionMapper.selectContributionsByType(illustration);
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for (Contribution contribution : contributions) {
            User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public List<R_OverviewContribution> getMangas() {
        List<Contribution> contributions = contributionMapper.selectContributionsByType(manga);
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for (Contribution contribution : contributions) {
            User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public List<R_OverviewContribution> getAllContributions() {
        List<Contribution> contributions = contributionMapper.selectAllContributions();
        List<R_OverviewContribution> rOverviewContributions = new ArrayList<>();
        for (Contribution contribution : contributions) {
            User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
            R_OverviewContribution rOverviewContribution = transformService.transformContributionToROverviewContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername());
            rOverviewContributions.add(rOverviewContribution);
        }
        return rOverviewContributions;
    }

    @Override
    public R_ContributionDTO getContribution(String userId, String contributionId) {
        Contribution contribution = contributionMapper.selectContributionById(contributionId);
        User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
        List<Comment> comments = commentMapper.selectCommentsByContributionId(contributionId);
        boolean isLiked = likeMapper.isLike(userId, contributionId);
        boolean isFavorite = favoriteMapper.isFavorite(userId, contributionId);
        List<Integer> tagIds = tagRelationMapper.getContributionTags(contributionId);
        List<Tag> tags = new ArrayList<>();
        for(Integer tagId : tagIds){
            Tag tag = tagMapper.selectTagById(tagId);
            tags.add(tag);
        }
        R_Contribution rContribution = transformService.transformContributionToRContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername(), tags);
        List<R_ContributionComment> rContributionComments = new ArrayList<>();
        for (Comment comment : comments) {
            User commentUser = userMapper.selectUserById(comment.getAuthor());
            R_ContributionComment rContributionComment = transformService.transformCommentToRContributionComment(comment, commentUser.getAvatar(), commentUser.getUsername());
            rContributionComments.add(rContributionComment);
        }
        R_ContributionDTO rContributionDTO = new R_ContributionDTO();
        rContributionDTO.setContribution(rContribution);
        rContributionDTO.setComments(rContributionComments);
        rContributionDTO.setIsLiked(isLiked);
        rContributionDTO.setIsFavorite(isFavorite);
        contribution.setViewCount(contribution.getViewCount()+1);
        contributionMapper.updateContribution(contribution);
        return rContributionDTO;
    }

    @Override
    public R_Contribution getPendingContribution(String userId, Integer role, String contributionId) {
        Contribution contribution = contributionMapper.selectPendingContributionById(contributionId);
        if(contribution!=null && (userId.equals(contribution.getAuthorId())||role.equals(communityAdmin)))
        {
            User contributionUser = userMapper.selectUserById(contribution.getAuthorId());
            List<Integer> tagIds = tagRelationMapper.getContributionTags(contributionId);
            List<Tag> tags = new ArrayList<>();
            for(Integer tagId : tagIds){
                Tag tag = tagMapper.selectTagById(tagId);
                tags.add(tag);
            }
            return transformService.transformContributionToRContribution(contribution, contributionUser.getAvatar(), contributionUser.getUsername(), tags);
        }
        return null;
    }

    @Override
    public List<R_OverviewContribution> getContributionsRanking(Integer type, Integer key) {
        List<Contribution> contributions = switch (key) {
            case viewCount -> contributionMapper.selectContributionsByTypeAndViewCount(type, maxSearchLimit);
            case favoriteCount -> contributionMapper.selectContributionsByTypeAndFavoriteCount(type, maxSearchLimit);
            case likeCount -> contributionMapper.selectContributionsByTypeAndLikeCount(type, maxSearchLimit);
            case commentCount -> contributionMapper.selectContributionsByTypeAndCommentCount(type, maxSearchLimit);
            default -> List.of();
        };
        List<R_OverviewContribution> result = new ArrayList<>();
        for (Contribution c : contributions) {
            User author = userMapper.selectUserById(c.getAuthorId());
            String avatar = author == null ? null : author.getAvatar();
            String username = author == null ? null : author.getUsername();
            result.add(transformService.transformContributionToROverviewContribution(c, avatar, username));
        }
        return result;
    }

    @Override
    public boolean likeContribution(String userId, String contributionId) {
        if(userMapper.selectUserById(userId)==null || contributionMapper.selectContributionById(contributionId)==null)
            return false;
        if(!likeMapper.isLike(userId,contributionId)){
            likeMapper.insertLike(userId,contributionId);
            Contribution contribution = contributionMapper.selectContributionById(contributionId);
            contribution.setLikeCount(contribution.getLikeCount()+1);
            contributionMapper.updateContribution(contribution);
            return true;
        }
        return true;
    }

    @Override
    public boolean unlikeContribution(String userId, String contributionId) {
        if(userMapper.selectUserById(userId)==null || contributionMapper.selectContributionById(contributionId)==null)
            return false;
        if(likeMapper.isLike(userId,contributionId)){
            likeMapper.deleteLike(userId,contributionId);
            Contribution contribution = contributionMapper.selectContributionById(contributionId);
            contribution.setLikeCount(Math.max(0,contribution.getLikeCount()-1));
            contributionMapper.updateContribution(contribution);
            return true;
        }
        return true;
    }

    @Override
    public boolean favoriteContribution(String userId, String contributionId) {
        if(userMapper.selectUserById(userId)==null || contributionMapper.selectContributionById(contributionId)==null)
            return false;
        if(!favoriteMapper.isFavorite(userId,contributionId)){
            favoriteMapper.insertFavorite(userId,contributionId);
            Contribution contribution = contributionMapper.selectContributionById(contributionId);
            contribution.setFavoriteCount(contribution.getFavoriteCount()+1);
            contributionMapper.updateContribution(contribution);
            return true;
        }
        return true;
    }

    @Override
    public boolean unfavoriteContribution(String userId, String contributionId) {
        if(userMapper.selectUserById(userId)==null || contributionMapper.selectContributionById(contributionId)==null)
            return false;
        if(favoriteMapper.isFavorite(userId,contributionId)){
            favoriteMapper.deleteFavorite(userId,contributionId);
            Contribution contribution = contributionMapper.selectContributionById(contributionId);
            contribution.setFavoriteCount(Math.max(0,contribution.getFavoriteCount()-1));
            contributionMapper.updateContribution(contribution);
            return true;
        }
        return true;
    }

    @Override
    public boolean commentContribution(String userId, String contributionId, String comment) {
        if(userMapper.selectUserById(userId)==null || contributionMapper.selectContributionById(contributionId)==null)
            return false;
        Comment newComment = new Comment();
        newComment.setAuthor(userId);
        newComment.setContribution(contributionId);
        newComment.setDescription(comment);
        commentMapper.insertComment(newComment);
        Contribution contribution = contributionMapper.selectContributionById(contributionId);
        contribution.setCommentCount(contribution.getCommentCount()+1);
        contributionMapper.updateContribution(contribution);
        return true;
    }

    @Override
    public boolean uploadContribution(String userId, String title, Integer type, String description,List<String>tags, List<MultipartFile> images) {
        try {
            if (userMapper.selectUserById(userId) == null)
                return false;
            Contribution newContribution = new Contribution();
            newContribution.setAuthorId(userId);
            newContribution.setTitle(title);
            newContribution.setType(type);
            newContribution.setDescription(description);
            newContribution.setStatus(normal);
            newContribution.setAuditStatus(pending);
            newContribution.setViewCount(0);
            newContribution.setFavoriteCount(0);
            newContribution.setLikeCount(0);
            newContribution.setCommentCount(0);
            List<String> imagePath = fileStorageService.saveWorkImages(images, type, userId);
            newContribution.setImage(imagePath);
            contributionMapper.insertContribution(newContribution);
            if (tags != null) {
                for (String tagName : tags) {
                    if (!tagMapper.isTagExist(tagName)) {
                        tagMapper.insertTag(tagName);
                    }
                    Integer tagId = tagMapper.selectTagByName(tagName).getId();
                    tagRelationMapper.insertTagRelation(newContribution.getContributionId(), tagId);
                }
            }
            return true;
        }catch(IllegalArgumentException e){
            throw new RuntimeException("上传作品失败，文件类型错误", e);
        }
    }
}
