package com.petfeed.facade.controller;

import com.petfeed.domain.dto.request.SignupRequest;
import com.petfeed.domain.dto.request.UpdateProfileRequest;
import com.petfeed.domain.dto.response.PostListResponse;
import com.petfeed.domain.dto.response.SignupResponse;
import com.petfeed.domain.dto.response.UserListResponse;
import com.petfeed.domain.dto.response.UserProfileResponse;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;

    public SignupResponse signup(SignupRequest request) {
        return userFeignClient.signup(request);
    }

    public UserProfileResponse getProfile(String userKey) {
        UserProfileResponse profile = userFeignClient.getProfile(userKey);
        long postCount = postFeignClient.getPostCount(userKey);
        return UserProfileResponse.of(
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getBio(),
                profile.getFollowerCount(),
                profile.getFollowingCount(),
                profile.isFollowing(),
                postCount
        );
    }

    public PostListResponse getPostsByUser(String userKey, Long cursor) {
        var author = userFeignClient.getAuthor(userKey);
        var posts = postFeignClient.getPostsByUser(userKey, cursor);
        return PostListResponse.of(posts.getPosts().stream().map(post -> post.withAuthor(author)).toList(),
                posts.getNextCursor(), posts.isHasNext());
    }

    public UserListResponse getFollowers(String userKey, Long cursor) {
        return userFeignClient.getFollowers(userKey, cursor);
    }

    public UserListResponse getFollowings(String userKey, Long cursor) {
        return userFeignClient.getFollowings(userKey, cursor);
    }

    public void updateProfile(String userKey, String requesterKey, UpdateProfileRequest request) {
        userFeignClient.updateProfile(userKey, requesterKey, request);
    }

    public void delete(String userKey, String requesterKey) {
        userFeignClient.delete(userKey, requesterKey);
    }

    // PATCH → POST/DELETE 분리 (UserController 변경사유 동일)
    public void follow(String targetUserKey) {
        userFeignClient.follow(targetUserKey);
    }

    public void unfollow(String targetUserKey) {
        userFeignClient.unfollow(targetUserKey);
    }
}
