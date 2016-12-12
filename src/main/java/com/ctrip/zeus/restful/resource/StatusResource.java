package com.ctrip.zeus.restful.resource;

import com.ctrip.zeus.auth.Authorize;
import com.ctrip.zeus.dal.core.GlobalJobDao;
import com.ctrip.zeus.dal.core.GlobalJobDo;
import com.ctrip.zeus.exceptions.ValidationException;
import com.ctrip.zeus.model.entity.SlbGroupCheckFailureEntity;
import com.ctrip.zeus.model.entity.SlbGroupCheckFailureEntityList;
import com.ctrip.zeus.restful.message.QueryParamRender;
import com.ctrip.zeus.restful.message.ResponseHandler;
import com.ctrip.zeus.restful.message.TrimmedQueryParam;
import com.ctrip.zeus.service.model.IdVersion;
import com.ctrip.zeus.service.model.SelectionMode;
import com.ctrip.zeus.service.query.CriteriaQueryFactory;
import com.ctrip.zeus.service.query.GroupCriteriaQuery;
import com.ctrip.zeus.service.query.QueryEngine;
import com.ctrip.zeus.service.status.GroupStatusService;
import com.ctrip.zeus.status.entity.GroupStatus;
import com.ctrip.zeus.status.entity.GroupStatusList;
import com.ctrip.zeus.task.check.SlbCheckStatusRollingMachine;
import com.ctrip.zeus.util.CircularArray;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author:xingchaowang
 * @date: 3/4/2015.
 */
@Component
@Path("/status")
public class StatusResource {

    @Resource
    private GroupStatusService groupStatusService;
    @Resource
    private GroupCriteriaQuery groupCriteriaQuery;
    @Resource
    private ResponseHandler responseHandler;
    @Resource
    private GlobalJobDao globalJobDao;
    @Resource
    private CriteriaQueryFactory criteriaQueryFactory;
    @Resource
    private SlbCheckStatusRollingMachine slbCheckStatusRollingMachine;

    @GET
    @Path("/groups")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Authorize(name = "getGroupStatus", uriGroupHint = -1)
    public Response allGroupStatusInSlb(@Context HttpServletRequest request, @Context HttpHeaders hh,
                                        @TrimmedQueryParam("mode") final String mode,
                                        @Context UriInfo uriInfo) throws Exception {
        QueryEngine queryRender = new QueryEngine(QueryParamRender.extractRawQueryParam(uriInfo), "group", SelectionMode.getMode(mode));
        queryRender.init(true);
        IdVersion[] searchKeys = queryRender.run(criteriaQueryFactory);
        Set<Long> groupIds = new HashSet<>();
        for (IdVersion idv : searchKeys) {
            groupIds.add(idv.getId());
        }
        List<GroupStatus> statusList = groupStatusService.getOfflineGroupsStatus(groupIds);
        GroupStatusList result = new GroupStatusList();
        for (GroupStatus groupStatus : statusList) {
            result.addGroupStatus(groupStatus);
        }
        return responseHandler.handle(result, hh.getMediaType());
    }

    @GET
    @Path("/group")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Authorize(name = "getGroupStatus", uriGroupHint = -1)
    public Response groupStatus(@Context HttpServletRequest request, @Context HttpHeaders hh,
                                @QueryParam("groupId") Long groupId,
                                @QueryParam("groupName") String groupName) throws Exception {
        if (groupId == null) {
            if (groupName != null) {
                groupId = groupCriteriaQuery.queryByName(groupName);
            }
        }
        if (groupId == null) {
            throw new ValidationException("Cannot find group by groupName " + groupName + ".");
        }
        IdVersion[] check = groupCriteriaQuery.queryByIdAndMode(groupId, SelectionMode.REDUNDANT);
        if (check.length == 0) {
            throw new ValidationException("Cannot find group by groupId " + groupId + ".");
        }

        GroupStatus status = groupStatusService.getOfflineGroupStatus(groupId);
        if (status == null) {
            throw new ValidationException("Not Found Group Status In Slb!");
        }
        return responseHandler.handle(status, hh.getMediaType());
    }

    @GET
    @Path("/check/slb")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSingleSlbCheckFailures(@Context HttpServletRequest request, @Context HttpHeaders hh,
                                              @QueryParam("slbId") Long slbId) throws Exception {
        List<Integer> count = slbCheckStatusRollingMachine.getCheckFailureCount(slbId);
        if (count == null) {
            throw new ValidationException("Cannot find check result count of slb " + slbId + ".");
        }
        SlbGroupCheckFailureEntity entity = new SlbGroupCheckFailureEntity().setSlbId(slbId);
        for (Integer c : count) {
            entity.addFailureCount(c);
        }
        return responseHandler.handle(entity, hh.getMediaType());
    }

    @GET
    @Path("/check/slbs")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSlbCheckFailures(@Context HttpServletRequest request, @Context HttpHeaders hh) throws Exception {
        SlbGroupCheckFailureEntityList list = new SlbGroupCheckFailureEntityList();
        for (Map.Entry<Long, List<Integer>> e : slbCheckStatusRollingMachine.getCheckFailureCount().entrySet()) {
            SlbGroupCheckFailureEntity entity = new SlbGroupCheckFailureEntity().setSlbId(e.getKey());
            for (Integer c : e.getValue()) {
                entity.addFailureCount(c);
            }
            list.addSlbGroupCheckFailureEntity(entity);
        }
        list.setTotal(list.getSlbGroupCheckFailureEntities().size());
        return responseHandler.handle(list, hh.getMediaType());
    }

    @GET
    @Path("/check/refresh")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSlbCheckFailures(@Context HttpServletRequest request, @Context HttpHeaders hh,
                                        @QueryParam("slbId") Long slbId) throws Exception {
        if (slbId == null) {
            throw new ValidationException("Query param slbId is required.");
        }
        List<GroupStatus> groupStatuses = groupStatusService.getOfflineGroupsStatusBySlbId(slbId);
        slbCheckStatusRollingMachine.refresh(slbId, groupStatuses);
        return responseHandler.handle("Successfully refreshed slb check count data.", hh.getMediaType());
    }

    @GET
    @Path("/job/unlock")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response jobUnlock(@Context HttpServletRequest request, @Context HttpHeaders hh,
                              @QueryParam("key") String key) throws Exception {
        globalJobDao.deleteByPK(new GlobalJobDo().setJobKey(key));
        return responseHandler.handle("success.", hh.getMediaType());
    }
}
