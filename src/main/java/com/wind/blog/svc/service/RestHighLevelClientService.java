package com.wind.blog.svc.service;

import com.alibaba.fastjson.JSONObject;
import com.wind.blog.model.Blog;
import com.wind.blog.svc.model.es.BlogESVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * RestHighLevelClientService
 *
 * @author qianchun 2018/9/29
 **/
@Service
public class RestHighLevelClientService {
    private final static Logger logger = LoggerFactory.getLogger(RestHighLevelClientService.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 根据主键查询
     * 
     * @param index index
     * @param type type
     * @param docId docId
     * @return 返回结果
     */
    public BlogESVO findById(String index, String type, String docId) {
        try {
            if (StringUtils.isEmpty(index) || StringUtils.isEmpty(type) || StringUtils.isEmpty(docId)) {
                return null;
            }

            BlogESVO blog = null;
            GetRequest request = new GetRequest();
            request.index(index);
            request.type(type);
            request.id(docId);
            GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            if (response.isExists()) {
                blog = JSONObject.toJavaObject(JSONObject.parseObject(response.getSourceAsString()), BlogESVO.class);
            }
            return blog;
        } catch (IOException e) {
            logger.error("findById 主键查询异常, index={}, type={}, docId={}", index, type, docId, e);
        }
        return null;
    }

    /**
     * 根据主键判断是否存在
     * 
     * @param index index
     * @param type type
     * @param docId docId
     * @return 返回结果
     */
    public boolean existsById(String index, String type, String docId) {
        try {
            if (StringUtils.isEmpty(index) || StringUtils.isEmpty(type) || StringUtils.isEmpty(docId)) {
                return false;
            }
            GetRequest request = new GetRequest();
            request.index(index);
            request.type(type);
            request.id(docId);
            return restHighLevelClient.exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("exists 主键是否存在校验查询异常, index={}, type={}, docId={}", index, type, docId, e);
        }
        return false;
    }

    /**
     * 根据主键删除
     * 
     * @param index index
     * @param type type
     * @param docId docId
     * @return 返回结果
     */
    public boolean deleteById(String index, String type, String docId) {
        try {
            if (StringUtils.isEmpty(index) || StringUtils.isEmpty(type) || StringUtils.isEmpty(docId)) {
                return false;
            }
            DeleteRequest request = new DeleteRequest();
            request.index(index);
            request.type(type);
            request.id(docId);
            DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);

            ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
            // 处理成功分片数量少于总分片数量的情况
            // if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            // logger.info("delete 主键删除数量少于总分片, total={}, sucess={}", shardInfo.getTotal(), shardInfo.getSuccessful());
            // }
            // 分片操作失败
            if (shardInfo.getFailed() > 0) {
                for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                    logger.info("create 主键删除分片新增失败, nodeId={}, shardId={}, reason={}", failure.nodeId(),
                            failure.shardId(), failure.reason());
                }
            }
            return true;
        } catch (IOException e) {
            logger.error("delete 主键删除异常, index={}, type={}, docId={}", index, type, docId, e);
        }
        return false;
    }

    /**
     * 新增
     * 
     * @param index index
     * @param type type
     * @param blog blog
     * @return 返回结果
     */
    public boolean add(String index, String type, Blog blog) {
        try {
            if (StringUtils.isEmpty(index) || StringUtils.isEmpty(type) || blog == null || blog.getId() == null) {
                return false;
            }
            IndexRequest request = new IndexRequest(index, type, blog.getId().toString());
            XContentBuilder builder =  createXContentBuilder(blog);
            if(builder == null) {
                return false;
            }
            request.source(builder);
            IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            if (response.getResult() == DocWriteResponse.Result.CREATED) {
                // 处理（如果需要）第一次创建文档的情况
                logger.info("create 新增成功");
            }

            ReplicationResponse.ShardInfo shardInfo = response.getShardInfo();
            // 处理成功分片数量少于总分片数量的情况
            // if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            // logger.info("create 成功数量少于总分片");
            // }
            // 分片操作失败
            if (shardInfo.getFailed() > 0) {
                for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                    logger.info("create 分片新增失败, nodeId={}, shardId={}, reason={}", failure.nodeId(), failure.shardId(),
                            failure.reason());
                }
            }
            logger.info("create 成功, id={}, response={}", response.getId(), JSONObject.toJSON(response));
            return true;
        } catch (Exception e) {
            logger.error("create 异常, index={}, type={}, blog={}", index, type, JSONObject.toJSON(blog), e);
        }
        return false;
    }

    /**
     * 批量增加
     * @param index index
     * @param type type
     * @param blogList list
     * @return 返回结果
     */
    public boolean bulkAdd(String index, String type, List<Blog> blogList) {
        try {
            if (StringUtils.isEmpty(index) || StringUtils.isEmpty(type) || CollectionUtils.isEmpty(blogList)) {
                return false;
            }
            BulkRequest request = new BulkRequest();
            for(Blog blog : blogList) {
                XContentBuilder builder = createXContentBuilder(blog);
                if(builder ==null) {
                    continue;
                }
                request.add(new IndexRequest(index, type, blog.getId().toString()).source(builder));
            }
            BulkResponse response = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            for(BulkItemResponse bulkItemResponse : response) {
                DocWriteResponse itemResponse = bulkItemResponse.getResponse();
                if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.INDEX
                        || bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE) {
                    IndexResponse indexResponse = (IndexResponse) itemResponse;
                    ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
                    // 处理成功分片数量少于总分片数量的情况
                    // if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    // logger.info("create 成功数量少于总分片");
                    // }
                    // 分片操作失败
                    if (shardInfo.getFailed() > 0) {
                        for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                            logger.info("bulk add 部分分片失败, nodeId={}, shardId={}, reason={}", failure.nodeId(), failure.shardId(),
                                    failure.reason());
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("bulk add 异常, index={}, type={}", index, type, e);
        }
        return false;
    }

    /**
     * 创建 XContentBuilder
     * @param blog blog
     * @return 返回结果
     * @throws Exception
     */
    private XContentBuilder createXContentBuilder(Blog blog) throws  Exception{
        if(blog ==null || blog.getId()==null) {
            return null;
        }
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.field("id", blog.getId());
            builder.field("tags", blog.getTags());
            builder.field("source", blog.getSource());
            builder.field("summary", blog.getSummary());
            builder.field("content", blog.getContent());
            builder.field("title", blog.getTitle());
            builder.field("status", blog.getStatus());
            builder.field("uid", blog.getUid());
            builder.timeField("create_time", blog.getCreateTime());
            builder.timeField("update_time", blog.getUpdateTime());
            builder.timeField("publish_time", blog.getPublishTime());
        }
        builder.endObject();
        return builder;
    }
}
