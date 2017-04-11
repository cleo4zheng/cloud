package com.cloud.cloudapi.service.common;

public interface SuperDaoService <Model, PK> {
    /**
     * 插入对象
     *
     * @param model 对象
     * @throws Exception
     */
    int insert(Model model) throws Exception;

    /**
     * 更新对象
     *
     * @param model 对象
     * @throws Exception
     */
    int update(Model model) throws Exception;

    /**
     * 通过主键, 删除对象
     *
     * @param id 主键
     * @throws Exception
     */
    int delete(PK id) throws Exception;

    /**
     * 通过主键, 查询对象
     *
     * @param id 主键
     * @return model 对象
     * @throws Exception
     */
    Model selectById(PK id) throws Exception;
}
