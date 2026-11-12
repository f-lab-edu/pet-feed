package com.petfeed.facade.controller;

import com.petfeed.contracts.dto.request.SignupRequest;
import com.petfeed.contracts.dto.request.UpdateProfileRequest;
import com.petfeed.contracts.dto.response.PostListResponse;
import com.petfeed.contracts.dto.response.SignupResponse;
import com.petfeed.contracts.dto.response.MemberListResponse;
import com.petfeed.contracts.dto.response.MemberProfileResponse;
import com.petfeed.contracts.dto.response.MemberInfoResponse;
import com.petfeed.facade.client.PostFeignClient;
import com.petfeed.facade.client.MemberFeignClient;
import com.petfeed.facade.client.CommentFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberFacade {

    private final MemberFeignClient memberFeignClient;
    private final PostFeignClient postFeignClient;
    private final CommentFeignClient commentFeignClient;
    private final FollowFacade followFacade;

    public SignupResponse signup(SignupRequest request) {
        return memberFeignClient.signup(request);
    }

    public MemberProfileResponse getProfile(String memberKey, String requesterKey) {
        MemberInfoResponse profile = memberFeignClient.getProfile(memberKey);
        long postCount = postFeignClient.getPostCount(memberKey);
        var follows = followFacade.statistics(memberKey, requesterKey);
        return MemberProfileResponse.of(
                profile.getNickname(),
                profile.getProfileImageUrl(),
                profile.getBio(),
                follows.followerCount(),
                follows.followingCount(),
                follows.following(),
                postCount
        );
    }

    public PostListResponse getPostsByMember(String memberKey, Long cursor) {
        var author = memberFeignClient.getAuthor(memberKey);
        var posts = postFeignClient.getPostsByMember(memberKey, cursor);
        var counts = posts.getPosts().isEmpty() ? java.util.Map.<Long, Long>of()
                : commentFeignClient.counts(posts.getPosts().stream().map(com.petfeed.contracts.dto.response.PostSummary::getPostId).toList());
        return PostListResponse.of(posts.getPosts().stream().map(post -> post.withAuthor(author)
                        .withCommentCount(Math.toIntExact(counts.getOrDefault(post.getPostId(), 0L)))).toList(),
                posts.getNextCursor(), posts.isHasNext());
    }

    public void updateProfile(String memberKey, String requesterKey, UpdateProfileRequest request) {
        memberFeignClient.updateProfile(memberKey, requesterKey, request);
    }

    public void delete(String memberKey, String requesterKey) {
        memberFeignClient.delete(memberKey, requesterKey);
    }

}
