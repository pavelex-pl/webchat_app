package com.webchat.friends;

import com.webchat.auth.User;
import com.webchat.auth.UserRepository;
import com.webchat.common.BadRequestException;
import com.webchat.common.ConflictException;
import com.webchat.common.NotFoundException;
import com.webchat.common.UnauthorizedException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private final FriendshipRepository friendships;
    private final UserBlockRepository blocks;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public FriendService(FriendshipRepository friendships, UserBlockRepository blocks, UserRepository users,
                         ApplicationEventPublisher events) {
        this.friendships = friendships;
        this.blocks = blocks;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public Friendship sendRequest(Long fromUserId, String toUsername, String text) {
        User to = users.findByUsername(toUsername)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (to.getId().equals(fromUserId)) throw new BadRequestException("Cannot friend yourself");
        if (blocks.eitherBlocks(fromUserId, to.getId())) {
            throw new UnauthorizedException("Blocked");
        }
        Optional<Friendship> existing = friendships.findBetween(fromUserId, to.getId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) throw new ConflictException("Already friends");
            if (f.getInitiatedBy().equals(fromUserId)) throw new ConflictException("Request already pending");
            // other side requested earlier; accept here
            f.accept();
            events.publishEvent(new FriendRequestEvent(f.getInitiatedBy(), fromUserId, FriendRequestEvent.Kind.ACCEPTED));
            return f;
        }
        Friendship f = new Friendship(fromUserId, to.getId(), text);
        Friendship saved = friendships.save(f);
        events.publishEvent(new FriendRequestEvent(to.getId(), fromUserId, FriendRequestEvent.Kind.CREATED));
        return saved;
    }

    @Transactional
    public Friendship accept(Long userId, Long otherUserId) {
        Friendship f = friendships.findBetween(userId, otherUserId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (f.getStatus() == FriendshipStatus.ACCEPTED) return f;
        if (f.getInitiatedBy().equals(userId)) throw new BadRequestException("Cannot accept your own request");
        f.accept();
        events.publishEvent(new FriendRequestEvent(f.getInitiatedBy(), userId, FriendRequestEvent.Kind.ACCEPTED));
        return f;
    }

    @Transactional
    public void decline(Long userId, Long otherUserId) {
        Friendship f = friendships.findBetween(userId, otherUserId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (f.getStatus() != FriendshipStatus.PENDING) throw new BadRequestException("Not a pending request");
        if (f.getInitiatedBy().equals(userId)) throw new BadRequestException("Use cancel for outgoing");
        friendships.delete(f);
        events.publishEvent(new FriendRequestEvent(f.getInitiatedBy(), userId, FriendRequestEvent.Kind.DECLINED));
    }

    @Transactional
    public void cancelOutgoing(Long userId, Long otherUserId) {
        Friendship f = friendships.findBetween(userId, otherUserId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        if (f.getStatus() != FriendshipStatus.PENDING) throw new BadRequestException("Not a pending request");
        if (!f.getInitiatedBy().equals(userId)) throw new BadRequestException("Not your request");
        friendships.delete(f);
        events.publishEvent(new FriendRequestEvent(otherUserId, userId, FriendRequestEvent.Kind.CANCELED));
    }

    @Transactional
    public void remove(Long userId, Long otherUserId) {
        friendships.findBetween(userId, otherUserId).ifPresent(friendships::delete);
    }

    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) throw new BadRequestException("Cannot block yourself");
        if (!users.existsById(blockedId)) throw new NotFoundException("User not found");
        // spec §2.3.5: user-to-user ban terminates friendship
        friendships.findBetween(blockerId, blockedId).ifPresent(friendships::delete);
        if (!blocks.existsByIdBlockerIdAndIdBlockedId(blockerId, blockedId)) {
            blocks.save(new UserBlock(blockerId, blockedId));
        }
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        blocks.findById(new UserBlockId(blockerId, blockedId)).ifPresent(blocks::delete);
    }

    public List<Friendship> listFriends(Long userId) {
        return friendships.findAcceptedFor(userId);
    }

    public List<Friendship> listIncoming(Long userId) {
        return friendships.findIncomingPending(userId);
    }

    public List<Friendship> listOutgoing(Long userId) {
        return friendships.findOutgoingPending(userId);
    }

    public List<UserBlock> listBlocks(Long userId) {
        return blocks.findByIdBlockerId(userId);
    }

    public boolean areFriends(Long u1, Long u2) {
        return friendships.findBetween(u1, u2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    public boolean canMessage(Long u1, Long u2) {
        if (u1.equals(u2)) return false;
        if (blocks.eitherBlocks(u1, u2)) return false;
        return areFriends(u1, u2);
    }
}
