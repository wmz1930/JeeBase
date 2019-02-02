package com.jeebase.common.weixin.service.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;
import com.jeebase.common.base.BusinessException;
import com.jeebase.common.weixin.domain.AccessToken;
import com.jeebase.common.weixin.domain.AttentionUserList;
import com.jeebase.common.weixin.domain.CumulateUserList;
import com.jeebase.common.weixin.domain.JsapiTicket;
import com.jeebase.common.weixin.domain.MsgSendRequest;
import com.jeebase.common.weixin.domain.MsgSendResponse;
import com.jeebase.common.weixin.domain.MsgTemplateList;
import com.jeebase.common.weixin.domain.SummaryUserList;
import com.jeebase.common.weixin.domain.SummaryUserRequest;
import com.jeebase.common.weixin.domain.UserListInfoRequest;
import com.jeebase.common.weixin.domain.UserListInfoResponse;
import com.jeebase.common.weixin.service.IWeiXinApiService;
import com.jeebase.common.weixin.util.WeiXinSign;

/**
 * @author jeebase
 */
@Service("weiXinApiService")
public class WeiXinApiServiceImpl implements IWeiXinApiService {

    /**
     * 日志记录
     */
    private static final Logger logger = LoggerFactory.getLogger(WeiXinApiServiceImpl.class);

    private static final String WEIXIN_TOKEN_KEY = "weiXin";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取access_token的URL,有效期目前为2个小时
     */
    private String accessTokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={appSecret}";

    /**
     * 获取access_token的URL,有效期目前为2个小时
     */
    private String accessWebTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={appId}&secret={appSecret}&code={code}&grant_type=authorization_code";

    /**
     * 获取帐号下所有模板信息
     */
    private String queryTemplateListUrl = "https://api.weixin.qq.com/cgi-bin/template/get_all_private_template?access_token={accessToken}";

    /**
     * 发送微信模板信息
     */
    private String sendTemplateMsgUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token={accessToken}";

    /**
     * 获取用户增减数据(最大时间跨度7天)
     */
    private String queryUserSummaryUrl = "https://api.weixin.qq.com/datacube/getusersummary?access_token={accessToken}";

    /**
     * 获取用户总关注数(最大时间跨度7天)
     */
    private String queryUserCumulateUrl = "https://api.weixin.qq.com/datacube/getusercumulate?access_token={accessToken}";

    /**
     * 获取jsapi_ticket
     */
    private String queryJsApiTicketUrl = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token={accessToken}&type=jsapi";

    /**
     * 获取用户关注列表
     */
    private String queryAttentionUserList = "https://api.weixin.qq.com/cgi-bin/user/get?access_token={accessToken}&next_openid={nextOpenId}";

    /**
     * 批量获取用户信息
     */
    private String queryUserInfoList = "https://api.weixin.qq.com/cgi-bin/user/info/batchget?access_token={accessToken}";

    /**
     * 从缓存获取accessToken
     */
    @Override
    @Cacheable(value = WEIXIN_TOKEN_KEY, key = "'app_id_'.concat(#appId)", condition = "#result != null")
    public String getAccessToken(String appId, String appSecret) {
        String tokenString = null;
        AccessToken accessToken = queryAccessToken(appId, appSecret);
        if (!StringUtils.isEmpty(accessToken.getAccess_token())) {
            tokenString = accessToken.getAccess_token();
        }
        return tokenString;
    }

    /**
     * 从微信获取accessToken
     */
    @Override
    public AccessToken queryAccessToken(String appId, String appSecret) {
        AccessToken accessToken = null;
        try {
            accessToken = restTemplate.getForObject(accessTokenUrl, AccessToken.class, appId, appSecret);
        } catch (Exception e) {
            logger.error("调用获取微信AccessToken接口异常：" + e);
            throw new BusinessException("调用获取微信AccessToken接口异常。");
        }
        return accessToken;
    }

    /**
     * 从微信获取消息模板列表
     */
    @Override
    public MsgTemplateList queryTemplateList(String appId, String appSecret) {
        MsgTemplateList msgTemplateList = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            msgTemplateList = restTemplate.getForObject(queryTemplateListUrl, MsgTemplateList.class, accessToken);
        } catch (Exception e) {
            logger.error("调用从微信获取消息模板列表接口异常：" + e);
            throw new BusinessException("调用从微信获取消息模板列表接口异常。");
        }
        return msgTemplateList;
    }

    /**
     * 发送模板消息
     */
    @Override
    public MsgSendResponse sendWeiXinMsg(String appId, String appSecret, MsgSendRequest msgSend) {
        MsgSendResponse msgSendResponse = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            msgSendResponse = restTemplate.postForObject(sendTemplateMsgUrl, msgSend, MsgSendResponse.class,
                    accessToken);
        } catch (Exception e) {
            logger.error("调用发送微信模板消息接口异常：" + e);
            throw new BusinessException("调用发送微信模板消息接口异常。");
        }
        return msgSendResponse;
    }

    /**
     * 获取关注的用户增减数据
     */
    @Override
    public SummaryUserList summaryUser(String appId, String appSecret, SummaryUserRequest suRequest) {
        SummaryUserList summaryUserList = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            summaryUserList = restTemplate.postForObject(queryUserSummaryUrl, suRequest, SummaryUserList.class,
                    accessToken);
        } catch (Exception e) {
            logger.error("调用获取关注的用户增减数据接口异常：" + e);
            throw new BusinessException("调用获取关注的用户增减数据接口异常。");
        }
        return summaryUserList;
    }

    /**
     * 获取关注的用户总数
     */
    @Override
    public CumulateUserList cumulateUser(String appId, String appSecret, SummaryUserRequest suRequest) {
        CumulateUserList cumulateUserList = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            cumulateUserList = restTemplate.postForObject(queryUserCumulateUrl, suRequest, CumulateUserList.class,
                    accessToken);
        } catch (Exception e) {
            logger.error("调用获取关注的用户总数接口异常：" + e);
            throw new BusinessException("调用获取关注的用户总数接口异常。");
        }
        return cumulateUserList;
    }

    /**
     * 从微信获取关注的用户列表
     */
    @Override
    public AttentionUserList queryAttentionUserList(String appId, String appSecret, String nextOpenId) {
        AttentionUserList attentionUserList = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            attentionUserList = restTemplate.getForObject(queryAttentionUserList, AttentionUserList.class, accessToken,
                    nextOpenId);
        } catch (Exception e) {
            logger.error("调用从微信获关注用户列表接口异常：" + e);
            throw new BusinessException("调用从微信获取关注用户列表接口异常。");
        }
        return attentionUserList;
    }

    /**
     * 批量获取用户信息
     */
    @Override
    public UserListInfoResponse queryUserInfoList(String appId, String appSecret, UserListInfoRequest userListReq) {
        UserListInfoResponse userListInfoResponse = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            userListInfoResponse = restTemplate.postForObject(queryUserInfoList, userListReq,
                    UserListInfoResponse.class, accessToken);
        } catch (Exception e) {
            logger.error("调用批量获取用户信息接口异常：" + e);
            throw new BusinessException("调用批量获取用户信息接口异常。");
        }
        return userListInfoResponse;
    }

    @Override
    @Cacheable(value = WEIXIN_TOKEN_KEY, key = "'jsapi_ticket_'.concat(#appId)", condition = "#result != null")
    public String getJsapiTicket(String appId, String appSecret) {
        String ticketString = null;
        if (StringUtils.isEmpty(ticketString)) {
            JsapiTicket jsapiTicket = queryJsapiTicket(appId, appSecret);
            ticketString = jsapiTicket.getTicket();
        }
        return ticketString;
    }

    @Override
    public JsapiTicket queryJsapiTicket(String appId, String appSecret) {
        JsapiTicket jsapiTicket = null;
        try {
            String accessToken = getAccessToken(appId, appSecret);
            jsapiTicket = restTemplate.getForObject(queryJsApiTicketUrl, JsapiTicket.class, accessToken);
        } catch (Exception e) {
            logger.error("调用获取微信jsapi_ticket接口异常：" + e);
            throw new BusinessException("调用获取微信jsapi_ticket接口异常。");
        }
        return jsapiTicket;
    }

    @Override
    public Map<String, String> signTicket(String appId, String appSecret, String url) {
        String ticket = getJsapiTicket(appId, appSecret);
        Map<String, String> signMap = WeiXinSign.sign(ticket, url);
        signMap.put("appid", appId);
        return signMap;
    }

    @Override
    public String queryAppId(String appId, String appSecret, String code) {
        String openId = "";
        try {
            logger.info("获取openID时code=" + code + "appId=" + appId + "secret=" + appSecret);
            ResponseEntity<String> returnStr = restTemplate.getForEntity(accessWebTokenUrl, String.class, appId,
                    appSecret, code);
            JSONObject obj = JSONObject.parseObject(returnStr.getBody());
            String returnId = (String) obj.get("openid");
            if (!StringUtils.isEmpty(returnId)) {
                openId = returnId;
            }
        } catch (Exception e) {
            logger.error("调用获取微信queryAppId接口异常：" + e);
        }
        logger.info("获取openID时openid=" + openId);
        return openId;
    }
}
