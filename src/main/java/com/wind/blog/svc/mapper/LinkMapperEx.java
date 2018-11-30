package com.wind.blog.svc.mapper;

import com.wind.blog.mapper.LinkMapper;
import com.wind.blog.model.Link;
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
public interface LinkMapperEx extends LinkMapper {

    String COLUMNS = " id, source, url, is_parse, blog_id ";

    @Select({ "select" + COLUMNS + "from link where url = #{url}" })
    @Results({ @Result(column = "id", property = "id", jdbcType = JdbcType.BIGINT, id = true),
            @Result(column = "from", property = "from", jdbcType = JdbcType.INTEGER),
            @Result(column = "url", property = "url", jdbcType = JdbcType.VARCHAR),
            @Result(column = "is_parse", property = "isParse", jdbcType = JdbcType.INTEGER),
            @Result(column = "blog_id", property = "blogId", jdbcType = JdbcType.BIGINT) })
    Link findByUrl(String url);
}
