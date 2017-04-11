package com.cloud.cloudapi.service.common.impl;
import com.cloud.cloudapi.dao.common.SuperMapper;
import com.cloud.cloudapi.service.common.SuperDaoService;

public abstract class SuperDaoServiceImpl<Model, PK> implements SuperDaoService<Model, PK> {

	/**
	 * 定义成抽象方法,由子类实现,完成dao的注入
	 *
	 * @return SuperMapper实现类
	 * @throws Exception
	 */
	public abstract SuperMapper<Model, PK> getMapper();

	/**
	 * 插入对象
	 *
	 * @param model
	 *            对象
	 * @throws Exception
	 */
	@Override
	public int insert(Model model) throws Exception {
		return getMapper().insertSelective(model);
	}

	/**
	 * 更新对象
	 *
	 * @param model
	 *            对象
	 * @throws Exception
	 */
	@Override
	public int update(Model model) throws Exception {
		return getMapper().updateByPrimaryKeySelective(model);
	}

	/**
	 * 通过主键, 删除对象
	 *
	 * @param id
	 *            主键
	 * @throws Exception
	 */
	@Override
	public int delete(PK id) throws Exception {
		return getMapper().deleteByPrimaryKey(id);
	}

	/**
	 * 通过主键, 查询对象
	 *
	 * @param id
	 *            主键
	 * @return
	 * @throws Exception
	 */
	@Override
	public Model selectById(PK id) throws Exception {
		return getMapper().selectByPrimaryKey(id);
	}

}
