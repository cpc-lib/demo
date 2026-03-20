package cc.ivera.mp.generator.user.service.impl;

import cc.ivera.mp.generator.user.entity.TbUser;
import cc.ivera.mp.generator.user.mapper.TbUserMapper;
import cc.ivera.mp.generator.user.service.ITbUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author itcast
 * @since 2019-05-09
 */
@Service
public class TbUserServiceImpl extends ServiceImpl<TbUserMapper, TbUser> implements ITbUserService {

}
