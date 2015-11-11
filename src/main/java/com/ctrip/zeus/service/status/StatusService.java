package com.ctrip.zeus.service.status;


import com.ctrip.zeus.service.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author:xingchaowang
 * @date: 3/16/2015.
 */
public interface StatusService extends Repository {

    /**
     * get all down app servers
     * @return app server ip list
     * @throws Exception
     */
    Set<String> findAllDownServers() throws Exception;

    /**
     * get all up app servers by slbId
     * @param slbId the slb id
     * @return app server ip list
     * @throws Exception
     */
    Set<String> findAllUpGroupServersBySlbId(Long slbId) throws Exception;
    /**
     * get group servers by slbId and status offset
     * @param slbId the slb id
     * @return app server ip list
     * @throws Exception
     */
    Set<String> findAllGroupServersBySlbIdAndStatusOffset(Long slbId , int offset , boolean status) throws Exception;

    /**
     * up server by app server ip
     * @param ip the app server ip
     * @return
     * @throws Exception
     */
    void upServer(String ip) throws Exception;

    /**
     * down server by app server ip
     * @param ip the app server ip
     * @return
     * @throws Exception
     */
    void downServer(String ip) throws Exception;

    /**
     * up member by app server ip and appname
     * @param ips the app server ips
     * @param groupId  app name
     * @return
     * @throws Exception
     */
    void upMember(Long slbId ,Long groupId, List<String> ips)throws Exception;
    /**
     * down member by app server ip and appname
     * @param ips the app server ips
     * @param groupId  app name
     * @return
     * @throws Exception
     */
    void downMember(Long slbId ,Long groupId, List<String> ips)throws Exception;
    /**
     * update status by group server ip and slbId and group id
     * @param ips the group server ips
     * @param groupId  group id
     * @param offset  offset [0-30]
     * @param status  status enable = true , disable = false
     * @return
     * @throws Exception
     */
    void updateStatus(Long slbId ,Long groupId, List<String> ips , int offset , boolean status)throws Exception;

    /**
     * get App server status by app name and slbname and virtual server ip
     * @param slbId the slb name
     * @param groupId  app name
     * @param vsip  virtual server ip
     * @return true : status=up false : status = down
     * @throws Exception
     */
    boolean getGroupServerStatus(Long slbId,Long groupId, String vsip)throws Exception;

    /**
     * get server status by virtual server ip
     * @param vsip  virtual server ip
     * @return true : status=up false : status = down
     * @throws Exception
     */
    boolean getServerStatus(String vsip )throws  Exception;
}
