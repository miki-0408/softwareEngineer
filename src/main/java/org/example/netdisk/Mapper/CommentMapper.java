package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.PCOI.Entity.Comment;

import java.util.List;
@Mapper
public interface CommentMapper {
    @Insert("INSERT INTO comment (contribution, author, description) VALUES (#{contribution}, #{author}, #{description})")
    void insertComment(Comment comment);

    @Delete("DELETE FROM comment WHERE commentId = #{commentId}")
    void deleteCommentById(String commentId);

    @Select("SELECT * FROM comment WHERE commentId = #{commentId}")
    Comment selectCommentById(String commentId);

    @Select("SELECT * FROM comment WHERE contribution = #{contributionId} ORDER BY time DESC")
    List<Comment> selectCommentsByContributionId(String contributionId);

    @Select("SELECT * FROM comment WHERE author = #{authorId} ORDER BY time DESC")
    List<Comment> selectCommentsByAuthorId(String authorId);

}
