package com.hsk.mfriend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsk.mfriend.common.ErrorCode;
import com.hsk.mfriend.entity.domain.Team;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.entity.domain.UserTeam;
import com.hsk.mfriend.entity.dto.TeamQuery;
import com.hsk.mfriend.entity.enums.TeamStatusEnum;
import com.hsk.mfriend.entity.request.JoinTeamRequest;
import com.hsk.mfriend.entity.request.TeamQuitRequest;
import com.hsk.mfriend.entity.request.TeamUpdateRequest;
import com.hsk.mfriend.entity.vo.TeamUserVO;
import com.hsk.mfriend.entity.vo.UserVO;
import com.hsk.mfriend.exception.BusinessException;
import com.hsk.mfriend.mapper.TeamMapper;
import com.hsk.mfriend.service.TeamService;
import com.hsk.mfriend.service.UserService;
import com.hsk.mfriend.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author dachui_boom
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-06-23 13:41:04
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {


    @Resource
    UserTeamService userTeamService;

    @Resource
    UserService userService;


    @Override
    public Boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser) {
        if (joinTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }

        Long userId = loginUser.getId();
        Long teamId = joinTeamRequest.getId();

        if (teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 至多加入五个队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasJoinedNum = userTeamService.count(queryWrapper);
        if (hasJoinedNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "至多加入五个队伍");
        }


        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 校验队伍是否超时
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 禁止加入私有队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }

        // 检查密码
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            String password = joinTeamRequest.getPassword();
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码错误");
            }
        }

        // 不能重复加入已加入的队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId).eq("teamId", teamId);
        long hasJoinedTeam = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinedTeam > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入队伍");
        }

        // 队伍数不能超过人数限制
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已达到人数限制");
        }

        // 添加用户队伍表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertTeam(Team team, User loginUser) {
        /**
         * 1. 创建队伍
         * - 用户可以创建一个队伍，设置队伍的人数、队伍名称（标题）、描述、超时时间。
         * - 队长、剩余人数是否可见可通过聊天进行设置。
         * - 公开、私人或加密的队伍在信息流中不展示已过期的队伍。
         */
// 1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }

// 2. 是否登录，未登录用户不允许创建队伍。
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }

// 3. 校验信息：
//    i. 队伍人数 > 1 且 <= 20。
        Integer maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不正确");
        }
//    ii. 队伍标题长度 <= 20。
        if (StringUtils.isBlank(team.getName()) || team.getName().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不正确");
        }
//    iii. 描述长度 <= 512。
        if (StringUtils.isBlank(team.getDescription()) || team.getDescription().length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述不正确");
        }
//    iv. status 是否公开（int），不传默认为 0（公开）。
//    v. 如果 status 是加密状态，一定要有密码，且密码长度 <= 32。
        Integer status = team.getStatus();
        String password = team.getPassword();
        if (status == TeamStatusEnum.SECRET.getValue()) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不正确");
            }
        }
//    vi. 超时时间必须大于当前时间。
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间设置不正确");
        }
//    vii. 校验用户最多只能创建 5 个队伍。
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍数量已达上限");
        }

// 4. 插入队伍信息到队伍表。
        //设置为空，让数据库自己生成id
        final long userId = loginUser.getId();
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

// 5. 插入用户与队伍关系到关系表。
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

// 返回创建成功的队伍信息
        return teamId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            //查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            //根据创建人来查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            //根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.SECRET)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        //不展示已过期的队伍
        // expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.isNull("expireTime").or().gt("expireTime", new Date()));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Long id = teamUpdateRequest.getId();
        //note 先判断null避免空指针异常
        if (id == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL, "队伍不存在");
        }

        //只有管理员和队伍的创建者可以修改
        if (!loginUser.getId().equals(teamUpdateRequest.getId()) && userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //如果队伍状态改为加密，密码要存在
        if (TeamStatusEnum.SECRET.equals(TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus()))) {
            if (teamUpdateRequest.getPassword() == null) {
                throw new BusinessException(ErrorCode.PARAMS_NULL, "未设置密码");
            }
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        return this.updateById(team);
    }

    @Override
    public Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL);
        }
        Long teamId = teamQuitRequest.getTeamId();
        Team team = this.getById(teamId);
        Long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) {
            this.removeById(teamId);
        } else {
            // 队伍还剩至少两人
            // 是队长
            if (team.getUserId().equals(userId)) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新当前队伍的队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        return userTeamService.remove(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        // 校验你是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_NULL, "队伍不存在");
        }
        return team;
    }


    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

}








