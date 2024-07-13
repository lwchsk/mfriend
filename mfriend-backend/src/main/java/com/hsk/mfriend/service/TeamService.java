package com.hsk.mfriend.service;

import com.hsk.mfriend.entity.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hsk.mfriend.entity.domain.User;
import com.hsk.mfriend.entity.dto.TeamQuery;
import com.hsk.mfriend.entity.request.JoinTeamRequest;
import com.hsk.mfriend.entity.request.TeamQuitRequest;
import com.hsk.mfriend.entity.request.TeamUpdateRequest;
import com.hsk.mfriend.entity.vo.TeamUserVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author dachui_boom
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2024-06-23 13:41:04
 */
public interface TeamService extends IService<Team> {

    /**
     * 加入队伍
     * @param joinTeamRequest
     * @param loginUser
     * @return
     */
    Boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser);

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    Long insertTeam(Team team, User loginUser);

    /**
     * 队伍展示
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 队伍修改
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    Boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    @Transactional(rollbackFor = Exception.class)
    boolean deleteTeam(long id, User loginUser);
}
