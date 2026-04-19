export type ChatType = "PUBLIC_ROOM" | "PRIVATE_ROOM" | "DIRECT";
export type ChatRole = "OWNER" | "ADMIN" | "MEMBER";
export type FriendshipStatus = "PENDING" | "ACCEPTED";

export type Room = {
  id: number;
  type: ChatType;
  name: string;
  description: string | null;
  ownerId: number | null;
  memberCount: number;
  bannedFromRoom: boolean;
  youAreMember: boolean;
};

export type ChatSummary = {
  id: number;
  type: ChatType;
  name: string | null;
  description: string | null;
  ownerId: number | null;
  memberCount: number;
  peerUserId: number | null;
  peerUsername: string | null;
  unreadCount: number;
};

export type ChatDetail = {
  id: number;
  type: ChatType;
  name: string | null;
  description: string | null;
  ownerId: number | null;
  ownerUsername: string | null;
  memberCount: number;
  yourRole: ChatRole | null;
  peerUserId: number | null;
  peerUsername: string | null;
  canMessage: boolean | null;
  lastReadMessageId: number | null;
};

export type RoomDetail = {
  id: number;
  type: ChatType;
  name: string;
  description: string | null;
  ownerId: number | null;
  ownerUsername: string | null;
  memberCount: number;
  yourRole: ChatRole | null;
};

export type Member = {
  userId: number;
  username: string;
  role: ChatRole;
  joinedAt: string;
};

export type Ban = {
  userId: number;
  username: string;
  bannedByUserId: number | null;
  bannedByUsername: string | null;
  bannedAt: string;
};

export type Invitation = {
  id: number;
  chatId: number;
  chatName: string;
  inviteeId: number;
  inviteeUsername: string;
  invitedByUserId: number | null;
  invitedByUsername: string | null;
  createdAt: string;
};

export type Friendship = {
  userId: number;
  username: string;
  status: FriendshipStatus;
  incoming: boolean;
  requestText: string | null;
  createdAt: string;
  acceptedAt: string | null;
};

export type UserSummary = {
  id: number;
  username: string;
  createdAt: string;
};

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};
