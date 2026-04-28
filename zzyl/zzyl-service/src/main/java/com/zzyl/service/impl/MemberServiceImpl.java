package com.zzyl.service.impl;

import com.google.common.collect.Lists;
import com.zzyl.constant.Constants;
import com.zzyl.dto.UserLoginRequestDto;
import com.zzyl.entity.Member;
import com.zzyl.mapper.MemberMapper;
import com.zzyl.properties.JwtTokenManagerProperties;
import com.zzyl.service.MemberService;
import com.zzyl.service.WechatService;
import com.zzyl.utils.JwtUtil;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.utils.StringUtils;
import com.zzyl.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员服务实现类
 * 处理小程序用户的登录、信息同步等业务逻辑
 */
@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    @Autowired
    private WechatService wechatService;

    @Autowired
    private MemberMapper memberMapper;

    /**
     * 默认昵称前缀列表，用于新用户注册时随机生成昵称
     */
    static List<String> DEFAULT_NICKNAME_PREFIX = Lists.newArrayList(
            "生活更美好",
            "大桔大利",
            "日富一日",
            "好柿开花",
            "柿柿如意",
            "一椰暴富",
            "大柚所为",
            "杨梅吐气",
            "天生荔枝"
    );

    @Autowired
    private JwtTokenManagerProperties jwtTokenManagerProperties;

    /**
     * 小程序登录逻辑
     * 流程：
     * 1. 换取微信 OpenID
     * 2. 检查本地数据库是否存在该用户
     * 3. 换取微信绑定的手机号
     * 4. 更新或新增用户信息
     * 5. 生成并返回 JWT Token
     *
     * @param userLoginRequestDto 包含登录所需的 code 和 phoneCode
     * @return 包含 Token 和 昵称的登录结果
     */
    @Override
    public LoginVo login(UserLoginRequestDto userLoginRequestDto) {

        // 1. 通过前端传递的 code 发起远程调用，获取用户的唯一标识 openid
        // code 只能使用一次，用于向微信服务器换取 openid
        String openid = wechatService.getOpenid(userLoginRequestDto.getCode());

        // 2. 通过 openid 查询数据库，判断用户是否曾经登录过
        Member member = memberMapper.getByOpenid(openid);

        // 3. 如果用户为空（第一次登录），则构建新的用户对象
        if(ObjectUtil.isEmpty(member)){
            // 使用 Builder 模式构建 Member 对象并设置 openid
            member = Member
                    .builder()
                    .openId(openid)
                    .build();
        }

        // 4. 根据前端传来的 phoneCode 调用微信接口查询用户绑定的手机号
        // 这一步通常需要用户在小程序端授权
        String phone = wechatService.getPhone(userLoginRequestDto.getPhoneCode());

        // 5. 新增或修改用户信息（同步手机号、生成昵称等）
        saveOrUpdate(member, phone);

        // 6. 登录成功后，生成 JWT Token 返回给前端
        // Token 中包含用户的 ID 和 名称，方便后续请求校验身份
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.JWT_USERID, member.getId());
        claims.put(Constants.JWT_USERNAME, member.getName());
        
        // 创建 JWT 字符串
        String token = JwtUtil.createJWT(
                jwtTokenManagerProperties.getBase64EncodedSecretKey(), 
                jwtTokenManagerProperties.getTtl(), 
                claims
        );

        // 7. 组装返回对象
        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        loginVo.setNickName(member.getName());
        return loginVo;
    }

    /**
     * 保存或修改会员信息
     * 逻辑：
     * - 如果手机号发生变化，则更新手机号
     * - 如果是新用户（ID 为空），则生成随机昵称并插入数据库
     * - 如果是老用户，则更新其信息
     *
     * @param member 会员对象
     * @param phone  最新的手机号
     */
    private void saveOrUpdate(Member member, String phone) {

        // 判断手机号是否更换或是否是新获取的手机号
        if(ObjectUtil.notEqual(member.getPhone(), phone)){
            member.setPhone(phone);
        }

        // 如果 ID 不为空，说明数据库已存在该用户，执行更新操作
        if(ObjectUtil.isNotEmpty(member.getId())){
            memberMapper.updateMember(member);
            return;
        }

        // 如果 ID 为空，说明是新用户，执行新增操作
        // 自动生成随机昵称：随机前缀 + 手机号后4位
        String nickName = DEFAULT_NICKNAME_PREFIX.get((int)(Math.random() * DEFAULT_NICKNAME_PREFIX.size())) 
                + StringUtils.substring(phone, 7);
        member.setName(nickName);
        
        // 将新用户插入数据库
        memberMapper.insertMember(member);
    }
}
