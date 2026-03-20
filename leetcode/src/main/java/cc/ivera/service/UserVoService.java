package cc.ivera.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.ivera.mapper.UserVoMapper;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.model.vo.UserVo;

/**
 * service层
 */
@Service
public class UserVoService {

    @Autowired
    private UserVoMapper userVoMapper;

    public AjaxResult get(Integer id) {
        LambdaQueryWrapper<UserVo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVo::getId, id);
        UserVo user = userVoMapper.selectOne(wrapper);
        return AjaxResult.success(user);
    }

    public AjaxResult insert(UserVo user) {
        int line = userVoMapper.insert(user);
        if (line > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.error(888, "操作数据库失败", null);
    }

    public AjaxResult delete(Integer id) {
        LambdaQueryWrapper<UserVo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVo::getId, id);
        int line = userVoMapper.delete(wrapper);
        if (line > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.error(888, "操作数据库失败", null);
    }

    public AjaxResult update(UserVo user) {
        int i = userVoMapper.updateById(user);
        if (i > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.error(888, "操作数据库失败", null);
    }

}
