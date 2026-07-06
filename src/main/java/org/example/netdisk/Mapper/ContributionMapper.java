package org.example.netdisk.Mapper;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.annotations.*;
import org.example.PCOI.Entity.Contribution;

import java.util.List;

@Mapper
public interface ContributionMapper {

    @Insert("INSERT INTO contribution(type,title,image,description,authorId) VALUES(#{type},#{title},#{image, typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler},#{description},#{authorId})")
    @SelectKey(statement = "SELECT contributionId FROM contribution WHERE id = LAST_INSERT_ID()", keyProperty = "contributionId", before = false, resultType = String.class)
    void insertContribution(Contribution contribution);

    // 映射 image 列为 List<String>，使用 JacksonTypeHandler
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE contributionId = #{contributionId} AND status = 0 AND auditStatus = 1")
    Contribution selectContributionById(String contributionId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE contributionId = #{contributionId} AND status = 0 AND auditStatus = 0")
    Contribution selectNoAuditContributionById(String contributionId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE contributionId = #{contributionId} AND status = 0 AND auditStatus = 2")
    Contribution selectDismissalContributionById(String contributionId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE contributionId = #{contributionId} AND status = 1")
    Contribution selectBannedContributionById(String contributionId);



    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE contributionId = #{contributionId} AND status = 0 AND auditStatus = 0")
    Contribution selectPendingContributionById(String contributionId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE authorId = #{authorId} AND status = 0 AND auditStatus = 1")
    List<Contribution> selectContributionsByAuthorId(String authorId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE auditStatus = #{auditStatus}")
    List<Contribution> selectContributionsByAuditStatus(int auditStatus);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE authorId = #{authorId} AND auditStatus = #{auditStatus}")
    List<Contribution> selectContributionsByAuthorIdAndAuditStatus(@Param("authorId") String authorId, @Param("auditStatus") int auditStatus);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = #{status} ")
    List<Contribution> selectContributionsByStatus(int status);

    @Delete("DELETE FROM contribution WHERE contributionId = #{contributionId} ")
    void deleteContributionById(String contributionId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE type = #{type} AND status = 0 AND auditStatus = 1")
    List<Contribution> selectContributionsByType(int type);

    // 按类型并按浏览量排序
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = 0 AND auditStatus = 1 AND type = #{type} ORDER BY viewCount DESC LIMIT #{limit}")
    List<Contribution> selectContributionsByTypeAndViewCount(@Param("type") int type, @Param("limit") int limit);

    // 按类型并按收藏量排序
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = 0 AND auditStatus = 1 AND type = #{type} ORDER BY favoriteCount DESC LIMIT #{limit}")
    List<Contribution> selectContributionsByTypeAndFavoriteCount(@Param("type") int type, @Param("limit") int limit);

    // 按类型并按点赞量排序
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = 0 AND auditStatus = 1 AND type = #{type} ORDER BY likeCount DESC LIMIT #{limit}")
    List<Contribution> selectContributionsByTypeAndLikeCount(@Param("type") int type, @Param("limit") int limit);

    // 按类型并按评论量排序
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = 0 AND auditStatus = 1 AND type = #{type} ORDER BY commentCount DESC LIMIT #{limit}")
    List<Contribution> selectContributionsByTypeAndCommentCount(@Param("type") int type, @Param("limit") int limit);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Update("UPDATE contribution SET type = #{type}, title = #{title}, image = #{image, typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}, description = #{description}, status = #{status}, auditStatus = #{auditStatus}, publishTime = #{publishTime}, authorId = #{authorId}, viewCount = #{viewCount}, favoriteCount = #{favoriteCount}, likeCount = #{likeCount}, commentCount = #{commentCount}, dismissalReason = #{dismissalReason} WHERE contributionId = #{contributionId}")
    void updateContribution(Contribution contribution);


//    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
//    @Select("""
//            SELECT * FROM contribution
//            WHERE MATCH(title) AGAINST(#{titleKeyword} IN NATURAL LANGUAGE MODE)
//            AND status = 0 AND auditStatus = 1
//            ORDER BY MATCH(title) AGAINST(#{titleKeyword} IN NATURAL LANGUAGE MODE) DESC
//            LIMIT #{limit}
//            """)
//    List<Contribution> selectContributionsByTitle(
//            @Param("limit") int limit,
//            @Param("titleKeyword") String titleKeyword
//    );

    @Results({
            @Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)
    })
    @Select("""
        SELECT *
        FROM contribution
        WHERE
            title LIKE CONCAT('%', #{titleKeyword}, '%')  -- 核心：子串匹配
            AND status = 0
            AND auditStatus = 1
        ORDER BY publishTime DESC  -- 按时间倒序（最新优先）
        LIMIT #{limit}
    """)
    List<Contribution> selectContributionsByTitle(
            @Param("limit") int limit,
            @Param("titleKeyword") String titleKeyword
    );

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("""
    SELECT c.*
    FROM contribution c
    JOIN tag_relation tr ON c.contributionId = tr.contributionId
    JOIN tag t ON tr.tagId = t.id
    WHERE t.tagName = #{tagName}
    AND c.auditStatus = 1 AND c.status = 0
    ORDER BY c.publishTime DESC
    """)
    List<Contribution> selectContributionsByTag(String tagName);


    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("""
    SELECT c.*
    FROM contribution c
    JOIN favorite fc ON c.contributionId = fc.contributionId
    WHERE fc.userId = #{userId}
    AND c.auditStatus = 1 AND c.status = 0
    ORDER BY  c.publishTime DESC
""")
    List<Contribution> selectFavoriteContributionsByUserId(String userId);

    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("""
    SELECT c.*
    FROM contribution c
    JOIN likes lc ON c.contributionId = lc.contributionId
    WHERE lc.userId = #{userId}
    AND c.auditStatus = 1 AND c.status = 0
    ORDER BY  c.publishTime DESC
    """)
    List<Contribution> selectLikeContributionsByUserId(String userId);

    // 获取所有审核通过且未封禁的作品
    @Results({@Result(property = "image", column = "image", typeHandler = JacksonTypeHandler.class)})
    @Select("SELECT * FROM contribution WHERE status = 0 AND auditStatus = 1 ORDER BY publishTime DESC")
    List<Contribution> selectAllContributions();
}
