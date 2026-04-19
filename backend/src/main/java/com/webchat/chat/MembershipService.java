package com.webchat.chat;

import com.webchat.common.BadRequestException;
import com.webchat.common.ConflictException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final ChatBanRepository bans;
    private final ChatInvitationRepository invitations;
    private final ChatPolicy policy;
    private final ApplicationEventPublisher events;

    public MembershipService(ChatRepository chats, ChatMemberRepository members,
                             ChatBanRepository bans, ChatInvitationRepository invitations,
                             ChatPolicy policy, ApplicationEventPublisher events) {
        this.chats = chats;
        this.members = members;
        this.bans = bans;
        this.invitations = invitations;
        this.policy = policy;
        this.events = events;
    }

    @Transactional
    public ChatMember joinPublic(Long userId, Long chatId) {
        Chat c = chats.findById(chatId).orElseThrow(() -> new NotFoundException("Room not found"));
        if (c.getType() != ChatType.PUBLIC_ROOM) throw new BadRequestException("Room is not public");
        if (bans.existsByIdChatIdAndIdUserId(chatId, userId)) throw new UnauthorizedException("You are banned from this room");
        if (members.existsByIdChatIdAndIdUserId(chatId, userId)) throw new ConflictException("Already a member");
        ChatMember saved = members.save(new ChatMember(chatId, userId, ChatRole.MEMBER));
        events.publishEvent(new ChatMembershipEvent(chatId, userId, ChatMembershipEvent.Kind.JOINED));
        return saved;
    }

    @Transactional
    public void leave(Long userId, Long chatId) {
        ChatMember m = policy.requireMembership(chatId, userId);
        if (m.getRole() == ChatRole.OWNER) {
            throw new BadRequestException("Owner cannot leave; delete the room instead");
        }
        members.delete(m);
        events.publishEvent(new ChatMembershipEvent(chatId, userId, ChatMembershipEvent.Kind.LEFT));
    }

    public List<ChatMember> list(Long userId, Long chatId) {
        policy.requireMembership(chatId, userId);
        return members.findByIdChatId(chatId);
    }

    public Page<ChatMember> listPage(Long userId, Long chatId, Pageable pageable) {
        policy.requireMembership(chatId, userId);
        return members.findByChatIdOrdered(chatId, pageable);
    }

    public List<ChatMember> listStaff(Long userId, Long chatId) {
        policy.requireMembership(chatId, userId);
        return members.findStaffByChatId(chatId);
    }

    public long memberCount(Long chatId) {
        return members.countByIdChatId(chatId);
    }

    public Map<Long, Long> memberCounts(Collection<Long> chatIds) {
        if (chatIds.isEmpty()) return Map.of();
        Map<Long, Long> out = new HashMap<>();
        for (var row : members.countByChatIds(chatIds)) {
            out.put(row.getChatId(), row.getCount());
        }
        return out;
    }

    @Transactional
    public void kickAndBan(Long adminId, Long chatId, Long targetUserId) {
        policy.requireAdmin(chatId, adminId);
        if (adminId.equals(targetUserId)) throw new BadRequestException("You cannot ban yourself; use leave instead");
        ChatMember target = members.findByIdChatIdAndIdUserId(chatId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (target.getRole() == ChatRole.OWNER) throw new BadRequestException("Cannot remove the owner");
        members.delete(target);
        if (!bans.existsByIdChatIdAndIdUserId(chatId, targetUserId)) {
            bans.save(new ChatBan(chatId, targetUserId, adminId));
        }
        invitations.findByChatIdAndInviteeIdAndAcceptedAtIsNullAndDeclinedAtIsNull(chatId, targetUserId)
                .ifPresent(inv -> { inv.decline(); invitations.save(inv); });
        events.publishEvent(new ChatMembershipEvent(chatId, targetUserId, ChatMembershipEvent.Kind.BANNED));
    }

    @Transactional
    public void unban(Long adminId, Long chatId, Long targetUserId) {
        policy.requireAdmin(chatId, adminId);
        bans.findByIdChatIdAndIdUserId(chatId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ban not found"));
        bans.deleteByIdChatIdAndIdUserId(chatId, targetUserId);
    }

    public List<ChatBan> listBans(Long adminId, Long chatId) {
        policy.requireAdmin(chatId, adminId);
        return bans.findByIdChatId(chatId);
    }

    @Transactional
    public void promoteToAdmin(Long ownerId, Long chatId, Long targetUserId) {
        policy.requireOwnership(chatId, ownerId);
        ChatMember target = members.findByIdChatIdAndIdUserId(chatId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (target.getRole() == ChatRole.OWNER) throw new BadRequestException("Owner is already admin");
        target.setRole(ChatRole.ADMIN);
    }

    @Transactional
    public void demoteAdmin(Long ownerId, Long chatId, Long targetUserId) {
        policy.requireOwnership(chatId, ownerId);
        ChatMember target = members.findByIdChatIdAndIdUserId(chatId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (target.getRole() == ChatRole.OWNER) throw new BadRequestException("Owner cannot be demoted");
        if (target.getRole() != ChatRole.ADMIN) throw new BadRequestException("User is not an admin");
        target.setRole(ChatRole.MEMBER);
    }
}
