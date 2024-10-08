package com.hsk.mfriend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hsk.mfriend.common.BaseResponse;
import com.hsk.mfriend.common.ErrorCode;
import com.hsk.mfriend.common.ResultUtils;
import com.hsk.mfriend.entity.domain.Team;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.entity.domain.UserTeam;
import com.hsk.mfriend.entity.dto.TeamQuery;
import com.hsk.mfriend.entity.request.DeleteRequest;
import com.hsk.mfriend.entity.request.JoinTeamRequest;
import com.hsk.mfriend.entity.request.TeamQuitRequest;
import com.hsk.mfriend.entity.request.TeamUpdateRequest;
import com.hsk.mfriend.entity.vo.TeamUserVO;
import com.hsk.mfriend.exception.BusinessException;
import com.hsk.mfriend.service.TeamService;
import com.hsk.mfriend.service.UserService;
import com.hsk.mfriend.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date:2024/06/22
 * Author:hsk
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", allowedHeaders = {"*"})
public class TeamController {
    @Resource
    TeamService teamService;

    @Resource
    UserService userService;

    @Resource
    UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody Team team, HttpServletRequest request) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        Long save = teamService.insertTeam(team, loginUser);
        if (save < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入失败");
        }
        return ResultUtils.success(team.getId());
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(result);
    }


    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改失败");
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamByid(long id) {
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询失败");
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<Team>> teamList(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Team team = new Team();
        BeanUtils.copyProperties(team, teamQuery);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        List<Team> list = teamService.list(queryWrapper);
//        if (list == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询列表失败");
//        }
        //如果查询失败，返回的list为空
        return ResultUtils.success(list);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> teamPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Team team = new Team();
        BeanUtils.copyProperties(team, teamQuery);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(JoinTeamRequest joinTeamRequest, HttpServletRequest request) {
        if (joinTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.joinTeam(joinTeamRequest, loginUser);

        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

}
///**
// * 队伍接口
// */
//@RestController
//@RequestMapping("/team")
//@CrossOrigin(origins = {"http://localhost:3000"})
//@Slf4j
//public class TeamController {
//
//    @Resource
//    private UserService userService;
//
//    @Resource
//    private TeamService teamService;
//
//    @Resource
//    private UserTeamService userTeamService;
//
//    @PostMapping("/add")
//    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
//        if (teamAddRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        Team team = new Team();
//        BeanUtils.copyProperties(teamAddRequest, team);
//        long teamId = teamService.addTeam(team, loginUser);
//        return ResultUtils.success(teamId);
//    }
//
//    @PostMapping("/update")
//    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
//        if (teamUpdateRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
//        }
//        return ResultUtils.success(true);
//    }
//
//    @GetMapping("/get")
//    public BaseResponse<Team> getTeamById(long id) {
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Team team = teamService.getById(id);
//        if (team == null) {
//            throw new BusinessException(ErrorCode.NULL_ERROR);
//        }
//        return ResultUtils.success(team);
//    }
//
//    @GetMapping("/list")
//    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean isAdmin = userService.isAdmin(request);
//        // 1、查询队伍列表
//        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
//        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
//        // 2、判断当前用户是否已加入队伍
//        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
//        try {
//            User loginUser = userService.getLoginUser(request);
//            userTeamQueryWrapper.eq("userId", loginUser.getId());
//            userTeamQueryWrapper.in("teamId", teamIdList);
//            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
//            // 已加入的队伍 id 集合
//            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
//            teamList.forEach(team -> {
//                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
//                team.setHasJoin(hasJoin);
//            });
//        } catch (Exception e) {
//        }
//        // 3、查询已加入队伍的人数
//        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
//        userTeamJoinQueryWrapper.in("teamId", teamIdList);
//        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
//        // 队伍 id => 加入这个队伍的用户列表
//        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
//        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));
//        return ResultUtils.success(teamList);
//    }
//
//    // todo 查询分页
//    @GetMapping("/list/page")
//    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Team team = new Team();
//        BeanUtils.copyProperties(teamQuery, team);
//        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
//        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
//        Page<Team> resultPage = teamService.page(page, queryWrapper);
//        return ResultUtils.success(resultPage);
//    }
//
//    @PostMapping("/join")
//    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
//        if (teamJoinRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
//        return ResultUtils.success(result);
//    }
//
//    @PostMapping("/quit")
//    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
//        if (teamQuitRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
//        return ResultUtils.success(result);
//    }
//
//    @PostMapping("/delete")
//    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
//        if (deleteRequest == null || deleteRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        long id = deleteRequest.getId();
//        User loginUser = userService.getLoginUser(request);
//        boolean result = teamService.deleteTeam(id, loginUser);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
//        }
//        return ResultUtils.success(true);
//    }
//
//
//    /**
//     * 获取我创建的队伍
//     *
//     * @param teamQuery
//     * @param request
//     * @return
//     */
//    @GetMapping("/list/my/create")
//    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        teamQuery.setUserId(loginUser.getId());
//        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
//        return ResultUtils.success(teamList);
//    }
//
//
//    /**
//     * 获取我加入的队伍
//     *
//     * @param teamQuery
//     * @param request
//     * @return
//     */
//    @GetMapping("/list/my/join")
//    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("userId", loginUser.getId());
//        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
//        // 取出不重复的队伍 id
//        // teamId userId
//        // 1, 2
//        // 1, 3
//        // 2, 3
//        // result
//        // 1 => 2, 3
//        // 2 => 3
//        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
//                .collect(Collectors.groupingBy(UserTeam::getTeamId));
//        List<Long> idList = new ArrayList<>(listMap.keySet());
//        teamQuery.setIdList(idList);
//        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
//        return ResultUtils.success(teamList);
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
