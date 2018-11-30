package com.wind.blog.svc.mapper;

import com.wind.blog.mapper.BlogMapper;
import com.wind.blog.model.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.JdbcType;

/**
 * BlogMapperEx
 *
 * @author qianchun 2018/8/28
 **/
@Mapper
public interface BlogMapperEx extends BlogMapper {

    String COLUMNS = " id, tags, from, title, summary, uid, create_time, update_time, publish_time, content ";

    @Select({ "select" + COLUMNS + "from blog where id = #{id}" })
    @Results({ @Result(column = "id", property = "id", jdbcType = JdbcType.BIGINT, id = true),
            @Result(column = "tags", property = "tags", jdbcType = JdbcType.VARCHAR),
            @Result(column = "from", property = "from", jdbcType = JdbcType.INTEGER),
            @Result(column = "title", property = "title", jdbcType = JdbcType.VARCHAR),
            @Result(column = "summary", property = "summary", jdbcType = JdbcType.VARCHAR),
            @Result(column = "uid", property = "uid", jdbcType = JdbcType.BIGINT),
            @Result(column = "create_time", property = "createTime", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "update_time", property = "updateTime", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "publish_time", property = "publishTime", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "content", property = "content", jdbcType = JdbcType.LONGVARCHAR) })
    Blog findById(String url);
}
