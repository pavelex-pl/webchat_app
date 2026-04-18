export type AttachmentDto = {
  id: number;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  comment: string | null;
};

export type MessageDto = {
  id: number;
  chatId: number;
  authorId: number | null;
  authorUsername: string | null;
  replyToId: number | null;
  body: string | null;
  createdAt: string;
  editedAt: string | null;
  deleted: boolean;
  attachments: AttachmentDto[];
};

export type ChatEvent = {
  kind: "created" | "updated" | "deleted";
  message: MessageDto;
};
