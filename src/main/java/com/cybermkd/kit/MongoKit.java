package com.cybermkd.kit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.Block;
import com.mongodb.DBRef;
import com.mongodb.MongoClient;
import com.mongodb.assertions.Assertions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;


/**
 * 创建人:T-baby
 * 创建日期: 16/4/15
 * 文件描述:MongoDB操作工具类
 */
public enum MongoKit {

    /*
    *枚举实现单例模式
    */
    INSTANS;
    private static MongoClient client;
    private static MongoDatabase defaultDb;

    public MongoClient getClient() {
        return client;
    }

    public void init(MongoClient client, String database) {
        MongoKit.client = client;
        MongoKit.defaultDb = client.getDatabase(database);
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return defaultDb.getCollection(collectionName);
    }

    public long insert(String collectionName, List<Document> docs) {
        long before = getCollection(collectionName).count();
        getCollection(collectionName).insertMany(docs);
        return getCollection(collectionName).count() - before;
    }


    public long insert(String collectionName, Document doc) {
        long before = getCollection(collectionName).count();
        getCollection(collectionName).insertOne(doc);
        return getCollection(collectionName).count() - before;
    }

    public List<JSONObject> aggregate(String collectionName, List<Bson> query, boolean allowDiskUse) {

        final List<JSONObject> list = new ArrayList<JSONObject>();

        Block<Document> block = new Block<Document>() {

            public void apply(Document document) {
                document = iding(document);
                list.add((JSONObject) JSON.toJSON(document));
            }
        };

        getCollection(collectionName).aggregate(query).allowDiskUse(allowDiskUse).forEach(block);

        return list;
    }

    public <T> List<T> aggregate(String collectionName, List<Bson> query, boolean allowDiskUse, Class<T> clazz) {

        final List list = new ArrayList();

        Block<Document> block = new Block<Document>() {

            public void apply(Document document) {
                document = iding(document);
                list.add(JSON.parseObject(JSONObject.toJSONString(document), clazz));
            }
        };

        getCollection(collectionName).aggregate(query).allowDiskUse(allowDiskUse).forEach(block);

        return list;
    }

    public List<JSONObject> find(String collectionName, Bson projection) {
        return find(collectionName, new BsonDocument(), projection, new BsonDocument(), 0, 0, "");
    }

    public List<JSONObject> find(String collectionName, int limit, Bson sort, Bson projection) {
        return find(collectionName, new BsonDocument(), projection, sort, limit, 0, "");
    }

    public List<JSONObject> find(String collectionName, int limit, int skip, Bson sort, Bson projection, String join) {
        return find(collectionName, new BsonDocument(), projection, sort, limit, 0, join);
    }

    public <T> List<T> find(String collectionName, int limit, Bson sort, Bson projection, Class<T> clazz) {
        return find(collectionName, new BsonDocument(), projection, sort, limit, 0, "", clazz);
    }

    public <T> List<T> find(String collectionName, int limit, int skip, Bson sort, Bson projection, String join, Class<T> clazz) {
        return find(collectionName, new BsonDocument(), projection, sort, limit, skip, join, clazz);
    }

    public List<JSONObject> find(String collectionName, Bson query, Bson projection) {
        return find(collectionName, query, projection, new BsonDocument(), 0, 0, "");
    }


    public long count(String collectionName, Bson query) {
        return getCollection(collectionName).count(query);
    }

    public long count(String collectionName) {
        return getCollection(collectionName).count();
    }


    public JSONObject findOne(String collectionName, Bson query, String join) {
        return (JSONObject) JSON.toJSON(
                iding(jointing(getCollection(collectionName).find(query).first(), join))
        );
    }

    public <T> T findOne(String collectionName, Bson query, String join, Class<T> clazz) {
        return JSON.parseObject(JSON.toJSONString(
                iding(jointing(getCollection(collectionName).find(query).first(), join)))
                , clazz);
    }

    public List<JSONObject> find(String collectionName, Bson query, Bson projection, Bson sort, int limit,
                                        int skip, String join) {

        final List<JSONObject> list = new ArrayList<JSONObject>();

        Block<Document> block = new Block<Document>() {

            public void apply(Document document) {
                document = iding(document);
                document = jointing(document, join);
                list.add((JSONObject) JSON.toJSON(document));
            }
        };
        getCollection(collectionName).find(query).projection(projection).sort(sort).limit(limit).skip(skip).forEach(block);

        return list;

    }

    public <T> List<T> find(String collectionName, Bson query, Bson projection, Bson sort, int limit, int skip,
                                   String join, Class<T> clazz) {

        final List list = new ArrayList();

        Block<Document> block = new Block<Document>() {

            public void apply(Document document) {
                document = iding(document);
                document = jointing(document, join);
                list.add(JSON.parseObject(JSONObject.toJSONString(document), clazz));
            }
        };

        getCollection(collectionName).find(query).projection(projection).sort(sort).limit(limit).skip(skip).forEach(block);

        return list;

    }


    public long update(String collectionName, Bson queue, Bson data) {
        UpdateResult updateResult = getCollection(collectionName).updateMany(queue, data);
        return updateResult.getModifiedCount();
    }

    public long updateOne(String collectionName, Bson queue, Bson data) {
        UpdateResult updateResult = getCollection(collectionName).updateOne(queue, data);
        return updateResult.getModifiedCount();
    }


    public long delete(String collectionName, Bson queue) {
        DeleteResult deleteResult = getCollection(collectionName).deleteMany(queue);
        return deleteResult.getDeletedCount();
    }

    public long deleteOne(String collectionName, Bson queue) {
        DeleteResult deleteResult = getCollection(collectionName).deleteOne(queue);
        return deleteResult.getDeletedCount();
    }

    public String validation(Object obj) {

        StringBuffer buffer = new StringBuffer(64);//用于存储验证后的错误信息

        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();

        Set<ConstraintViolation<Object>> constraintViolations = validator
                .validate(obj);//验证某个对象,其实也可以只验证其中的某一个属性的

        constraintViolations.forEach((ConstraintViolation c) -> buffer.append(c.getMessage()));

        return buffer.toString();
    }

    //校验单个属性
    public String validation(Object obj, String[] keys) {

        StringBuffer buffer = new StringBuffer(64);//用于存储验证后的错误信息

        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();

        Set<ConstraintViolation<Object>> constraintViolations = new HashSet<>();

        for (String key : keys) {
            Iterator<ConstraintViolation<Object>> it = validator.validateProperty(obj, key).iterator();
            if (it.hasNext()) {
                constraintViolations.add(it.next());
            }

        }


        constraintViolations.forEach((ConstraintViolation c) -> buffer.append(c.getMessage()));

        return buffer.toString();
    }

    public String setIndex(String collectionName, Bson bson) {
        return getCollection(collectionName).createIndex(bson);
    }

    public List<String> setIndex(String collectionName, List<IndexModel> list) {
        return getCollection(collectionName).createIndexes(list);
    }

    public List<JSONObject> getIndex(String collectionName) {

        List list = new ArrayList();

        Block<Document> block = new Block<Document>() {

            public void apply(final Document document) {
                list.add(JSON.parseObject(document.toJson()));
            }
        };

        getCollection(collectionName).listIndexes().forEach(block);

        return list;
    }

    public void deleteIndex(String collectionName, Bson bson) {

        getCollection(collectionName).dropIndex(bson);

    }

    public void deleteIndex(String collectionName) {
        getCollection(collectionName).dropIndexes();
    }

    private Document iding(Document document) {
        Assertions.notNull("document", document);
        try {
            if (document.get("_id") != null && !document.get("_id").toString().isEmpty()) {
                document.put("id", document.get("_id").toString());
                document.remove("_id");
            }
        } catch (ClassCastException e) {
                /*如果转换出错直接返回原本的值,不做任何处理*/
        }
        return document;
    }

    private Document jointing(Document document, String join) {
        if (join != null && !join.isEmpty()) {
            try {
                DBRef dbRef = document.get(join, DBRef.class);
                Document joinDoc = getCollection(dbRef.getCollectionName())
                        .find(new Document("_id", dbRef.getId())).first();
                joinDoc = iding(joinDoc);
                document.put(join, joinDoc);
            } catch (ClassCastException e) {
                /*用于避免如果key对应的值并不是DBRef,如果转换出错直接返回原本的值,不做任何处理*/
            }
        }
        return document;

    }


}



