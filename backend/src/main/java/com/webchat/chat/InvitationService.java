package com.webchat.chat;

import com.webchat.auth.User;
import com.webchat.auth.UserRepository;
import com.webchat.common.BadRequestException;
import com.webchat.common.ConflictException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private final ChatRepository chats;
    private final ChatMemberRepository members;
    private final ChatBanRepository bans;
    private final ChatInvitationRepository invitations;
    private final UserRepository users;
    private final ChatPolicy policy;

    public InvitationService(ChatRepository chats, ChatMemberRepository members, ChatBanRepository bans,
                             ChatInvitationRepository invitations, UserRepository users, ChatPolicy policy) {
        this.chats = chats;
        this.members = members;
        this.bans = bans;
        this.invitations = invitations;
        this.users = users;
        this.policy = policy;
    }

    @Transactional
    public ChatInvitation invite(Long inviterId, Long chatId, String inviteeUsername) {
        policy.requireMembership(chatId, inviterId);
        Chat c = chats.findById(chatId).orElseThrow(() -> new NotFoundException("Room not found"));
        if (c.getType() != ChatType.PRIVATE_ROOM) {
            throw new BadRequestException("Invitations are only for private rooms");
        }
        User invitee = users.findByUsername(inviteeUsername)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (invitee.getId().equals(inviterId)) throw new BadRequestException("Cannot invite yourself");
        if (members.existsByIdChatIdAndIdUserId(chatId, invitee.getId())) {
            throw new ConflictException("User is already a member");
        }
        if (bans.existsByIdChatIdAndIdUserId(chatId, invitee.getId())) {
            throw new ConflictException("User is banned from this room");
        }
        if (invitations.findByChatIdAndInviteeIdAndAcceptedAtIsNullAndDeclinedAtIsNull(chatId, invitee.getId()).isPresent()) {
            throw new ConflictException("An invitation is already pending");
        }
        return invitations.save(new ChatInvitation(chatId, invitee.getId(), inviterId));
    }

    public List<ChatInvitation> listForUser(Long userId) {
        return invitations.findByInviteeIdAndAcceptedAtIsNullAndDeclinedAtIsNullOrderByCreatedAtDesc(userId);
    }

    public List<ChatInvitation> listForChat(Long adminId, Long chatId) {
        policy.requireAdmin(chatId, adminId);
        return invitations.findByChatIdAndAcceptedAtIsNullAndDeclinedAtIsNullOrderByCreatedAtDesc(chatId);
    }

    @Transactional
    public void accept(Long userId, Long invitationId) {
        ChatInvitation inv = invitations.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (!inv.getInviteeId().equals(userId)) throw new UnauthorizedException("Not your invitation");
        if (!inv.isPending()) throw new BadRequestException("Invitation no longer pending");
        if (bans.existsByIdChatIdAndIdUserId(inv.getChatId(), userId)) {
            throw new UnauthorizedException("You are banned from this room");
        }
        if (!members.existsByIdChatIdAndIdUserId(inv.getChatId(), userId)) {
            members.save(new ChatMember(inv.getChatId(), userId, ChatRole.MEMBER));
        }
        inv.accept();
    }

    @Transactional
    public void decline(Long userId, Long invitationId) {
        ChatInvitation inv = invitations.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (!inv.getInviteeId().equals(userId)) throw new UnauthorizedException("Not your invitation");
        if (!inv.isPending()) throw new BadRequestException("Invitation no longer pending");
        inv.decline();
    }
}
