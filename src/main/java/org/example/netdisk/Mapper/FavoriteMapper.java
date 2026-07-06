package org.example.netdisk.Mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {
    @Insert("INSERT INTO favorite (userId, contributionId) VALUES (#{userId}, #{contributionId})")
    void insertFavorite(@Param("userId") String userId, @Param("contributionId") String contributionId);

    @Delete("DELETE FROM favorite WHERE userId = #{userId} AND contributionId = #{contributionId}")
    void deleteFavorite(@Param("userId") String userId, @Param("contributionId") String contributionId);

    @Select("SELECT EXISTS(SELECT 1 FROM favorite WHERE userId = #{userId} AND contributionId = #{contributionId})")
    boolean isFavorite(
            @Param("userId") String userId,
            @Param("contributionId") String contributionId
    );
}
