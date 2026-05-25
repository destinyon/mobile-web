package com.server.backend.common;

public record ActionState(boolean liked, boolean favorited, int likeCount, int favoriteCount) {
}
